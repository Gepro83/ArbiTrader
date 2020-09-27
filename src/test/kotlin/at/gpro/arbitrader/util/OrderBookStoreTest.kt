package at.gpro.arbitrader.util

import at.gpro.arbitrader.ANOTHER_EMPTY_TEST_EXCHANGE
import at.gpro.arbitrader.EMPTY_TEST_EXCHANGE
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.order.Offer
import at.gpro.arbitrader.entity.order.OrderBook
import at.gpro.arbitrader.util.time.ManualClock
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test

internal class OrderBookStoreTest {

    private val orderBookStore = OrderBookStore()

    @Test
    internal fun `getBooksFor currency not present expect empty list`() {
        assertThat(orderBookStore.getBooksFor(CurrencyPair.XRP_EUR), empty())
    }

    @Test
    internal fun `getBooksFor returns latest oderbook for repeated updates of same exchange`() {
        val testOrderBook = OrderBook(
            EMPTY_TEST_EXCHANGE,
            listOf(
                Offer(1, 12),
                Offer(2, 14)
            ),
            listOf(
                Offer(3, 5),
                Offer(1, 3)
            )
        )

        orderBookStore.update(testOrderBook, CurrencyPair.XRP_EUR)

        val updatedOrderBook = testOrderBook.copy(buyOffers = listOf(Offer(1, 20)))

        orderBookStore.update(updatedOrderBook, CurrencyPair.XRP_EUR)

        assertThat(orderBookStore.getBooksFor(CurrencyPair.XRP_EUR), contains(updatedOrderBook))
    }

    @Test
    internal fun `getBooksFor returns one orderbook per exchange`() {
        val testOrderBook = OrderBook(
            EMPTY_TEST_EXCHANGE,
            listOf(
                Offer(1, 12),
                Offer(2, 14)
            ),
            listOf(
                Offer(3, 5),
                Offer(1, 3)
            )
        )

        val otherExchangeOrderBook = OrderBook(
            ANOTHER_EMPTY_TEST_EXCHANGE,
            listOf(
                Offer(1, 12),
                Offer(2, 15)
            ),
            listOf(
                Offer(2, 5),
                Offer(5, 1)
            )
        )

        orderBookStore.update(testOrderBook, CurrencyPair.XRP_EUR)
        orderBookStore.update(otherExchangeOrderBook, CurrencyPair.XRP_EUR)

        assertThat(orderBookStore.getBooksFor(CurrencyPair.XRP_EUR), containsInAnyOrder(testOrderBook, otherExchangeOrderBook))
    }

    @Test
    internal fun `getBooksFor returns only orderbook for given currency single exchange`() {
        val testOrderBook = OrderBook(
            EMPTY_TEST_EXCHANGE,
            listOf(
                Offer(1, 12),
                Offer(2, 14)
            ),
            listOf(
                Offer(2, 5),
                Offer(1, 3)
            )
        )

        orderBookStore.update(testOrderBook, CurrencyPair.XRP_EUR)

        val updatedOrderBook = testOrderBook.copy(buyOffers = listOf(Offer(4, 15)))

        orderBookStore.update(updatedOrderBook, CurrencyPair.BTC_EUR)

        assertThat(orderBookStore.getBooksFor(CurrencyPair.BTC_EUR), contains(updatedOrderBook))
    }

    @Test
    internal fun `getBooksFor returns latest orderbook with two exchanges and updates of multiple currencies`() {
        val testOrderBook = OrderBook(
            EMPTY_TEST_EXCHANGE,
            listOf(
                Offer(1, 12),
                Offer(2, 14)
            ),
            listOf(
                Offer(2, 5),
                Offer(1, 3)
            )
        )

        val otherExchangeOrderBook = OrderBook(
            ANOTHER_EMPTY_TEST_EXCHANGE,
            listOf(
                Offer(1, 12),
                Offer(2, 17)
            ),
            listOf(
                Offer(5, 6),
                Offer(2, 2)
            )
        )

        orderBookStore.update(testOrderBook, CurrencyPair.XRP_EUR)
        orderBookStore.update(otherExchangeOrderBook, CurrencyPair.XRP_EUR)

        val updatedOtherExchangeOrderBook = otherExchangeOrderBook.copy(buyOffers = listOf(Offer(4, 15)))

        orderBookStore.update(updatedOtherExchangeOrderBook, CurrencyPair.BTC_EUR)

        assertThat(orderBookStore.getBooksFor(CurrencyPair.XRP_EUR), containsInAnyOrder(testOrderBook, otherExchangeOrderBook))
    }

    @Test
    internal fun `orderbook no longer present after period expired`() {
        val clock = ManualClock()
        val manualOrderBookStore = OrderBookStore(clock)
        val testOrderBook = OrderBook(
            EMPTY_TEST_EXCHANGE,
            listOf(
                Offer(1, 7),
                Offer(5, 14)
            ),
            listOf(
                Offer(2, 4),
                Offer(3, 2)
            )
        )

        manualOrderBookStore.update(testOrderBook, CurrencyPair.XRP_EUR)

        clock.expireTimers()

        assertThat(manualOrderBookStore.getBooksFor(CurrencyPair.XRP_EUR), empty())

    }
}