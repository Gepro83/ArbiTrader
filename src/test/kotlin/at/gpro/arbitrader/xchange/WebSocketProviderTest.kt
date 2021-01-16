package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.core.StreamingMarketDataService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException

internal class WebSocketProviderTest {

    @Test
    fun `instantiate calls getOrderBook on streamingMarketDataService for all pairs`() {
        val marketDataServiceMock = mockk<StreamingMarketDataService>(relaxed = true)
        val exchangeMock = mockk<WebSocketExchange>(relaxed = true) {
            every {
                supportedPairs
            } returns listOf(CurrencyPair.ETH_EUR, CurrencyPair.BTC_EUR, CurrencyPair.XRP_EUR)
            every {
                streamingMarketDataService
            } returns marketDataServiceMock

        }
        WebSocketProvider(listOf(exchangeMock), emptyList())

        verify {
            marketDataServiceMock.getOrderBook(XchangePair.ETH_EUR)
            marketDataServiceMock.getOrderBook(XchangePair.BTC_EUR)
            marketDataServiceMock.getOrderBook(XchangePair.XRP_EUR)
        }
    }

    @Test
    internal fun `subscribe exactly pairs from constructor`() {
        val marketDataServiceMock = mockk<StreamingMarketDataService>(relaxed = true)
        val exchangeMock = mockk<WebSocketExchange>(relaxed = true) {
            every {
                supportedPairs
            } returns listOf(CurrencyPair.ETH_EUR, CurrencyPair.BTC_EUR, CurrencyPair.XRP_EUR)
            every {
                streamingMarketDataService
            } returns marketDataServiceMock

        }

        WebSocketProvider(listOf(exchangeMock), listOf(CurrencyPair.BTC_EUR, CurrencyPair.XRP_EUR))
            .getOrderBooks(CurrencyPair.BTC_EUR)

        verify {
            marketDataServiceMock.getOrderBook(org.knowm.xchange.currency.CurrencyPair.BTC_EUR)
            marketDataServiceMock.getOrderBook(org.knowm.xchange.currency.CurrencyPair.XRP_EUR)
        }
        verify(exactly = 0) {
            marketDataServiceMock.getOrderBook(org.knowm.xchange.currency.CurrencyPair.ETH_EUR)
        }

    }

    @Test
    internal fun `exception for getOrderBooks on unsubscribed pair`() {
        val marketDataServiceMock = mockk<StreamingMarketDataService>(relaxed = true)
        val exchangeMock = mockk<WebSocketExchange>(relaxed = true) {
            every {
                supportedPairs
            } returns listOf(CurrencyPair.ETH_EUR, CurrencyPair.BTC_EUR, CurrencyPair.XRP_EUR)
            every {
                streamingMarketDataService
            } returns marketDataServiceMock

        }

        assertThrows<IllegalArgumentException> {
            WebSocketProvider(listOf(exchangeMock), listOf(CurrencyPair.BTC_EUR, CurrencyPair.XRP_EUR))
                .getOrderBooks(CurrencyPair.ETH_EUR)
        }
    }
}