package at.gpro.arbitrader.xchange.utils

import at.gpro.arbitrader.EMPTY_TEST_EXCHANGE
import at.gpro.arbitrader.entity.order.Offer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import java.util.*

internal class OrderBookConverterTest {

    @Test
    fun `empty orderbook`() {
        val convertedBook = OrderBookConverter()
            .convert(
                XchangeOrderBook(
                    Date(),
                    emptyList(),
                    emptyList()
                ), EMPTY_TEST_EXCHANGE
            )
        assertThat(convertedBook.buyOffers, empty())
        assertThat(convertedBook.sellOffers, empty())
        assertThat(convertedBook.exchange, `is`(EMPTY_TEST_EXCHANGE))
    }

    @Test
    fun `2 asks 2 bids`() {
        val convertedBook = OrderBookConverter().convert(
            XchangeOrderBook(
                Date(),
                listOf(
                    TestUtils.makeAskOrder(1, 12),
                    TestUtils.makeAskOrder(2, 13)
                ),
                listOf(
                    TestUtils.makeBidOrder(3, 5),
                    TestUtils.makeBidOrder(1, 4)
                )
            ),
            EMPTY_TEST_EXCHANGE
        )

        assertThat(convertedBook.sellOffers, contains(
            Offer(amount = 1, price = 12),
            Offer(amount = 2, price = 13)
        ))
        assertThat(convertedBook.buyOffers, contains(
            Offer(amount = 3, price = 5),
            Offer(amount = 1, price = 4)
        ))
    }

}