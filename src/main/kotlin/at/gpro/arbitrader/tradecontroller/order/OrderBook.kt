package at.gpro.arbitrader.tradecontroller.order

import at.gpro.arbitrader.tradecontroller.exchange.Exchange
import java.math.BigDecimal

data class Offer (
    val amount: BigDecimal,
    val price: BigDecimal
)

data class OrderBook(
    val exchange: Exchange,
    val buyOffers: List<Offer>,
    val sellOffers: List<Offer>
) {
    fun asSorted(): OrderBook =
        OrderBook(
            exchange,
            buyOffers.sortedByDescending { it.price },
            sellOffers.sortedBy { it.price }
        )
}