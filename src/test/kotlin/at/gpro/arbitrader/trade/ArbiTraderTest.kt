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
        val SEVEN = BigDecimal(7)

        val KRAKEN = object : Exchange {}
        val COINBASE = object : Exchange {}
    }

    @Test
    fun `no arbitrage with some orders`() {
        val trades = ArbiTrader().findTrades(
            OrderBook(
                exchange = KRAKEN,
                sellOffers = listOf(Offer(amount = TEN, price = THREE), Offer(amount = TEN, price = FOUR)),
                buyOffers = listOf(Offer(amount = TEN, price = TWO), Offer(amount = TEN, price = ONE))
            ),
            OrderBook(
                COINBASE,
                sellOffers = listOf(Offer(amount = TEN, price = FOUR), Offer(amount = TEN, price = FIVE)),
                buyOffers = listOf(Offer(amount = TEN, price = THREE), Offer(amount = TEN, price = TWO))
            )
        )
        assertThat(trades, empty())
    }

    @Test
    fun `expect one arbitrade`() {
        val trades = ArbiTrader().findTrades(
            OrderBook(
                KRAKEN,
                sellOffers = listOf(Offer(amount = TEN, price = FIVE), Offer(amount = TEN, price = SIX)),
                buyOffers = listOf(Offer(amount = TEN, price = FOUR), Offer(amount = TEN, price = ONE))
            ),
            OrderBook(
                COINBASE,
                sellOffers = listOf(Offer(amount = FIVE, price = THREE), Offer(amount = TEN, price = FIVE)),
                buyOffers = listOf(Offer(amount = TEN, price = TWO), Offer(amount = TEN, price = ONE))
            )
        )

        assertThat(trades, hasItem(
            ArbiTrade(
                BuyTrade(Offer(amount = FIVE, price = THREE), COINBASE),
                SellTrade(Offer(amount = FIVE, price = FOUR), KRAKEN)
            )
        ))
    }

    @Test
    fun `expect one arbitrade reversed exchanges`() {
        val trades = ArbiTrader().findTrades(
            OrderBook(
                COINBASE,
                sellOffers = listOf(Offer(amount = FIVE, price = THREE), Offer(amount = TEN, price = FIVE)),
                buyOffers = listOf(Offer(amount = TEN, price = TWO), Offer(amount = TEN, price = ONE))
            ),
            OrderBook(
                KRAKEN,
                sellOffers = listOf(Offer(amount = TEN, price = FIVE), Offer(amount = TEN, price = SIX)),
                buyOffers = listOf(Offer(amount = TEN, price = FOUR), Offer(amount = TEN, price = ONE))
            )
        )

        assertThat(trades, hasItem(
            ArbiTrade(
                BuyTrade(Offer(amount = FIVE, price = THREE), COINBASE),
                SellTrade(Offer(amount = FIVE, price = FOUR), KRAKEN)
            )
        ))
    }

    @Test
    fun `expect two arbitrades`() {
        val trades = ArbiTrader().findTrades(
            OrderBook(
                KRAKEN,
                sellOffers = listOf(
                    Offer(amount = TEN, price = SEVEN),
                    Offer(amount = TEN, price = TEN)),
                buyOffers = listOf(
                    Offer(amount = THREE, price = SIX),
                    Offer(amount = TWO, price = FIVE),
                    Offer(amount = FOUR, price = TWO)
                )
            ),
            OrderBook(
                COINBASE,
                sellOffers = listOf(
                    Offer(amount = FOUR, price = THREE),
                    Offer(amount = TEN, price = SIX)),
                buyOffers = listOf(
                    Offer(amount = TEN, price = TWO),
                    Offer(amount = TEN, price = ONE)
                )
            )
        )

        assertThat(trades, hasItems(
            ArbiTrade(
                BuyTrade(Offer(amount = THREE, price = THREE), COINBASE),
                SellTrade(Offer(amount = THREE, price = SIX), KRAKEN)
            ),
            ArbiTrade(
                BuyTrade(Offer(amount = ONE, price = THREE), COINBASE),
                SellTrade(Offer(amount = ONE, price = FIVE), KRAKEN)
            )
        ))
    }
}