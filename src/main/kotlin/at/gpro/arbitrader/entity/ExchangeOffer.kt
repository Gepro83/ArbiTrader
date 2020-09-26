package at.gpro.arbitrader.entity

import at.gpro.arbitrader.entity.order.Offer
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

sealed class ExchangeOffer(
    open val offer: Offer,
    open val exchange: Exchange
) {
    override fun toString(): String {
        return "ExchangeOffer(offer=$offer, exchange=$exchange)"
    }
}

data class BuyOffer(override val offer: Offer, override val exchange: Exchange) : ExchangeOffer(offer, exchange)
data class SellOffer(override val offer: Offer, override val exchange: Exchange) : ExchangeOffer(offer, exchange)

