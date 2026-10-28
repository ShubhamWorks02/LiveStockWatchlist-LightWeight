package com.shubham.stockwatchlistdemo.feature.watchlist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shubham.stockwatchlistdemo.core.designsystem.theme.DarkBackground
import com.shubham.stockwatchlistdemo.core.designsystem.theme.DarkBorder
import com.shubham.stockwatchlistdemo.core.model.MarketConnectionState
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.components.EmptyWatchlistView
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.components.ErrorWatchlistView
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.components.WatchlistHeader
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.components.WatchlistRow

/**
 * Pure, stateless screen composable accepting immutable [WatchlistUiState] and action callbacks.
 * Supports Jetpack Compose Previews and deterministic UI unit tests without constructing a ViewModel.
 */
@Composable
fun WatchlistScreen(
    uiState: WatchlistUiState,
    onSearchQueryChanged: (String) -> Unit,
    onFilterSelected: (WatchlistFilter) -> Unit,
    onSortSelected: (WatchlistSortOption) -> Unit,
    onRetryClicked: () -> Unit,
    onInstrumentClicked: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header: Branding, Search, Connection Status Badge, Filters & Summary Cards
            WatchlistHeader(
                connectionState = uiState.connectionState,
                totalCount = uiState.rawInstrumentsCount,
                gainersCount = uiState.gainersCount,
                losersCount = uiState.losersCount,
                searchQuery = uiState.searchQuery,
                activeFilter = uiState.activeFilter,
                activeSort = uiState.sortOption,
                onSearchQueryChanged = onSearchQueryChanged,
                onFilterSelected = onFilterSelected,
                onSortSelected = onSortSelected,
                onRetryClicked = onRetryClicked
            )

            HorizontalDivider(
                color = DarkBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Content Area: Empty State, Error State, or Optimized LazyColumn Watchlist
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.instruments.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
                        EmptyWatchlistView(query = uiState.searchQuery)
                    }
                    uiState.instruments.isEmpty() && uiState.connectionState is MarketConnectionState.Error -> {
                        ErrorWatchlistView(
                            errorMessage = (uiState.connectionState as MarketConnectionState.Error).message,
                            onRetryClicked = onRetryClicked
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(
                                items = uiState.instruments,
                                key = { instrument -> instrument.id } // Stable item key prevents redundant row rebuilds
                            ) { instrument ->
                                WatchlistRow(
                                    instrument = instrument,
                                    onClick = { onInstrumentClicked(instrument.id) }
                                )
                                HorizontalDivider(
                                    color = DarkBorder.copy(alpha = 0.5f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
