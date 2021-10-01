package at.gpro.arbitrader.entity.order

import at.gpro.arbitrader.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal.ONE
import java.math.BigDecimal.TEN

internal class OrderBookTest {

    @Test
    fun asSorted() {

        assertThat(
            OrderBook(
                EMPTY_TEST_EXCHANGE,
                sellOffers = listOf(
                    Offer(amount = TEN, price = SIX),
                    Offer(amount = FIVE, price = TEN),
                    Offer(amount = TEN, price = FIVE)
                ),
                buyOffers = listOf(
                    Offer(amount = TEN, price = TWO),
                    Offer(amount = TEN, price = ONE),
                    Offer(amount = TEN, price = FOUR)),
                timestamp = 100L
            ).asSorted(),
            `is` (equalTo(
                OrderBook(
                    EMPTY_TEST_EXCHANGE,
                    sellOffers = listOf(
                        Offer(amount = TEN, price = FIVE),
                        Offer(amount = TEN, price = SIX),
                        Offer(amount = FIVE, price = TEN)
                    ),
                    buyOffers = listOf(
                        Offer(amount = TEN, price = FOUR),
                        Offer(amount = TEN, price = TWO),
                        Offer(amount = TEN, price = ONE)
                    ),
                timestamp = 100L
            ))
        ))
    }
}