package com.shubham.stockwatchlistdemo.feature.watchlist.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Public Route Composable bridging the [WatchlistViewModel] and stateless [WatchlistScreen].
 * Collects UI state in a lifecycle-aware manner using [collectAsStateWithLifecycle].
 */
@Composable
fun WatchlistRoute(
    viewModel: WatchlistViewModel,
    modifier: Modifier = Modifier,
    onInstrumentClicked: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WatchlistScreen(
        uiState = uiState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onFilterSelected = viewModel::onFilterSelected,
        onSortSelected = viewModel::onSortSelected,
        onRetryClicked = viewModel::retryConnection,
        onInstrumentClicked = onInstrumentClicked,
        modifier = modifier
    )
}
