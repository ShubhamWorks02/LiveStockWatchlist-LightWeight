package com.shubham.stockwatchlistdemo.feature.watchlist.presentation

import androidx.compose.runtime.Immutable
import com.shubham.stockwatchlistdemo.core.model.InstrumentUiModel
import com.shubham.stockwatchlistdemo.core.model.MarketConnectionState

enum class WatchlistSortOption(val displayName: String) {
    DEFAULT("Default"),
    PRICE_DESC("Price ↓"),
    PRICE_ASC("Price ↑"),
    CHANGE_DESC("Top Gainers"),
    CHANGE_ASC("Top Losers")
}

enum class WatchlistFilter(val displayName: String) {
    ALL("All Pairs"),
    GAINERS("Gainers"),
    LOSERS("Losers")
}

/**
 * Single immutable state holder for the Watchlist screen.
 * Follows unidirectional data flow (UDF) principles.
 */
@Immutable
data class WatchlistUiState(
    val connectionState: MarketConnectionState = MarketConnectionState.Connecting,
    val instruments: List<InstrumentUiModel> = emptyList(),
    val rawInstrumentsCount: Int = 0,
    val searchQuery: String = "",
    val activeFilter: WatchlistFilter = WatchlistFilter.ALL,
    val sortOption: WatchlistSortOption = WatchlistSortOption.DEFAULT,
    val gainersCount: Int = 0,
    val losersCount: Int = 0,
    val lastStateUpdateTimeMs: Long = System.currentTimeMillis()
) {
    val isConnecting: Boolean
        get() = connectionState is MarketConnectionState.Connecting

    val isConnected: Boolean
        get() = connectionState is MarketConnectionState.Connected

    val isReconnecting: Boolean
        get() = connectionState is MarketConnectionState.Reconnecting

    val isError: Boolean
        get() = connectionState is MarketConnectionState.Error
}
