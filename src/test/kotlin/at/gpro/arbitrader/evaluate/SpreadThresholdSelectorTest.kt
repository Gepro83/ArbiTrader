package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.TestUtils.newTestExchange
import at.gpro.arbitrader.entity.*
import at.gpro.arbitrader.entity.Currency
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

internal class SpreadThresholdSelectorTest {

    private class CollectingExchange(
        private val fee: Double = 0.0,
        private val intBalance: Int = 1000
        ): Exchange {

        private val placedOrders = ArrayList<Order>()

        fun getPlacedOrders(): List<Order> = placedOrders

        override fun getName(): String = UUID.randomUUID().toString()

        override fun getFee(): Double = fee

        override fun place(order: Order) {
            placedOrders.add(order)
        }

        override fun getBalance(pair: Currency): BigDecimal = BigDecimal(intBalance)

    }

    @Test
    fun `drop trades with too high fees`() {
        val tooExpensiveTrade = ArbiTrade(
            BigDecimal.ONE,
            buyPrice = ExchangePrice(100, newTestExchange("testexchange1", 0.04)),
            sellPrice = ExchangePrice(109, newTestExchange("testexchange1", 0.06)),
        )
        val worthyTrade = ArbiTrade(
            BigDecimal.ONE,
            buyPrice = ExchangePrice(100, newTestExchange("testexchange1", 0.04)),
            sellPrice = ExchangePrice(111, newTestExchange("testexchange1", 0.06)),
        )
        val selectedTrades = SpreadThresholdSelector(0.0).selectTrades(
            listOf(
                tooExpensiveTrade,
                worthyTrade
            )
        )

        assertThat(selectedTrades, contains(worthyTrade))
    }

}