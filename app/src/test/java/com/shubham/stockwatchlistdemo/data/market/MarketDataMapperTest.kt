package com.shubham.stockwatchlistdemo.data.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MarketDataMapperTest {

    private lateinit var mapper: MarketDataMapper

    @Before
    fun setUp() {
        mapper = MarketDataMapper()
    }

    @Test
    fun parsePriceUpdate_validCombinedPayload_returnsCorrectModel() {
        val jsonPayload = """
            {
                "stream": "btcusdt@trade",
                "data": {
                    "e": "trade",
                    "E": 1729837200000,
                    "s": "BTCUSDT",
                    "t": 12345678,
                    "p": "68542.50",
                    "q": "0.05432",
                    "T": 1729837200123
                }
            }
        """.trimIndent()

        val update = mapper.parsePriceUpdate(jsonPayload)

        assertNotNull(update)
        assertEquals("BTCUSDT", update?.instrumentId)
        assertEquals(68542.50, update?.price ?: 0.0, 0.001)
        assertEquals(0.05432, update?.quantity ?: 0.0, 0.00001)
        assertEquals(1729837200123L, update?.timestamp)
    }

    @Test
    fun parsePriceUpdate_validDirectPayload_returnsCorrectModel() {
        val jsonPayload = """
            {
                "e": "trade",
                "E": 1729837200000,
                "s": "ethusdt",
                "t": 987654,
                "p": "2540.75",
                "q": "1.25",
                "T": 1729837200555
            }
        """.trimIndent()

        val update = mapper.parsePriceUpdate(jsonPayload)

        assertNotNull(update)
        assertEquals("ETHUSDT", update?.instrumentId)
        assertEquals(2540.75, update?.price ?: 0.0, 0.001)
    }

    @Test
    fun parsePriceUpdate_malformedJson_returnsNullWithoutCrashing() {
        val malformedJson = "{ stream: btcusdt, data: { broken... "

        val update = mapper.parsePriceUpdate(malformedJson)

        assertNull(update)
    }

    @Test
    fun parsePriceUpdate_subscriptionAck_returnsNull() {
        val ackJson = """{"result":null,"id":1}"""

        val update = mapper.parsePriceUpdate(ackJson)

        assertNull(update)
    }

    @Test
    fun parsePriceUpdate_emptyOrBlankString_returnsNull() {
        assertNull(mapper.parsePriceUpdate(""))
        assertNull(mapper.parsePriceUpdate("   "))
    }

    @Test
    fun parsePriceUpdate_missingPrice_returnsNull() {
        val jsonPayload = """
            {
                "stream": "btcusdt@trade",
                "data": {
                    "e": "trade",
                    "s": "BTCUSDT"
                }
            }
        """.trimIndent()

        assertNull(mapper.parsePriceUpdate(jsonPayload))
    }

    @Test
    fun parsePriceUpdate_zeroOrNegativePrice_returnsNull() {
        val zeroPayload = """{"data":{"s":"BTCUSDT","p":"0.00"}}"""
        val negativePayload = """{"data":{"s":"BTCUSDT","p":"-150.00"}}"""

        assertNull(mapper.parsePriceUpdate(zeroPayload))
        assertNull(mapper.parsePriceUpdate(negativePayload))
    }

    @Test
    fun formatPrice_formatsWithSpecifiedDecimalPlaces() {
        assertEquals("$68,542.50", MarketDataMapper.formatPrice(68542.5, 2))
        assertEquals("$0.5234", MarketDataMapper.formatPrice(0.5234, 4))
        assertEquals("$0.12345", MarketDataMapper.formatPrice(0.12345, 5))
        assertEquals("$125", MarketDataMapper.formatPrice(125.0, 0))
    }

    @Test
    fun formatPercentChange_formatsCorrectlyWithSign() {
        assertEquals("+2.45%", MarketDataMapper.formatPercentChange(2.45))
        assertEquals("-1.10%", MarketDataMapper.formatPercentChange(-1.1))
        assertEquals("0.00%", MarketDataMapper.formatPercentChange(0.0))
    }
}
