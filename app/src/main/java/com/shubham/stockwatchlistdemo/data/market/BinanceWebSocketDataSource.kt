package com.shubham.stockwatchlistdemo.data.market

import android.util.Log
import com.shubham.stockwatchlistdemo.core.model.MarketConnectionState
import com.shubham.stockwatchlistdemo.core.model.MarketInstrument
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Concrete WebSocket data source communicating with Binance public combined streams.
 *
 * Implements exponential backoff with jitter, protection against concurrent duplicate connections,
 * non-blocking message emission, and deterministic cancellation.
 */
class BinanceWebSocketDataSource(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxReconnectAttempts: Int = 5,
    private val initialBackoffMs: Long = 1000L,
    private val maxBackoffMs: Long = 30000L,
    private val baseUrls: List<String> = listOf(
        "wss://data-stream.binance.vision/stream?streams=",
        "wss://stream.binance.com/stream?streams=",
        "wss://stream.binance.com:9443/stream?streams="
    )
) : MarketDataDataSource {

    private val tag = "BinanceWebSocket"
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    // Bounded channel to prevent memory overflow under excessive network burst
    private val messageChannel = Channel<String>(
        capacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val incomingMessages: Flow<String> = messageChannel.receiveAsFlow()

    private val _connectionState = MutableStateFlow<MarketConnectionState>(MarketConnectionState.Disconnected)
    override val connectionState: Flow<MarketConnectionState> = _connectionState.asStateFlow()

    private val connectionMutex = Mutex()
    private var currentWebSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private val isManuallyDisconnected = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)
    private var currentUrlIndex = 0
    private var currentInstruments: List<MarketInstrument> = emptyList()

    override fun connect(instruments: List<MarketInstrument>) {
        if (instruments.isEmpty()) return
        currentInstruments = instruments
        isManuallyDisconnected.set(false)
        reconnectAttempts.set(0)

        scope.launch {
            connectionMutex.withLock {
                initiateConnectionInternal()
            }
        }
    }

    override fun reconnect(instruments: List<MarketInstrument>) {
        currentInstruments = instruments
        isManuallyDisconnected.set(false)
        reconnectAttempts.set(0)
        reconnectJob?.cancel()

        scope.launch {
            connectionMutex.withLock {
                closeCurrentSocketInternal()
                initiateConnectionInternal()
            }
        }
    }

    override fun disconnect() {
        isManuallyDisconnected.set(true)
        reconnectJob?.cancel()
        reconnectAttempts.set(0)

        scope.launch {
            connectionMutex.withLock {
                closeCurrentSocketInternal()
                _connectionState.value = MarketConnectionState.Disconnected
            }
        }
    }

    private fun initiateConnectionInternal() {
        if (currentWebSocket != null) return

        val streamsQuery = currentInstruments.joinToString(separator = "/") { instrument ->
            "${instrument.id.lowercase(Locale.US)}@trade"
        }

        val baseUrl = baseUrls[currentUrlIndex % baseUrls.size]
        val fullUrl = "$baseUrl$streamsQuery"
        val request = Request.Builder()
            .url(fullUrl)
            .build()

        _connectionState.value = MarketConnectionState.Connecting
        Log.i(tag, "Opening WebSocket connection to endpoint: $baseUrl ($streamsQuery)")

        currentWebSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    private fun closeCurrentSocketInternal() {
        try {
            currentWebSocket?.close(1000, "Client initiated closure")
        } catch (_: Exception) {
            // Socket may already be dead
        } finally {
            currentWebSocket = null
        }
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(tag, "WebSocket connected successfully.")
                reconnectAttempts.set(0)
                _connectionState.value = MarketConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Non-blocking trySend to bounded channel
                messageChannel.trySend(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closing (code=$code, reason=$reason)")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closed (code=$code, reason=$reason)")
                currentWebSocket = null
                if (!isManuallyDisconnected.get()) {
                    scheduleReconnect()
                } else {
                    _connectionState.value = MarketConnectionState.Disconnected
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "WebSocket failure on endpoint: ${t.localizedMessage}")
                currentWebSocket = null
                currentUrlIndex++ // Fail over to next endpoint on failure
                if (!isManuallyDisconnected.get()) {
                    scheduleReconnect(t.localizedMessage ?: "Network connection failed")
                } else {
                    _connectionState.value = MarketConnectionState.Disconnected
                }
            }
        }
    }

    private fun scheduleReconnect(errorReason: String? = null) {
        if (isManuallyDisconnected.get()) return

        val attempt = reconnectAttempts.incrementAndGet()
        if (attempt > maxReconnectAttempts) {
            val message = errorReason ?: "Max reconnection attempts reached ($maxReconnectAttempts). Tap retry to connect."
            Log.e(tag, message)
            _connectionState.value = MarketConnectionState.Error(message = message, canRetry = true)
            return
        }

        // Exponential backoff: min(initial * 2^(attempt-1), maxBackoff) with +10% jitter
        val exponential = (initialBackoffMs * (1L shl (attempt - 1).coerceAtMost(30))).coerceAtMost(maxBackoffMs)
        val jitter = (Math.random() * 0.1 * exponential).toLong()
        val backoffDelay = exponential + jitter

        Log.w(tag, "Reconnection attempt $attempt/$maxReconnectAttempts in ${backoffDelay}ms")
        _connectionState.value = MarketConnectionState.Reconnecting(
            attempt = attempt,
            maxAttempts = maxReconnectAttempts,
            delayMs = backoffDelay
        )

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(backoffDelay)
            connectionMutex.withLock {
                if (!isManuallyDisconnected.get()) {
                    initiateConnectionInternal()
                }
            }
        }
    }
}
