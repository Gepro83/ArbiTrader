package at.gpro.arbitrader.order

import at.gpro.arbitrader.exchange.Exchange
import java.math.BigDecimal

data class Offer(
    val amount: BigDecimal,
    val price: BigDecimal
)

class OrderBook(
    private val exchange: Exchange,
    private val buyOffers: List<Offer>,
    private val sellOffers: List<Offer>) {
}