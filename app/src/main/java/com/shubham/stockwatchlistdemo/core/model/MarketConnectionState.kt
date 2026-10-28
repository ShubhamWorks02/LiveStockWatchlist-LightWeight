package com.shubham.stockwatchlistdemo.core.model

/**
 * Explicit connection lifecycle states for the real-time WebSocket market data feed.
 */
sealed interface MarketConnectionState {
    /** WebSocket connection is closed / idle. */
    data object Disconnected : MarketConnectionState

    /** Establishing TCP/TLS handshake and subscribing to streams. */
    data object Connecting : MarketConnectionState

    /** Actively connected and receiving live tick updates. */
    data object Connected : MarketConnectionState

    /**
     * Connection was dropped; attempting automatic reconnect with exponential backoff.
     *
     * @property attempt Current attempt number (1-indexed).
     * @property maxAttempts Maximum retries before transitioning to [Error].
     * @property delayMs Backoff delay in milliseconds before the next attempt.
     */
    data class Reconnecting(
        val attempt: Int,
        val maxAttempts: Int,
        val delayMs: Long
    ) : MarketConnectionState

    /**
     * Unrecoverable error or max reconnection attempts exceeded.
     *
     * @property message Human-readable diagnostic or error description.
     * @property canRetry Whether the user can manually trigger a retry.
     */
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : MarketConnectionState
}
