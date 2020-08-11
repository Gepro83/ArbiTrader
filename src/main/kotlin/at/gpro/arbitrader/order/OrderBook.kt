package at.gpro.arbitrader.order

import at.gpro.arbitrader.exchange.Exchange
import java.math.BigDecimal

data class Offer(
    val type: OfferType,
    val amount: BigDecimal,
    val price: BigDecimal
) {
    enum class OfferType { BUY, SELL }

    companion object {
        fun newSell(amount: BigDecimal, price: BigDecimal) = Offer(OfferType.SELL, amount, price)
        fun newBuy(amount: BigDecimal, price: BigDecimal) = Offer(OfferType.BUY, amount, price)
    }
}

class OrderBook(
    val exchange: Exchange,
    private val offers: List<Offer>) {
}