package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.xchange.utils.TestUtils
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.core.StreamingMarketDataService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.reactivex.functions.Consumer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.knowm.xchange.dto.marketdata.OrderBook
import java.util.*

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
            marketDataServiceMock.getOrderBook(XchangePair.BTC_EUR)
            marketDataServiceMock.getOrderBook(XchangePair.XRP_EUR)
        }
        verify(exactly = 0) {
            marketDataServiceMock.getOrderBook(XchangePair.ETH_EUR)
        }

    }

    @Test
    internal fun `onUpdate called when update comes`() {
        val subscription = slot<Consumer<OrderBook>>()
        val marketDataServiceMock = mockk<StreamingMarketDataService>(relaxed = true) {
            every {
                getOrderBook(XchangePair.ETH_EUR)
            } returns mockk {
                every { subscribe(capture(subscription), any()) }  returns mockk(relaxed = true)
            }
        }
        val exchangeMock = mockk<WebSocketExchange>(relaxed = true) {
            every { supportedPairs} returns listOf(CurrencyPair.ETH_EUR)
            every { streamingMarketDataService} returns marketDataServiceMock
        }

        var updateCalled = false
        WebSocketProvider(listOf(exchangeMock), listOf(CurrencyPair.ETH_EUR)).onUpdate { updateCalled = true }

        assertFalse(updateCalled)
        subscription.captured.accept(
            OrderBook(
                Date(),
                listOf(TestUtils.makeBidOrder(1, 2)),
                listOf(TestUtils.makeAskOrder(21, 32))
            )
        )
        assertTrue(updateCalled)
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