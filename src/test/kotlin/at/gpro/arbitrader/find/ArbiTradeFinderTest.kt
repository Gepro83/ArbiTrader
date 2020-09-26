package at.gpro.arbitrader.find

import at.gpro.arbitrader.*
import at.gpro.arbitrader.entity.BuyOffer
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.SellOffer
import at.gpro.arbitrader.entity.order.Offer
import at.gpro.arbitrader.entity.order.OrderBook
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal.ONE
import java.math.BigDecimal.TEN

internal class ArbiTradeFinderTest {

    companion object {

        val KRAKEN = object : Exchange {
            override fun getName(): String = toString()
            override fun toString(): String = "Kraken"
        }
        val COINBASE = object : Exchange {
            override fun getName(): String = toString()
            override fun toString(): String = "Coinbase"
        }
    }

    @Test
    fun `no arbitrage with some orders`() {
        val trades = ArbiTradeFinder(
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
        val trades = ArbiTradeFinder(
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
                BuyOffer(
                    Offer(amount = FIVE, price = THREE),
                    COINBASE
                ),
                SellOffer(
                    Offer(amount = FIVE, price = FOUR),
                    KRAKEN
                )
            )
        ))
    }

    @Test
    fun `expect one arbitrade with repeated calls to findTrades`() {
        val arbiTrader = ArbiTradeFinder(
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
                BuyOffer(
                    Offer(amount = FIVE, price = THREE),
                    COINBASE
                ),
                SellOffer(
                    Offer(amount = FIVE, price = FOUR),
                    KRAKEN
                )
            )
        ))
    }

    @Test
    fun `expect one arbitrade reversed orderbooks`() {
        val trades = ArbiTradeFinder(
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
                BuyOffer(
                    Offer(amount = FIVE, price = THREE),
                    COINBASE
                ),
                SellOffer(
                    Offer(amount = FIVE, price = FOUR),
                    KRAKEN
                )
            )
        ))
    }

    @Test
    fun `expect two arbitrades filling two buy offers and one sell offer without remainder`() {
        val trades = ArbiTradeFinder(
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
                BuyOffer(
                    Offer(amount = THREE, price = THREE),
                    COINBASE
                ),
                SellOffer(
                    Offer(amount = THREE, price = SIX),
                    KRAKEN
                )
            ),
            ArbiTrade(
                BuyOffer(
                    Offer(amount = ONE, price = THREE),
                    COINBASE
                ),
                SellOffer(
                    Offer(amount = ONE, price = FIVE),
                    KRAKEN
                )
            )
        ))
    }

    @Test
    fun `expect three arbitrades filling two buy offers and two sell offer with remainder`() {
        val trades = ArbiTradeFinder(
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
                BuyOffer(
                    Offer(amount = THREE, price = THREE),
                    COINBASE
                ),
                SellOffer(
                    Offer(amount = THREE, price = SIX),
                    KRAKEN
                )
            ),
            ArbiTrade(
                BuyOffer(
                    Offer(amount = ONE, price = THREE),
                    COINBASE
                ),
                SellOffer(
                    Offer(amount = ONE, price = FIVE),
                    KRAKEN
                )
            ),
            ArbiTrade(
                BuyOffer(
                    Offer(amount = ONE, price = FOUR),
                    COINBASE
                ),
                SellOffer(
                    Offer(amount = ONE, price = FIVE),
                    KRAKEN
                )
            )
        ))
    }

    @Test
    internal fun `empty Orderbooks expect empty trades`() {
        val trades = ArbiTradeFinder(
            OrderBook(COINBASE, emptyList(), emptyList()),
            OrderBook(KRAKEN, emptyList(), emptyList())
        ).findTrades()

        assertThat(trades, empty())
    }

    @Test
    internal fun `unsorted orderbooks with one arbitrade`() {
        val trades = ArbiTradeFinder(
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
                BuyOffer(
                    Offer(amount = FIVE, price = THREE),
                    COINBASE
                ),
                SellOffer(
                    Offer(amount = FIVE, price = FOUR),
                    KRAKEN
                )
            )
        ))
    }

    @Test
    internal fun `unsorted orderbooks with one arbitrade reversed orderbookorder`() {
        val trades = ArbiTradeFinder(
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
                BuyOffer(
                    Offer(amount = FIVE, price = THREE),
                    COINBASE
                ),
                SellOffer(
                    Offer(amount = FIVE, price = FOUR),
                    KRAKEN
                )
            )
        ))
    }
}