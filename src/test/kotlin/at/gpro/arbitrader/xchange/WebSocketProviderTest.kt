package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.core.StreamingMarketDataService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

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
        WebSocketProvider(listOf(exchangeMock))

        verify {
            marketDataServiceMock.getOrderBook(XchangePair.ETH_EUR)
            marketDataServiceMock.getOrderBook(XchangePair.BTC_EUR)
            marketDataServiceMock.getOrderBook(XchangePair.XRP_EUR)
        }
    }
}