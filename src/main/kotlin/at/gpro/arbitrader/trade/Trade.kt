package at.gpro.arbitrader.trade

import at.gpro.arbitrader.exchange.Exchange
import at.gpro.arbitrader.order.Offer
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class Trade(
    val offer: Offer,
    val exchange: Exchange
) {
    fun place() {
        LOG.debug { "placing $offer at $exchange" }
    }

    override fun toString(): String {
        return "Trade(offer=$offer, exchange=$exchange)"
    }

}