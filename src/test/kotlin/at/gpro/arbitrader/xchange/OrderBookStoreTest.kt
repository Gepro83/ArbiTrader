package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.ANOTHER_EMPTY_TEST_EXCHANGE
import at.gpro.arbitrader.EMPTY_TEST_EXCHANGE
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.order.Offer
import at.gpro.arbitrader.entity.order.OrderBook
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

        val anotherTestOrderBook = OrderBook(
            ANOTHER_EMPTY_TEST_EXCHANGE,
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
        orderBookStore.update(anotherTestOrderBook, CurrencyPair.XRP_EUR)

        assertThat(orderBookStore.getBooksFor(CurrencyPair.XRP_EUR), containsInAnyOrder(testOrderBook, anotherTestOrderBook))
    }

    @Test
    internal fun `getBooksFor returns only orderbook for given currency`() {
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

        orderBookStore.update(testOrderBook, CurrencyPair.BTC_EUR)

        assertThat(orderBookStore.getBooksFor(CurrencyPair.BTC_EUR), contains(updatedOrderBook))
    }
}