package at.gpro.arbitrader.execute

import at.gpro.arbitrader.entity.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.lessThan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

internal class CombineTradesTest {

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

        override fun getBalance(currency: Currency) = BigDecimal(1000000)
    }

    private class SlowExchange(private val placeDelay: Long): Exchange {
        override fun getName(): String = "SlowExchange"
        override fun getFee(): Double = 0.0
        override fun place(order: Order) {
            Thread.sleep(placeDelay)
        }

        override fun getBalance(currency: Currency) = BigDecimal(10000000)
    }

    private class TestScoredTrade(
        override val amount: BigDecimal,
        override val buyPrice: BigDecimal,
        override val sellPrice: BigDecimal,
        override val score: BigDecimal
    ) : ScoredArbiTrade

    @Test
    fun `MockExchange keeping placedOrders`() {
        val order = Order.ask(BigDecimal.ZERO, CurrencyPair.BTC_EUR)
        val exchange = MockExchange()
        exchange.place(order)
        assertThat(exchange.placedOrders, contains(order))
    }

    @Test
    fun `place trade at buy and sell exchange`() {
        val placer = MarketPlacer()

        placer.placeTrades(
            CurrencyPair.BTC_EUR,
            mockBuyExchange,
            mockSellExchange,
            listOf(
                    TestScoredTrade(
                        score = BigDecimal(1),
                        amount = BigDecimal(1),
                        buyPrice = BigDecimal(10),
                        sellPrice = BigDecimal(11),
                    )
            )
        )

        assertThat(mockBuyExchange.placedOrders, contains(Order.bid(BigDecimal(1), CurrencyPair.BTC_EUR)))
        assertThat(mockSellExchange.placedOrders, contains(Order.ask(BigDecimal(1), CurrencyPair.BTC_EUR)))
    }

    @Test
    fun `execute trades in paralell`() {
        val placer = MarketPlacer()

        val before = System.currentTimeMillis()

        placer.placeTrades(
            CurrencyPair.BTC_EUR,
            SlowExchange(300),
            SlowExchange(300),
            listOf(
                TestScoredTrade(
                    score = BigDecimal(1),
                    amount = BigDecimal(1),
                    buyPrice = BigDecimal(10),
                    sellPrice = BigDecimal(11)
                ),
                TestScoredTrade(
                    score = BigDecimal(2),
                    amount = BigDecimal(1),
                    buyPrice = BigDecimal(10),
                    sellPrice = BigDecimal(11),
                )
            )
        )

        val after = System.currentTimeMillis()

        assertThat(after - before, lessThan(600))
    }

    @Test
    fun `exception in place trade propagating`() {

        assertThrows<RuntimeException> {
            MarketPlacer().placeTrades(
                CurrencyPair.BTC_EUR,
                SlowExchange(300),
                object : Exchange {
                    override fun getName(): String = "asd"
                    override fun getFee(): Double = 1.0
                    override fun place(order: Order) {
                        Thread.sleep(50)
                        throw RuntimeException()
                    }
                    override fun getBalance(currency: Currency) = BigDecimal(100000)
                },
                listOf(
                    TestScoredTrade(
                        score = BigDecimal(1),
                        amount = BigDecimal(1),
                        buyPrice = BigDecimal(10),
                        sellPrice = BigDecimal(11)
                    )
                )
            )
        }
    }

    @Test
    fun `combine trades before placing`() {
        MarketPlacer().placeTrades(
            CurrencyPair.BTC_EUR,
            mockBuyExchange,
            mockSellExchange,
            listOf(
                TestScoredTrade(
                    score = BigDecimal(1),
                    amount = BigDecimal.ONE,
                    buyPrice = BigDecimal(100),
                    sellPrice = BigDecimal(110)
                ),
                TestScoredTrade(
                    score = BigDecimal(1),
                    amount = BigDecimal.TEN,
                    buyPrice = BigDecimal(100),
                    sellPrice = BigDecimal(110)
                ),
            )
        )

        assertThat(mockBuyExchange.placedOrders, contains(Order.bid(BigDecimal(11), CurrencyPair.BTC_EUR)))
        assertThat(mockSellExchange.placedOrders, contains(Order.ask(BigDecimal(11), CurrencyPair.BTC_EUR)))
    }


}