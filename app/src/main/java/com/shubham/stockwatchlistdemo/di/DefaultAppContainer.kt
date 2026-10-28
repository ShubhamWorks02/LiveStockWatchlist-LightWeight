package com.shubham.stockwatchlistdemo.di

import com.shubham.stockwatchlistdemo.data.market.BinanceWebSocketDataSource
import com.shubham.stockwatchlistdemo.data.market.MarketDataMapper
import com.shubham.stockwatchlistdemo.data.market.MarketDataRepository
import com.shubham.stockwatchlistdemo.data.market.WebSocketMarketDataRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Production implementation of [AppContainer].
 * Configures OkHttpClient timeouts, JSON serializer, WebSocket data source, and repository.
 */
class DefaultAppContainer : AppContainer {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite read timeout for continuous WebSocket stream
            .connectTimeout(5, TimeUnit.SECONDS)   // 5s connect timeout so blocked endpoints fail over quickly
            .pingInterval(15, TimeUnit.SECONDS)    // Keep-alive ping every 15s to detect broken connections
            .retryOnConnectionFailure(true)
            .build()
    }

    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    private val marketDataMapper: MarketDataMapper by lazy {
        MarketDataMapper(json = json)
    }

    private val webSocketDataSource: BinanceWebSocketDataSource by lazy {
        BinanceWebSocketDataSource(
            okHttpClient = okHttpClient
        )
    }

    override val marketDataRepository: MarketDataRepository by lazy {
        WebSocketMarketDataRepository(
            dataSource = webSocketDataSource,
            mapper = marketDataMapper
        )
    }
}
