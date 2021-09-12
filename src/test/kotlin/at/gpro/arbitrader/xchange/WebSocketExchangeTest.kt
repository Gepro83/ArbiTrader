package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.testutils.runInThread
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.core.ProductSubscription
import info.bitrich.xchangestream.core.StreamingExchange
import info.bitrich.xchangestream.core.StreamingTradeService
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.knowm.xchange.ExchangeSpecification
import org.knowm.xchange.currency.Currency
import org.knowm.xchange.dto.Order
import org.knowm.xchange.dto.account.AccountInfo
import org.knowm.xchange.dto.account.Balance
import org.knowm.xchange.dto.account.Wallet
import org.knowm.xchange.dto.meta.ExchangeMetaData
import org.knowm.xchange.dto.trade.MarketOrder
import org.knowm.xchange.service.account.AccountService
import org.knowm.xchange.service.marketdata.MarketDataService
import org.knowm.xchange.service.trade.TradeService
import si.mazi.rescu.SynchronizedValueFactory
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WebSocketExchangeTest {


    val orderChangesObservable = PublishSubject.create<Order>()

    val testOrderId = "some order id"

    var btcBalance = BigDecimal("10.0")
    var eurBalance = BigDecimal("2000.0")

    var placedTrade: MarketOrder? = null

    val mockExchange = object : StreamingExchange {
        override fun getExchangeSpecification(): ExchangeSpecification? = null
        override fun getExchangeMetaData(): ExchangeMetaData? = null
        override fun getExchangeSymbols(): MutableList<XchangePair>? = null
        override fun getNonceFactory(): SynchronizedValueFactory<Long>? = null
        override fun getDefaultExchangeSpecification(): ExchangeSpecification? = null
        override fun applySpecification(exchangeSpecification: ExchangeSpecification?) {}
        override fun getMarketDataService(): MarketDataService? = null
        override fun getTradeService(): TradeService {
            return object : TradeService {
                override fun placeMarketOrder(marketOrder: MarketOrder?): String {
                    placedTrade = marketOrder
                    return testOrderId
                }
            }
        }

        override fun getAccountService(): AccountService {
            return object: AccountService {
                override fun getAccountInfo(): AccountInfo {
                    return AccountInfo(Wallet("walletid", "walletname", mutableListOf(
                        Balance(Currency.BTC, btcBalance),
                        Balance(Currency.EUR, eurBalance),
                    ), emptySet(), BigDecimal.ZERO, BigDecimal.ZERO))
                }
            }
        }

        override fun remoteInit() {}
        override fun connect(vararg args: ProductSubscription?): Completable? = null
        override fun disconnect(): Completable? = null
        override fun isAlive(): Boolean = true
        override fun useCompressedMessages(compressedMessages: Boolean) {}
        override fun getStreamingTradeService(): StreamingTradeService {
            return object : StreamingTradeService {
                override fun getOrderChanges(currencyPair: XchangePair?, vararg args: Any?): Observable<Order> {
                    return orderChangesObservable
                }
            }
        }

    }

    @Test
    internal fun `place trade blocking`() {
        val orderPlacedLatch = CountDownLatch(1)

        runInThread {
            WebSocketExchange(mockExchange, 0.0, listOf(CurrencyPair.BTC_EUR))
                .place(XchangePair.BTC_EUR, XchangeOrderType.BID, BigDecimal(3))
            orderPlacedLatch.countDown()
        }

        assertThat(orderPlacedLatch.count, `is`(1L))
    }

    @Test
    internal fun `place trade blocking for order change with wrong id`() {
        val orderPlacedLatch = CountDownLatch(1)

        runInThread {
            WebSocketExchange(mockExchange, 0.0, listOf(CurrencyPair.BTC_EUR))
                .place(XchangePair.BTC_EUR, XchangeOrderType.BID, BigDecimal(3))
            orderPlacedLatch.countDown()
        }

        orderChangesObservable.onNext(
            MarketOrder.Builder(XchangeOrderType.BID, XchangePair.BTC_EUR)
                .originalAmount(BigDecimal(3))
                .id("wrong id")
                .orderStatus(Order.OrderStatus.FILLED)
                .build()
        )

        assertThat(orderPlacedLatch.count, `is`(1L))
    }

    @Test
    internal fun `place trade blocking for order change with status OPEN`() {
        val orderPlacedLatch = CountDownLatch(1)

        runInThread {
            WebSocketExchange(mockExchange, 0.0, listOf(CurrencyPair.BTC_EUR))
                .place(XchangePair.BTC_EUR, XchangeOrderType.BID, BigDecimal(3))
            orderPlacedLatch.countDown()
        }

        orderChangesObservable.onNext(
            MarketOrder.Builder(XchangeOrderType.BID, XchangePair.BTC_EUR)
                .originalAmount(BigDecimal(3))
                .id(testOrderId)
                .orderStatus(Order.OrderStatus.OPEN)
                .build()
        )

        assertThat(orderPlacedLatch.count, `is`(1L))
    }

    @Test
    internal fun `place trade blocking until correct order change with status FILLED`() {
        val orderPlacedLatch = CountDownLatch(1)

        runInThread {
            WebSocketExchange(mockExchange, 0.0, listOf(CurrencyPair.BTC_EUR))
                .place(XchangePair.BTC_EUR, XchangeOrderType.BID, BigDecimal(3))
            orderPlacedLatch.countDown()
        }

        orderChangesObservable.onNext(
            MarketOrder.Builder(XchangeOrderType.BID, XchangePair.BTC_EUR)
                .originalAmount(BigDecimal(3))
                .id(testOrderId)
                .orderStatus(Order.OrderStatus.FILLED)
                .build()
        )

        assertTrue(orderPlacedLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    internal fun `amount cut to pair scale`() {
        val orderPlacedLatch = CountDownLatch(1)

        runInThread {
            WebSocketExchange(mockExchange, 0.0, listOf(CurrencyPair.BTC_EUR))
                .place(XchangePair.BTC_EUR, XchangeOrderType.BID, BigDecimal("3.000001"))
            orderPlacedLatch.countDown()
        }

        orderChangesObservable.onNext(
            MarketOrder.Builder(XchangeOrderType.BID, XchangePair.BTC_EUR)
                .originalAmount(BigDecimal(3))
                .id(testOrderId)
                .orderStatus(Order.OrderStatus.FILLED)
                .build()
        )
        assertThat(placedTrade?.originalAmount, `is`(BigDecimal("3.00000")))
    }

    @Test
    internal fun `minimum pair scale possible`() {
        val orderPlacedLatch = CountDownLatch(1)

        runInThread {
            WebSocketExchange(mockExchange, 0.0, listOf(CurrencyPair.BTC_EUR))
                .place(XchangePair.BTC_EUR, XchangeOrderType.BID, BigDecimal("3.00001"))
            orderPlacedLatch.countDown()
        }

        orderChangesObservable.onNext(
            MarketOrder.Builder(XchangeOrderType.BID, XchangePair.BTC_EUR)
                .originalAmount(BigDecimal(3))
                .id(testOrderId)
                .orderStatus(Order.OrderStatus.FILLED)
                .build()
        )
        assertThat(placedTrade?.originalAmount, `is`(BigDecimal("3.00001")))
    }



}