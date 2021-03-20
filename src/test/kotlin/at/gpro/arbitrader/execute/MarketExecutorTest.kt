package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradeExecutor
import at.gpro.arbitrader.entity.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.lessThan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

internal class MarketExecutorTest {

    private val mockBuyExchange = MockExchange()
    private val mockSellExchange = MockExchange()

    private class MockExchange : Exchange {

        private val orders: MutableList<Order> = ArrayList()

        val placedOrders: List<Order>
            get() = orders

        override fun getName(): String = "MockExchange"
        override fun getFee(): Double = 0.0
        override fun place(order: Order) {
            orders.add(order)
        }
    }

    private class SlowExchange(private val placeDelay: Long): Exchange {
        override fun getName(): String = "SlowExchange"
        override fun getFee(): Double = 0.0
        override fun place(order: Order) {
            Thread.sleep(placeDelay)
        }

    }

    @Test
    fun `MockExchange keeping placedOrders`() {
        val order = Order.ask(BigDecimal.ZERO, CurrencyPair.BTC_EUR)
        val exchange = MockExchange()
        exchange.place(order)
        assertThat(exchange.placedOrders, contains(order))
    }

    @Test
    fun `place trade at buy and sell exchange`() {
        val executor: TradeExecutor = MarketExecutor()

        executor.executeTrades(
            listOf(
                CurrencyTrade(
                    CurrencyPair.BTC_EUR,
                    ArbiTrade(
                        1,
                        buyPrice = ExchangePrice(10, mockBuyExchange),
                        sellPrice = ExchangePrice(11, mockSellExchange),
                    )
                )
            ))

        assertThat(mockBuyExchange.placedOrders, contains(Order.ask(BigDecimal(1), CurrencyPair.BTC_EUR)))
        assertThat(mockSellExchange.placedOrders, contains(Order.bid(BigDecimal(1), CurrencyPair.BTC_EUR)))
    }

    @Test
    fun `execute trades in paralell`() {
        val executor = MarketExecutor()

        val before = System.currentTimeMillis()

        executor.executeTrades(
            listOf(
                CurrencyTrade(
                    CurrencyPair.BTC_EUR,
                    ArbiTrade(
                        1,
                        buyPrice = ExchangePrice(10, SlowExchange(300)),
                        sellPrice = ExchangePrice(11, SlowExchange(300)),
                    )
                ),
                CurrencyTrade(
                    CurrencyPair.BTC_EUR,
                    ArbiTrade(
                        1,
                        buyPrice = ExchangePrice(10, SlowExchange(300)),
                        sellPrice = ExchangePrice(11, SlowExchange(300)),
                    )
                )
            )
        )

        val after = System.currentTimeMillis()

        assertThat(after - before, lessThan(600))
    }

    @Test
    fun `exception in place trade propagating`() {

        assertThrows<RuntimeException> {
            MarketExecutor().executeTrades(
                listOf(
                    CurrencyTrade(
                        CurrencyPair.BTC_EUR,
                        ArbiTrade(
                            1,
                            buyPrice = ExchangePrice(10, SlowExchange(300)),
                            sellPrice = ExchangePrice(11, object : Exchange {
                                override fun getName(): String = "asd"
                                override fun getFee(): Double = 1.0
                                override fun place(order: Order) {
                                    Thread.sleep(50)
                                    throw RuntimeException()
                                }
                            }),
                        )
                    )
                )
            )
        }
    }
}