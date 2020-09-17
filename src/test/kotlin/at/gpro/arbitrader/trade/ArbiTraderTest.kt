package at.gpro.arbitrader.trade

import at.gpro.arbitrader.*
import at.gpro.arbitrader.exchange.Exchange
import at.gpro.arbitrader.order.Offer
import at.gpro.arbitrader.order.OrderBook
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal.ONE
import java.math.BigDecimal.TEN

internal class ArbiTraderTest {

    companion object {

        val KRAKEN = object : Exchange {
            override fun toString(): String = "Kraken"
        }
        val COINBASE = object : Exchange {
            override fun toString(): String = "Coinbase"
        }
    }

    @Test
    fun `no arbitrage with some orders`() {
        val trades = ArbiTrader(
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
        ).findTrades()
        assertThat(trades, empty())
    }

    @Test
    fun `expect one arbitrade`() {
        val trades = ArbiTrader(
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
        ).findTrades()

        assertThat(trades, hasItem(
            ArbiTrade(
                BuyTrade(Offer(amount = FIVE, price = THREE), COINBASE),
                SellTrade(Offer(amount = FIVE, price = FOUR), KRAKEN)
            )
        ))
    }

    @Test
    fun `expect one arbitrade with repeated calls to findTrades`() {
        val arbiTrader = ArbiTrader(
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
        arbiTrader.findTrades()
        arbiTrader.findTrades()

        val trades = arbiTrader.findTrades()

        assertThat(trades, hasItem(
            ArbiTrade(
                BuyTrade(Offer(amount = FIVE, price = THREE), COINBASE),
                SellTrade(Offer(amount = FIVE, price = FOUR), KRAKEN)
            )
        ))
    }

    @Test
    fun `expect one arbitrade reversed orderbooks`() {
        val trades = ArbiTrader(
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
        ).findTrades()

        assertThat(trades, hasItem(
            ArbiTrade(
                BuyTrade(Offer(amount = FIVE, price = THREE), COINBASE),
                SellTrade(Offer(amount = FIVE, price = FOUR), KRAKEN)
            )
        ))
    }

    @Test
    fun `expect two arbitrades filling two buy offers and one sell offer without remainder`() {
        val trades = ArbiTrader(
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
        ).findTrades()

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

    @Test
    fun `expect three arbitrades filling two buy offers and two sell offer with remainder`() {
        val trades = ArbiTrader(
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
                    Offer(amount = TEN, price = FOUR)),
                buyOffers = listOf(
                    Offer(amount = TEN, price = TWO),
                    Offer(amount = TEN, price = ONE)
                )
            )
        ).findTrades()

        assertThat(trades, hasItems(
            ArbiTrade(
                BuyTrade(Offer(amount = THREE, price = THREE), COINBASE),
                SellTrade(Offer(amount = THREE, price = SIX), KRAKEN)
            ),
            ArbiTrade(
                BuyTrade(Offer(amount = ONE, price = THREE), COINBASE),
                SellTrade(Offer(amount = ONE, price = FIVE), KRAKEN)
            ),
            ArbiTrade(
                BuyTrade(Offer(amount = ONE, price = FOUR), COINBASE),
                SellTrade(Offer(amount = ONE, price = FIVE), KRAKEN)
            )
        ))
    }

    @Test
    internal fun `empty Orderbooks expect empty trades`() {
        val trades = ArbiTrader(
            OrderBook(COINBASE, emptyList(), emptyList()),
            OrderBook(KRAKEN, emptyList(), emptyList())
        ).findTrades()

        assertThat(trades, empty())
    }

    @Test
    internal fun `unsorted orderbooks with one arbitrade`() {
        val trades = ArbiTrader(
            OrderBook(
                KRAKEN,
                sellOffers = listOf(
                    Offer(amount = TEN, price = SIX),
                    Offer(amount = FIVE, price = TEN),
                    Offer(amount = TEN, price = FIVE)
                ),
                buyOffers = listOf(
                    Offer(amount = TEN, price = TWO),
                    Offer(amount = TEN, price = ONE),
                    Offer(amount = TEN, price = FOUR))
            ),
            OrderBook(
                COINBASE,
                sellOffers = listOf(
                    Offer(amount = TEN, price = FIVE),
                    Offer(amount = FIVE, price = THREE),
                    Offer(amount = ONE, price = TEN)
                ),
                buyOffers = listOf(
                    Offer(amount = TEN, price = ONE),
                    Offer(amount = TEN, price = TWO)
                )
            )
        ).findTrades()

        assertThat(trades, hasItem(
            ArbiTrade(
                BuyTrade(Offer(amount = FIVE, price = THREE), COINBASE),
                SellTrade(Offer(amount = FIVE, price = FOUR), KRAKEN)
            )
        ))
    }

    @Test
    internal fun `unsorted orderbooks with one arbitrade reversed orderbookorder`() {
        val trades = ArbiTrader(
            OrderBook(
                COINBASE,
                sellOffers = listOf(
                    Offer(amount = TEN, price = FIVE),
                    Offer(amount = FIVE, price = THREE),
                    Offer(amount = ONE, price = TEN)
                ),
                buyOffers = listOf(
                    Offer(amount = TEN, price = ONE),
                    Offer(amount = TEN, price = TWO)
                )
            ),
            OrderBook(
                KRAKEN,
                sellOffers = listOf(
                    Offer(amount = TEN, price = SIX),
                    Offer(amount = FIVE, price = TEN),
                    Offer(amount = TEN, price = FIVE)
                ),
                buyOffers = listOf(
                    Offer(amount = TEN, price = TWO),
                    Offer(amount = TEN, price = ONE),
                    Offer(amount = TEN, price = FOUR))
            )
        ).findTrades()

        assertThat(trades, hasItem(
            ArbiTrade(
                BuyTrade(Offer(amount = FIVE, price = THREE), COINBASE),
                SellTrade(Offer(amount = FIVE, price = FOUR), KRAKEN)
            )
        ))
    }
}