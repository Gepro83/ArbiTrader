package at.gpro.arbitrader.execute

import at.gpro.arbitrader.entity.*
import at.gpro.arbitrader.entity.Currency
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.lessThan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.*

internal class CombineTradesTest {

    private val mockBuyExchange = MockExchange()
    private val mockSellExchange = MockExchange()

    private val randomOrder : (ArbiTrade, ArbiTrade) -> Int = { _, _ ->
        if(Random().nextBoolean())
            1
        else
            -1
    }

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
                    SimpleArbiTrade(
                        amount = BigDecimal(1),
                        buyPrice = BigDecimal(10),
                        sellPrice = BigDecimal(11),
                    )
            ).toSortedSet(randomOrder)
        )

        assertThat(mockBuyExchange.placedOrders, contains(Order.ask(BigDecimal(1), CurrencyPair.BTC_EUR)))
        assertThat(mockSellExchange.placedOrders, contains(Order.bid(BigDecimal(1), CurrencyPair.BTC_EUR)))
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
                SimpleArbiTrade(
                    amount = BigDecimal(1),
                    buyPrice = BigDecimal(10),
                    sellPrice = BigDecimal(11)
                ),
                SimpleArbiTrade(
                    amount = BigDecimal(1),
                    buyPrice = BigDecimal(10),
                    sellPrice = BigDecimal(11),
                )
            ).toSortedSet(randomOrder)
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
                    SimpleArbiTrade(
                        amount = BigDecimal(1),
                        buyPrice = BigDecimal(10),
                        sellPrice = BigDecimal(11)
                    )
                ).toSortedSet(randomOrder)
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
                SimpleArbiTrade(
                    amount = BigDecimal.ONE,
                    buyPrice = BigDecimal(100),
                    sellPrice = BigDecimal(110)
                ),
                SimpleArbiTrade(
                    amount = BigDecimal.TEN,
                    buyPrice = BigDecimal(100),
                    sellPrice = BigDecimal(110)
                ),
            ).toSortedSet(randomOrder)
        )

        assertThat(mockBuyExchange.placedOrders, contains(Order.ask(BigDecimal(11), CurrencyPair.BTC_EUR)))
        assertThat(mockSellExchange.placedOrders, contains(Order.bid(BigDecimal(11), CurrencyPair.BTC_EUR)))
    }


}