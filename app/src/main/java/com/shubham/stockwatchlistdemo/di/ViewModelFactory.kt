package com.shubham.stockwatchlistdemo.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shubham.stockwatchlistdemo.data.market.MarketDataRepository
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.WatchlistViewModel

/**
 * Factory for creating ViewModels with injected dependencies without reflection.
 */
class ViewModelFactory(
    private val marketDataRepository: MarketDataRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WatchlistViewModel::class.java)) {
            return WatchlistViewModel(repository = marketDataRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
