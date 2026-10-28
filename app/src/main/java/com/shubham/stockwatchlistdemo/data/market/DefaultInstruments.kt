package com.shubham.stockwatchlistdemo.data.market

import com.shubham.stockwatchlistdemo.core.model.MarketInstrument

object DefaultInstruments {
    val items: List<MarketInstrument> = listOf(
        MarketInstrument(
            id = "BTCUSDT",
            symbol = "BTC/USDT",
            name = "Bitcoin",
            baseAsset = "BTC",
            quoteAsset = "USDT",
            decimalPlaces = 2
        ),
        MarketInstrument(
            id = "ETHUSDT",
            symbol = "ETH/USDT",
            name = "Ethereum",
            baseAsset = "ETH",
            quoteAsset = "USDT",
            decimalPlaces = 2
        ),
        MarketInstrument(
            id = "SOLUSDT",
            symbol = "SOL/USDT",
            name = "Solana",
            baseAsset = "SOL",
            quoteAsset = "USDT",
            decimalPlaces = 2
        ),
        MarketInstrument(
            id = "BNBUSDT",
            symbol = "BNB/USDT",
            name = "BNB",
            baseAsset = "BNB",
            quoteAsset = "USDT",
            decimalPlaces = 2
        ),
        MarketInstrument(
            id = "XRPUSDT",
            symbol = "XRP/USDT",
            name = "Ripple XRP",
            baseAsset = "XRP",
            quoteAsset = "USDT",
            decimalPlaces = 4
        ),
        MarketInstrument(
            id = "ADAUSDT",
            symbol = "ADA/USDT",
            name = "Cardano",
            baseAsset = "ADA",
            quoteAsset = "USDT",
            decimalPlaces = 4
        ),
        MarketInstrument(
            id = "DOGEUSDT",
            symbol = "DOGE/USDT",
            name = "Dogecoin",
            baseAsset = "DOGE",
            quoteAsset = "USDT",
            decimalPlaces = 5
        ),
        MarketInstrument(
            id = "LINKUSDT",
            symbol = "LINK/USDT",
            name = "Chainlink",
            baseAsset = "LINK",
            quoteAsset = "USDT",
            decimalPlaces = 3
        )
    )

    val defaultIds: Set<String> = items.map { it.id }.toSet()
}
