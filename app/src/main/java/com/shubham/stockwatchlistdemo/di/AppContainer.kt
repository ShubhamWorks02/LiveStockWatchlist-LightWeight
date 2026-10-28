package com.shubham.stockwatchlistdemo.di

import com.shubham.stockwatchlistdemo.data.market.MarketDataRepository

/**
 * Dependency injection container interface providing application-level singletons.
 * Clean, reflection-free dependency injection suitable for modern Android apps.
 */
interface AppContainer {
    val marketDataRepository: MarketDataRepository
}
