package at.gpro.arbitrader.order

import java.math.BigDecimal

data class Offer(
    val type: OfferType,
    val amount: BigDecimal,
    val price: BigDecimal
) {
    enum class OfferType { BUY, SELL }
}

class OrderBook{

}