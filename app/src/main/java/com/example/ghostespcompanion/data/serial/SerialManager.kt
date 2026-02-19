package com.example.ghostespcompanion.data.serial

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * USB Serial Manager for GhostESP communication
 *
 * Architecture for robust data handling with responsive UI:
 *
 * 1. Serial Read Loop (IO dispatcher):
 *    - Reads raw bytes from USB serial port
 *    - Processes into lines immediately (no blocking)
 *    - Every cleaned line is sent to rawOutput immediately (terminal sees everything)
 *    - Multi-line grouping is only applied for the parsed response channel
 *    - Binary mode: When SD:READ:LENGTH: is detected, switches to raw byte collection
 *
 * 2. Channel Consumer (IO dispatcher):
 *    - Receives grouped lines from responseChannel
 *    - Wraps in GhostSerialResponse and emits to SharedFlow
 *    - Uses tryEmit (non-blocking) with DROP_OLDEST for UI responsiveness
 *
 * 3. UI Collection (Main dispatcher):
 *    - Collects from SharedFlows
 *    - Updates UI state
 *
 * This ensures:
 * - Terminal sees EVERY line the firmware sends, with indentation preserved
 * - Parsed responses get intelligent multi-line grouping for structured data
 * - Serial reading is NEVER blocked (non-blocking Channel.send)
 * - UI remains responsive (DROP_OLDEST prevents backpressure buildup)
 * - Binary file transfers work correctly (raw bytes preserved)
 */
