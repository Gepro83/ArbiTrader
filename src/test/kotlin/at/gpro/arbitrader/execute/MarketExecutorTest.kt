package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradeExecutor
import at.gpro.arbitrader.entity.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains

import org.junit.jupiter.api.Test
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



}