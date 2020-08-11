package at.gpro.arbitrader.trade

import at.gpro.arbitrader.exchange.Exchange
import at.gpro.arbitrader.order.Offer
import at.gpro.arbitrader.order.OrderBook
import org.junit.jupiter.api.Assertions.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.TEN

internal class ArbiTraderTest {

    companion object {
        val TWO = BigDecimal(2)
        val THREE = BigDecimal(3)
        val FOUR = BigDecimal(4)
        val FIVE = BigDecimal(5)
        val SIX = BigDecimal(6)

        val KRAKEN = object : Exchange {}
        val COINBASE = object : Exchange {}
    }

    @Test
    fun `no arbitrage with some orders`() {
        val trades = ArbiTrader().findTrades(
            OrderBook(
                KRAKEN,
                listOf(
                    Offer.newSell(amount = TEN, price = THREE), Offer.newSell(amount = TEN, price = FOUR),
                    Offer.newBuy(amount = TEN, price = ONE), Offer.newBuy(amount = TEN, price = TWO)
                )
            ),
            OrderBook(
                COINBASE,
                listOf(
                    Offer.newSell(amount = TEN, price = FOUR), Offer.newSell(amount = TEN, price = FIVE),
                    Offer.newBuy(amount = TEN, price = TWO), Offer.newBuy(amount = TEN, price = THREE)
                )
            )
        )
        assertThat(trades, empty())
    }

    @Test
    fun `expect one specific trade`() {
        val trades = ArbiTrader().findTrades(
            OrderBook(
                KRAKEN,
                listOf(
                    Offer.newSell(amount = TEN, price = FIVE), Offer.newSell(amount = TEN, price = SIX),
                    Offer.newBuy(amount = TEN, price = ONE), Offer.newBuy(amount = TEN, price = FOUR)
                )
            ),
            OrderBook(
                COINBASE,
                listOf(
                    Offer.newSell(amount = FIVE, price = THREE), Offer.newSell(amount = TEN, price = FIVE),
                    Offer.newBuy(amount = TEN, price = ONE), Offer.newBuy(amount = TEN, price = TWO)
                )
            )
        )
        assertThat(trades, hasItems(
            Trade(Offer.newBuy(amount = FIVE, price = THREE), COINBASE),
            Trade(Offer.newSell(amount = FIVE, price = FOUR), KRAKEN)
            )
        )
    }
}