@Singleton
class SerialManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var serialDriver: UsbSerialDriver? = null
    private var serialPort: UsbSerialPort? = null
    private var usbConnection: UsbDeviceConnection? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Channel for parsed/grouped responses (multi-line accumulation applied here)
    // UNLIMITED capacity ensures we NEVER block the serial read loop and NEVER lose data
    private val responseChannel = Channel<String>(Channel.UNLIMITED)

    // SharedFlows for UI consumption
    // DROP_OLDEST ensures UI never blocks even if consumer is slow
    private val _responses = MutableSharedFlow<GhostSerialResponse>(
        replay = 1,
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val responses: SharedFlow<GhostSerialResponse> = _responses.asSharedFlow()

    // Raw output for terminal display — every line goes here immediately
    // No multi-line grouping, indentation preserved
    private val _rawOutput = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 2048,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val rawOutput: SharedFlow<String> = _rawOutput.asSharedFlow()

    // Debug log for chipinfo lifecycle — captures flush/skip/send events
    private val _chipInfoDebugLog = MutableStateFlow<List<String>>(emptyList())
    val chipInfoDebugLog: StateFlow<List<String>> = _chipInfoDebugLog.asStateFlow()

    private fun chipInfoLog(msg: String) {
        val ts = System.currentTimeMillis() % 100_000
        _chipInfoDebugLog.value = (_chipInfoDebugLog.value + "[$ts] $msg").takeLast(20)
    }

    // Binary data chunks for file transfers
    // Firmware sends raw binary after SD:READ:LENGTH: line, terminated by \nSD:READ:END:
    // Using Channel instead of SharedFlow to avoid race conditions - the collector
    // will receive the data reliably, whereas SharedFlow with replay=0 can lose data
    private val binaryChannel = Channel<ByteArray>(Channel.UNLIMITED)
    
    // Exposed as flow for collection
    val binaryChunks: Flow<ByteArray> = binaryChannel.receiveAsFlow()

    private var readJob: Job? = null
    private var consumerJob: Job? = null
    private var flushJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Track if we're in the process of connecting (to prevent double-connects)
    private val isConnecting = AtomicBoolean(false)

    // Pre-allocated buffers for performance
    private val readBuffer = ByteArray(4096)
    private val lineBuffer = StringBuilder(1024)
    private val multilineBuffer = StringBuilder(512)
    private var isAccumulatingMultiline = false
    private var multilineType: LineType? = null
    private var lastLineTime = 0L

    // Dedicated chipinfo collector — completely independent of the multiline
    // state machine.  Every line that matches a known chipinfo field is appended
    // here regardless of indentation, ordering, or prompt-stripping artefacts.
    // The collector is armed when we see "chipinfo" or "Chip Information" and
    // flushed by the timer once no new field has arrived for 500ms.
    private val chipInfoCollector = StringBuilder(512)
    private var chipInfoCollectorActive = false
    private var chipInfoLastFieldTime = 0L
    private var chipInfoCollectAllUntil = 0L
    private var chipInfoSeenCount = 0
    private val recentLines = ArrayDeque<String>(32)

    // Binary mode state for SD file transfers
    // Firmware protocol: SD:READ:BEGIN:... SD:READ:SIZE:... SD:READ:OFFSET:... SD:READ:LENGTH:...
    // Then raw binary data, then \nSD:READ:END:bytes=N\n
    private var isBinaryMode = false
    private val binaryAccumulator = ByteArrayOutputStream(8192)
    private val binaryTerminator = "\nSD:READ:END:".toByteArray(Charsets.US_ASCII)
    private var terminatorMatchPos = 0
    private val binaryHeaderBuffer = ByteArrayOutputStream(256)
    private var isCollectingBinaryHeader = false

    // Atomic flag for connection status
    private val isConnectedFlag = AtomicBoolean(false)

    // Mutex to prevent concurrent connect/disconnect races
    private val connectionMutex = Mutex()

    /**
     * Connection state enum
     */
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    /**
     * Get list of available USB serial devices
     */
    fun getAvailableDevices(): List<UsbDevice> {
        return UsbSerialProber.getDefaultProber()
            .findAllDrivers(usbManager)
            .mapNotNull { it.device }
    }

    /**
     * Connect to a USB device
     * 
     * Improved reconnection handling:
     * - Uses isConnecting flag to prevent concurrent connection attempts
     * - Uses withTimeout to prevent hanging on blocked operations
     * - Force-closes previous connection before attempting new one
     */
    @Suppress("DEPRECATION")
    suspend fun connect(device: UsbDevice): Boolean = connectionMutex.withLock {
        // Prevent double-connect attempts
        if (isConnecting.get()) {
            return@withLock false
        }
        
        isConnecting.set(true)
        
        // Always do a clean disconnect first, regardless of current state
        // Use withTimeout to prevent hanging if disconnect gets stuck
        try {
            withTimeout(2000) {
                disconnectInternal()
            }
        } catch (e: TimeoutCancellationException) {
            // If disconnect times out, force reset
            forceReset()
        } catch (e: Exception) {
            e.printStackTrace()
            forceReset()
        }

        _connectionState.value = ConnectionState.CONNECTING

        try {
            val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            serialDriver = drivers.find { it.device == device }

            if (serialDriver == null) {
                _connectionState.value = ConnectionState.ERROR
                isConnecting.set(false)
                return@withLock false
            }

            serialPort = serialDriver!!.ports.firstOrNull() ?: run {
                _connectionState.value = ConnectionState.ERROR
                isConnecting.set(false)
                return@withLock false
            }

            usbConnection = usbManager.openDevice(device) ?: run {
                _connectionState.value = ConnectionState.ERROR
                isConnecting.set(false)
                return@withLock false
            }

            serialPort?.open(usbConnection)
            serialPort?.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

            serialPort?.setDTR(true)
            serialPort?.setRTS(true)

            // Clear buffers
            lineBuffer.clear()
            multilineBuffer.clear()
            isAccumulatingMultiline = false
            multilineType = null
            lastLineTime = 0L

            // Reset binary mode state
            isBinaryMode = false
            binaryAccumulator.reset()
            terminatorMatchPos = 0
            binaryHeaderBuffer.reset()
            isCollectingBinaryHeader = false

        // Reset binary mode state
        isBinaryMode = false
        binaryAccumulator.reset()
        terminatorMatchPos = 0
        binaryHeaderBuffer.reset()
        isCollectingBinaryHeader = false

            isConnectedFlag.set(true)
            isConnecting.set(false)
            startReading()
            startConsumer()
            startFlushTimer()

            _connectionState.value = ConnectionState.CONNECTED
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _connectionState.value = ConnectionState.ERROR
            isConnecting.set(false)
            try {
                withTimeout(1000) {
                    disconnectInternal()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                forceReset()
            }
            false
        }
    }

    /**
     * Connect using the first available device
     */
    suspend fun connectFirstAvailable(): Boolean {
        val devices = getAvailableDevices()
        return if (devices.isNotEmpty()) {
            connect(devices.first())
        } else {
            false
        }
    }

    /**
     * Disconnect from the USB device (thread-safe, serialized with connect)
     */
    suspend fun disconnect() = connectionMutex.withLock {
        try {
            withTimeout(2000) {
                disconnectInternal()
            }
        } catch (e: TimeoutCancellationException) {
            forceReset()
        }
    }

    /**
     * Force disconnect - can be called from UI when app appears stuck
     * This bypasses the mutex lock and forces a reset
     */
    fun forceDisconnect() {
        isConnecting.set(false)
        isConnectedFlag.set(false)
        forceReset()
    }

    /**
     * Internal disconnect - must only be called while holding connectionMutex
     * Improved to handle stuck connections more gracefully
     */
    private fun disconnectInternal() {
        // Set flag first to stop read loop
        isConnectedFlag.set(false)
        isConnecting.set(false)

        // Cancel jobs immediately (don't wait for them)
        readJob?.cancel()
        readJob = null
        consumerJob?.cancel()
        consumerJob = null
        flushJob?.cancel()
        flushJob = null

        // Close serial port first - with individual try-catch for each operation
        serialPort?.let { port ->
            try { port.setDTR(false) } catch (e: Exception) { /* ignore */ }
            try { port.setRTS(false) } catch (e: Exception) { /* ignore */ }
            try { port.close() } catch (e: Exception) { /* ignore */ }
        }

        // Close USB connection
        try { usbConnection?.close() } catch (e: Exception) { /* ignore */ }

        // Clear references
        serialPort = null
        usbConnection = null
        serialDriver = null

        // Clear buffers
        lineBuffer.clear()
        multilineBuffer.clear()
        isAccumulatingMultiline = false
        multilineType = null
        lastLineTime = 0L

        // Clear the channel
        while (!responseChannel.isEmpty) {
            responseChannel.tryReceive()
        }

        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Force reset - use when connection is in error state
     */
    private fun forceReset() {
        try {
            disconnectInternal()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Ensure state is clean
        serialPort = null
        usbConnection = null
        serialDriver = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Send a command string to the device
     */
    suspend fun sendCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConnectedFlag.get()) return@withContext false

        try {
            // Flush any pending multiline buffer before sending a new command
            // so previous response data isn't lost
            flushMultilineBuffer()

            // If we're about to send chipinfo, arm the collector immediately
            // and collect all non-empty lines for a short window. This avoids
            // missing early fields that may arrive before the echo/header.
            if (command.trim().equals("chipinfo", ignoreCase = true)) {
                chipInfoCollector.clear()
                chipInfoCollectorActive = true
                chipInfoLastFieldTime = System.currentTimeMillis()
                chipInfoCollectAllUntil = chipInfoLastFieldTime + 1500
                chipInfoSeenCount = 0
                chipInfoLog("COLLECTOR armed (command)")
                seedChipInfoCollectorFromRecentLines()
            }

            val commandBytes = (command + "\r\n").toByteArray(Charsets.US_ASCII)
            serialPort?.write(commandBytes, 1000)
            _rawOutput.tryEmit("> $command")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Start reading from serial port
     * This runs on IO dispatcher and sends data to Channel (never blocks)
     */
    private fun startReading() {
        readJob = scope.launch {
            var consecutiveErrors = 0
            while (isActive && isConnectedFlag.get()) {
                try {
                    serialPort?.let { port ->
                        val bytesRead = port.read(readBuffer, 1000)
                        if (bytesRead > 0) {
                            consecutiveErrors = 0
                            processIncomingDataFast(readBuffer, bytesRead)
                        }
                    }
                } catch (e: Exception) {
                    if (isActive && isConnectedFlag.get()) {
                        consecutiveErrors++
                        e.printStackTrace()

                        if (consecutiveErrors > 5) {
                            mainScope.launch {
                                _connectionState.value = ConnectionState.ERROR
                            }
                            isConnectedFlag.set(false)
                            break
                        }
                        delay(100)
                    }
                }
            }
        }
    }

    /**
     * Start the channel consumer that processes grouped lines and emits to response flow
     * This runs on IO dispatcher separate from the read loop
     */
    private fun startConsumer() {
        consumerJob = scope.launch {
            for (line in responseChannel) {
                val response = GhostSerialResponse(line)
                _responses.tryEmit(response)
            }
        }
    }

    /**
     * Periodic flush timer for multiline buffer.
     * If no new data arrives for 500ms while accumulating, flush the buffer.
     * This prevents data from getting stuck (e.g., chipinfo being the last response).
     */
    private fun startFlushTimer() {
        flushJob = scope.launch {
            while (isActive && isConnectedFlag.get()) {
                delay(500)
                // Flush normal multiline buffer
                if (isAccumulatingMultiline && multilineBuffer.isNotEmpty()) {
                    val elapsed = System.currentTimeMillis() - lastLineTime
                    if (elapsed >= 500) {
                        flushMultilineBuffer()
                    }
                }
                // Flush chipinfo collector independently
                if (chipInfoCollectorActive && chipInfoCollector.isNotEmpty()) {
                    val elapsed = System.currentTimeMillis() - chipInfoLastFieldTime
                    if (elapsed >= 500) {
                        val collected = chipInfoCollector.toString()
                        chipInfoCollector.clear()
                        chipInfoCollectorActive = false
                        chipInfoCollectAllUntil = 0L
                        chipInfoSeenCount = 0
                        chipInfoLog("COLLECTOR flushed (${collected.length} chars, idle ${elapsed}ms)")
                        // Build a synthetic chipinfo response the parser can recognise
                        sendToResponseChannel("Chip Information: $collected")
                    }
                }
            }
        }
    }

    /**
     * Flush any accumulated multiline data to the response channel.
     * Chipinfo is handled by its own dedicated collector — this only
     * handles AP / station / flipper / handshake / etc. multi-line groups.
     */
    private fun flushMultilineBuffer() {
        if (isAccumulatingMultiline && multilineBuffer.isNotEmpty()) {
            val buffer = multilineBuffer.toString()
            multilineBuffer.clear()
            isAccumulatingMultiline = false
            multilineType = null
            sendToResponseChannel(buffer)
        }
    }
    
    /**
     * Returns true when [line] looks like a chipinfo field or sub-field.
     * Used by the dedicated chipinfo collector to capture every relevant line
     * regardless of indentation or arrival order.
     */
    private fun isChipInfoField(line: String): Boolean {
        val t = line.trim()
        return t.startsWith("Model:") ||
                t.startsWith("Revision:") ||
                t.startsWith("CPU Cores:") ||
                t.startsWith("Features:") && !t.startsWith("Features:,") ||
                t.startsWith("Free Heap:") ||
                t.startsWith("Min Free Heap:") ||
                t.startsWith("IDF Version:") ||
                t.startsWith("Build Config:") ||
                t.startsWith("Firmware:") ||
                t.startsWith("Git Commit:") ||
                t.startsWith("Enabled Features") ||
                // Sub-fields under "Enabled Features:" — short names like
                // "Display", "NFC", "BadUSB", "Infrared TX", etc.
                (chipInfoCollectorActive &&
                 t.length < 40 && !t.startsWith("[") && !t.contains(":") && t.isNotEmpty())
    }

    private fun rememberRecentLine(line: String) {
        val t = line.trim()
        if (t.isEmpty()) return
        if (recentLines.size >= 25) {
            recentLines.removeFirst()
        }
        recentLines.addLast(t)
    }

    private fun seedChipInfoCollectorFromRecentLines() {
        var seeded = 0
        for (line in recentLines) {
            if (isChipInfoField(line)) {
                if (chipInfoCollector.isNotEmpty()) chipInfoCollector.append(", ")
                chipInfoCollector.append(line.trim())
                seeded += 1
            }
        }
        if (seeded > 0) {
            chipInfoLastFieldTime = System.currentTimeMillis()
            chipInfoLog("COLLECTOR seeded ($seeded)")
        }
    }

    /**
     * Fast data processing - avoids string allocations where possible
     * Every line goes to rawOutput immediately for terminal display.
     * Multi-line grouping only happens for the response channel (parsed data).
     * Binary mode: When SD:READ:LENGTH: is detected, switches to raw byte collection.
     */
    private fun processIncomingDataFast(buffer: ByteArray, length: Int) {
        if (isBinaryMode) {
            processBinaryData(buffer, length)
            return
        }

        for (i in 0 until length) {
            val byte = buffer[i]

            when (byte) {
                '\r'.code.toByte(), '\n'.code.toByte() -> {
                    if (lineBuffer.isNotEmpty()) {
                        val line = lineBuffer.toString()
                        lineBuffer.clear()
                        
                        // Check if this line triggers binary mode
                        if (line.startsWith("SD:READ:LENGTH:")) {
                            processLine(line)
                            // Switch to binary mode after the LENGTH line.
                            // Any bytes remaining in this buffer after the newline are
                            // already binary payload — pass them to processBinaryData.
                            isBinaryMode = true
                            binaryAccumulator.reset()
                            terminatorMatchPos = 0
                            val remaining = i + 1
                            if (remaining < length) {
                                processBinaryData(buffer, length, offset = remaining)
                            }
                            return
                        } else {
                            processLine(line)
                        }
                    }
                }
                else -> {
                    lineBuffer.append(byte.toInt().toChar())
                }
            }
        }
    }

    /**
     * Process binary data during SD file transfers.
     * Collects raw bytes until terminator "\nSD:READ:END:" is detected.
     * [offset] lets us start mid-buffer when switching from line mode on the same read.
     */
    private fun processBinaryData(buffer: ByteArray, length: Int, offset: Int = 0) {
        for (i in offset until length) {
            val byte = buffer[i]

            // Check if this byte matches the next expected terminator byte
            if (byte == binaryTerminator[terminatorMatchPos]) {
                terminatorMatchPos++
                
                if (terminatorMatchPos == binaryTerminator.size) {
                    // Complete terminator found - emit the binary chunk
                    val chunkData = binaryAccumulator.toByteArray()
                    binaryChannel.trySend(chunkData)
                    
                    // Reset binary mode
                    isBinaryMode = false
                    binaryAccumulator.reset()
                    terminatorMatchPos = 0
                    
                    // Process remaining bytes in this buffer as lines
                    if (i + 1 < length) {
                        processIncomingDataFast(buffer.copyOfRange(i + 1, length), length - i - 1)
                    }
                    return
                }
            } else {
                // Byte doesn't match terminator
                if (terminatorMatchPos > 0) {
                    // We had a partial match - flush those bytes to accumulator
                    for (j in 0 until terminatorMatchPos) {
                        binaryAccumulator.write(binaryTerminator[j].toInt())
                    }
                    terminatorMatchPos = 0
                    
                    // Re-check this byte against first terminator byte
                    if (byte == binaryTerminator[0]) {
                        terminatorMatchPos = 1
                        continue
                    }
                }
                // Add this byte to accumulator
                binaryAccumulator.write(byte.toInt())
            }
        }
    }

    /**
     * Process a complete line from serial.
     * 1. Strip ANSI codes and prompt prefix
     * 2. Emit to rawOutput immediately (terminal sees everything, indentation preserved)
     * 3. Feed into multi-line state machine for parsed responses only
     */
    private fun processLine(line: String) {
        // Strip ANSI escape codes efficiently
        var cleanLine = stripAnsiFast(line)

        // Strip prompt prefix
        when {
            cleanLine.startsWith("ghost-cli>") -> {
                cleanLine = cleanLine.removePrefix("ghost-cli>").trim()
            }
            cleanLine.startsWith("> ") -> {
                val afterPrompt = cleanLine.removePrefix("> ")
                if (afterPrompt.isNotBlank()) {
                    cleanLine = afterPrompt.trim()
                }
            }
        }

        if (cleanLine.isEmpty()) {
            // Flush multiline buffer on empty lines (but never chipinfo — it has its own collector)
            if (isAccumulatingMultiline && multilineBuffer.isNotEmpty()) {
                flushMultilineBuffer()
            }
            return
        }

        // Always emit to raw output for terminal display — indentation preserved
        _rawOutput.tryEmit(cleanLine)

        // Keep a short history of recent lines in case chipinfo output
        // arrives before the collector is armed.
        rememberRecentLine(cleanLine)

        // ── Chipinfo collector (independent of multiline state machine) ──
        // Use markers [CHIPINFO_START] and [CHIPINFO_END] for robust parsing
        val trimmedLine = cleanLine.trim()
        val isChipInfoStartMarker = trimmedLine.startsWith("[CHIPINFO_START]")
        val isChipInfoEndMarker = trimmedLine.startsWith("[CHIPINFO_END]")
        val isChipInfoTrigger = trimmedLine.equals("chipinfo", ignoreCase = true) ||
            trimmedLine.startsWith("Chip Information") || isChipInfoStartMarker
        val isChipInfoData = isChipInfoField(cleanLine)
        val now = System.currentTimeMillis()
        val collectAllWindow = chipInfoCollectorActive && now <= chipInfoCollectAllUntil

        // Handle [CHIPINFO_END] - flush immediately
        if (isChipInfoEndMarker && chipInfoCollectorActive && chipInfoCollector.isNotEmpty()) {
            val collected = chipInfoCollector.toString()
            chipInfoCollector.clear()
            chipInfoCollectorActive = false
            chipInfoCollectAllUntil = 0L
            chipInfoSeenCount = 0
            chipInfoLog("COLLECTOR flushed (${collected.length} chars, end marker)")
            sendToResponseChannel("Chip Information: $collected")
            return
        }

        if ((isChipInfoTrigger || isChipInfoData) && !chipInfoCollectorActive) {
            chipInfoCollector.clear()
            chipInfoCollectorActive = true
            chipInfoLastFieldTime = now
            chipInfoCollectAllUntil = now + 1500
            chipInfoSeenCount = 0
            chipInfoLog("COLLECTOR armed (marker: ${isChipInfoStartMarker}, auto)")
            seedChipInfoCollectorFromRecentLines()
        }

        val shouldCollectLine = chipInfoCollectorActive &&
            !isChipInfoTrigger &&
            !isChipInfoEndMarker &&
            trimmedLine.isNotEmpty() &&
            (isChipInfoData || collectAllWindow)

        if (collectAllWindow && chipInfoSeenCount < 20) {
            chipInfoSeenCount += 1
            chipInfoLog("SEEN: '${trimmedLine.take(40)}'")
        }

        if (shouldCollectLine) {
            if (chipInfoCollector.isNotEmpty()) chipInfoCollector.append(", ")
            chipInfoCollector.append(trimmedLine)
            chipInfoLastFieldTime = now
            chipInfoLog("COLLECT: '${trimmedLine.take(40)}' (buf=${chipInfoCollector.length})")
        }

        // ── Normal multi-line grouping for everything else ──
        lastLineTime = System.currentTimeMillis()
        val lineType = detectLineTypeFast(cleanLine)

        when (lineType) {
            // Chipinfo triggers no longer participate in the multiline state machine
            LineType.CHIP_INFO_START -> {
                // Already handled above by the collector — skip
            }
            LineType.AP_START, LineType.FLIPPER_START, LineType.AIRTAG_START,
            LineType.STATION_START, LineType.GATT_START,
            LineType.TRACK_HEADER_START -> {
                // Flush any previous accumulation
                if (isAccumulatingMultiline && multilineBuffer.isNotEmpty()) {
                    sendToResponseChannel(multilineBuffer.toString())
                }
                multilineBuffer.clear()
                multilineBuffer.append(cleanLine)
                isAccumulatingMultiline = true
                multilineType = lineType
            }
            LineType.HANDSHAKE_START -> {
                if (isAccumulatingMultiline && multilineBuffer.isNotEmpty()) {
                    sendToResponseChannel(multilineBuffer.toString())
                }
                multilineBuffer.clear()
                multilineBuffer.append(cleanLine)
                isAccumulatingMultiline = true
                multilineType = lineType
            }
            LineType.HANDSHAKE_CONTINUATION -> {
                if (isAccumulatingMultiline) {
                    multilineBuffer.append(", ").append(cleanLine.trim())
                } else {
                    sendToResponseChannel(cleanLine.trim())
                }
            }
            LineType.WIFI_STATUS_START -> {
                if (isAccumulatingMultiline && multilineBuffer.isNotEmpty()) {
                    sendToResponseChannel(multilineBuffer.toString())
                }
                multilineBuffer.clear()
                multilineBuffer.append(cleanLine)
                isAccumulatingMultiline = true
                multilineType = lineType
            }
            LineType.WIFI_STATUS_CONTINUATION -> {
                if (isAccumulatingMultiline) {
                    multilineBuffer.append("\n").append(cleanLine.trim())
                    // Check for end marker - flush immediately
                    if (cleanLine.contains("=== END STATUS ===")) {
                        val buffer = multilineBuffer.toString()
                        multilineBuffer.clear()
                        isAccumulatingMultiline = false
                        multilineType = null
                        sendToResponseChannel(buffer)
                    }
                } else {
                    sendToResponseChannel(cleanLine.trim())
                }
            }
            LineType.CONTINUATION -> {
                if (isAccumulatingMultiline) {
                    multilineBuffer.append(", ").append(cleanLine.trim())
                } else {
                    sendToResponseChannel(cleanLine.trim())
                }
            }
            LineType.IR_REMOTE, LineType.IR_BUTTON -> {
                if (isAccumulatingMultiline && multilineBuffer.isNotEmpty()) {
                    sendToResponseChannel(multilineBuffer.toString())
                    multilineBuffer.clear()
                    isAccumulatingMultiline = false
                    multilineType = null
                }
                sendToResponseChannel(cleanLine)
            }
            else -> {
                if (isAccumulatingMultiline && multilineBuffer.isNotEmpty()) {
                    sendToResponseChannel(multilineBuffer.toString())
                    multilineBuffer.clear()
                    isAccumulatingMultiline = false
                    multilineType = null
                    sendToResponseChannel(cleanLine)
                } else {
                    sendToResponseChannel(cleanLine)
                }
            }
        }
    }

    /**
     * Send grouped/parsed line to response channel for structured parsing.
     * Trimmed since parsers don't need indentation.
     */
    private fun sendToResponseChannel(line: String) {
        responseChannel.trySend(line.trim())
    }

    /**
     * Fast ANSI stripping without regex
     */
    private fun stripAnsiFast(input: String): String {
        val result = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '\u001b' && i + 1 < input.length && input[i + 1] == '[') {
                i += 2
                while (i < input.length) {
                    val ch = input[i]
                    if (ch in 'A'..'Z' || ch in 'a'..'z' || ch == '~') {
                        i++
                        break
                    }
                    i++
                }
            } else if (c >= ' ' || c == '\t') {
                result.append(c)
                i++
            } else {
                i++
            }
        }
        return result.toString()
    }

    /**
     * Fast line type detection without regex
     */
    private fun detectLineTypeFast(line: String): LineType {
        if (line.startsWith("Chip Information") || line.trim().equals("chipinfo", ignoreCase = true)) {
            return LineType.CHIP_INFO_START
        }

        val trimmed = line.trim()

        // IR remote files - check trimmed since firmware may indent
        if (trimmed.startsWith("[") && (trimmed.contains(".ir") || trimmed.contains(".json"))) {
            if (!trimmed.contains("SSID:") && !trimmed.contains("Flipper", ignoreCase = true)
                && !trimmed.contains("AirTag") && !trimmed.contains("Station MAC:")
                && !trimmed.contains("Name:")) {
                return LineType.IR_REMOTE
            }
        }

        // IR buttons - firmware outputs like "  [0] Power (NEC)" with leading spaces
        // Must check BEFORE CONTINUATION to avoid misclassifying
        if (trimmed.startsWith("[") && trimmed.contains("]") && !trimmed.contains(".ir") && !trimmed.contains(".json")) {
            // Check if it looks like an IR button: [N] name (protocol) or [N] name
            val afterBracket = trimmed.substringAfter("]", "").trim()
            if (afterBracket.isNotEmpty() && !afterBracket.contains("SSID:") && !afterBracket.contains("Station MAC:")
                && !afterBracket.contains("Name:") && !afterBracket.contains("Flipper") && !afterBracket.contains("AirTag")
                && !afterBracket.contains("RSSI:")) {
                return LineType.IR_BUTTON
            }
        }

        if (line.startsWith("[") && line.contains("SSID:")) {
            return LineType.AP_START
        }

        if (line.startsWith("[") && line.contains("Flipper", ignoreCase = true) && line.contains("Found")) {
            return LineType.FLIPPER_START
        }

        if (line.startsWith("[") && line.contains("AirTag") && line.contains("Found")) {
            return LineType.AIRTAG_START
        }

        if (line.startsWith("New Station:") || (line.startsWith("[") && line.contains("Station MAC:")) || line.startsWith("Station:")) {
            return LineType.STATION_START
        }

        if (line.startsWith("[") && line.contains("Name:") && !line.contains("SSID:")) {
            return LineType.GATT_START
        }

        // GATT tracking header - "=== Tracking Device ===" starts a multi-line block
        if (trimmed.startsWith("===") && trimmed.contains("Tracking Device", ignoreCase = true)) {
            return LineType.TRACK_HEADER_START
        }

        // Handshake detection - "Handshake found!" starts a multi-line block
        if (trimmed.startsWith("Handshake found", ignoreCase = true)) {
            return LineType.HANDSHAKE_START
        }

        // Handshake continuation - AP= and Pair= lines
        if (trimmed.startsWith("AP=") || trimmed.startsWith("Pair=")) {
            return LineType.HANDSHAKE_CONTINUATION
        }
        
        // WiFi Status header - "=== WIFI STATUS ===" starts a multi-line block
        if (trimmed.contains("=== WIFI STATUS ===")) {
            return LineType.WIFI_STATUS_START
        }
        
        // WiFi Status continuation - key=value lines and end marker
        if (trimmed.contains("=") && !trimmed.startsWith("[") && 
            (trimmed.startsWith("connected=") || trimmed.startsWith("has_saved_network=") ||
             trimmed.startsWith("connected_ssid=") || trimmed.startsWith("connected_rssi=") ||
             trimmed.startsWith("connected_bssid=") || trimmed.startsWith("connected_channel=") ||
             trimmed.startsWith("saved_ssid=") || trimmed.contains("=== END STATUS ==="))) {
            return LineType.WIFI_STATUS_CONTINUATION
        }

        // Continuation lines - but NOT if trimmed starts with [ (those are IR buttons)
        if ((line.startsWith("  ") || line.startsWith("\t") || line.startsWith(" ")) && !trimmed.startsWith("[")) {
            return LineType.CONTINUATION
        }

        return LineType.SINGLE
    }

    private enum class LineType {
        AP_START, FLIPPER_START, AIRTAG_START, STATION_START, GATT_START, CHIP_INFO_START, TRACK_HEADER_START, IR_REMOTE, IR_BUTTON, HANDSHAKE_START, HANDSHAKE_CONTINUATION, WIFI_STATUS_START, WIFI_STATUS_CONTINUATION, CONTINUATION, SINGLE
    }

    /**
     * Check if device is connected
     */
    fun isConnected(): Boolean = isConnectedFlag.get()

    /**
     * Clean up resources
     */
    fun destroy() {
        disconnectInternal()
        responseChannel.close()
        scope.cancel()
        mainScope.cancel()
    }
}

/**
 * Serial response wrapper with optimized type detection
 */
data class GhostSerialResponse(
    val raw: String
) {
    enum class ResponseType {
        UNKNOWN,
        ACCESS_POINT,
        BLE_DEVICE,
        FLIPPER_DEVICE,
        AIRTAG_DEVICE,
        GATT_DEVICE,
        GATT_SERVICE,
        STATION,
        NFC_TAG,
        GPS_POSITION,
        SD_ENTRY,
        ERROR,
        SUCCESS,
        STATUS,
        PROMPT,
        AERIAL_DEVICE,
        PORTAL_CREDS,
        IR_LEARNED,
        IR_LEARN_SAVED,
        IR_LEARN_STATUS,
        IR_DAZZLER,
        IR_REMOTE,
        IR_BUTTON,
        GHOSTESP_OK,
        SETTING_VALUE,
        DEVICE_INFO,
        TRACK_DATA,
        TRACK_HEADER,
        FLIPPER_TRACK_DATA,
        HANDSHAKE,
        PCAP_FILE,
        WIFI_CONNECTION,
        WIFI_STATUS
    }

    // Lazy evaluation of type for performance
    val type: ResponseType by lazy {
        detectTypeFast()
    }

    private fun detectTypeFast(): ResponseType {
        return when {
            raw.startsWith("[") && raw.contains("SSID:") && raw.contains("BSSID:") -> ResponseType.ACCESS_POINT

            raw.contains("Flipper") && raw.contains("Found") -> ResponseType.FLIPPER_DEVICE

            raw.contains("AirTag") && raw.contains("Found") -> ResponseType.AIRTAG_DEVICE

            raw.startsWith("[") && raw.contains("Name:") && raw.contains("MAC:") && !raw.contains("SSID:") -> ResponseType.GATT_DEVICE

            raw.startsWith("Service:") && raw.contains("handles") -> ResponseType.GATT_SERVICE

            raw.startsWith("New Station:") || raw.contains("Station MAC:") || (raw.contains("Station:") && raw.contains("Associated AP:")) -> ResponseType.STATION

            raw.startsWith("BLE:") -> ResponseType.BLE_DEVICE

            raw.contains("NFC Tag") -> ResponseType.NFC_TAG

            raw.contains("Lat:") && raw.contains("Lon:") -> ResponseType.GPS_POSITION

            raw.startsWith("SD:") -> ResponseType.SD_ENTRY

            raw.startsWith("ERROR:") -> ResponseType.ERROR

            raw.startsWith("OK:") -> ResponseType.SUCCESS

            raw.startsWith(">") -> ResponseType.PROMPT

            raw.startsWith("[") && raw.contains("MAC:") && raw.contains("Type:") -> ResponseType.AERIAL_DEVICE

            raw.contains("Captured credentials:") -> ResponseType.PORTAL_CREDS

            raw.contains("Captured:") && raw.contains("A:") && raw.contains("C:") -> ResponseType.IR_LEARNED

            raw.contains("Captured RAW signal") -> ResponseType.IR_LEARNED

            raw.contains("Saved to") && raw.contains(".ir") -> ResponseType.IR_LEARN_SAVED

            raw.contains("Waiting for IR signal") || raw.contains("IR learn task started") -> ResponseType.IR_LEARN_STATUS

            raw.contains("Timeout, no signal received") -> ResponseType.IR_LEARN_STATUS

            raw.startsWith("IR_DAZZLER:") -> ResponseType.IR_DAZZLER

            raw.trim().startsWith("[") && raw.trim().contains(".ir") -> ResponseType.IR_REMOTE
            raw.trim().startsWith("[") && raw.trim().contains(".json") && raw.contains("IR files") -> ResponseType.IR_REMOTE

            (raw.contains("#####") || (raw.startsWith("[") && raw.contains("RSSI:") && raw.contains("Min:") && raw.contains("Max:"))) && raw.contains("dBm") -> ResponseType.TRACK_DATA

            raw.contains("Tracking Flipper", ignoreCase = true) && raw.contains("RSSI") && raw.contains("dBm") -> ResponseType.FLIPPER_TRACK_DATA

            raw.trim().startsWith("[") && !raw.contains(".ir") && !raw.contains(".json") -> ResponseType.IR_BUTTON

            raw == "GHOSTESP_OK" -> ResponseType.GHOSTESP_OK

            raw.contains("Chip Information") ||
                (raw.contains("Model:") && raw.contains("IDF Version:") && raw.contains("CPU Cores:")) -> ResponseType.DEVICE_INFO

            raw.contains(" = ") && !raw.startsWith("[") -> ResponseType.SETTING_VALUE

            (raw.contains("tracking") || raw.contains("Tracking")) && raw.contains("===") -> ResponseType.TRACK_HEADER

            raw.contains("Handshake found", ignoreCase = true) -> ResponseType.HANDSHAKE

            raw.contains("PCAP") && raw.contains(".pcap") -> ResponseType.PCAP_FILE

            raw.contains("Got IP:") || 
                raw.contains("WiFi Connected", ignoreCase = true) ||
                raw.contains("WiFi Disconnected", ignoreCase = true) ||
                raw.contains("Attempting", ignoreCase = true) && raw.contains("connection", ignoreCase = true) -> ResponseType.WIFI_CONNECTION

            raw.contains("=== WIFI STATUS ===") || 
                (raw.contains("connected=") && raw.contains("has_saved_network=")) -> ResponseType.WIFI_STATUS

            else -> ResponseType.STATUS
        }
    }
}
