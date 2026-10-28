package com.shubham.stockwatchlistdemo

import android.app.Application
import com.shubham.stockwatchlistdemo.di.AppContainer
import com.shubham.stockwatchlistdemo.di.DefaultAppContainer

/**
 * Custom Application class initializing the dependency container.
 */
class StockWatchListApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer()
    }
}
