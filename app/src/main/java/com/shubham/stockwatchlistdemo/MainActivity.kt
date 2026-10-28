package com.shubham.stockwatchlistdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.shubham.stockwatchlistdemo.core.designsystem.theme.StockWatchListTheme
import com.shubham.stockwatchlistdemo.di.ViewModelFactory
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.WatchlistRoute
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.WatchlistViewModel

/**
 * Single-Activity host for the Jetpack Compose Watchlist.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: WatchlistViewModel by viewModels {
        val app = application as StockWatchListApp
        ViewModelFactory(app.appContainer.marketDataRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StockWatchListTheme {
                WatchlistRoute(viewModel = viewModel)
            }
        }
    }
}
