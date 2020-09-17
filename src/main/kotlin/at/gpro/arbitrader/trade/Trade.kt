package at.gpro.arbitrader.trade

import at.gpro.arbitrader.exchange.Exchange
import at.gpro.arbitrader.order.Offer
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

sealed class Trade(
    open val offer: Offer,
    open val exchange: Exchange
) {
    fun place() {
        LOG.debug { "placing $offer at $exchange" }
    }

    override fun toString(): String {
        return "Trade(offer=$offer, exchange=$exchange)"
    }
}

data class BuyTrade(override val offer: Offer, override val exchange: Exchange) : Trade(offer, exchange)
data class SellTrade(override val offer: Offer, override val exchange: Exchange) : Trade(offer, exchange)

data class ArbiTrade(
    val buyTrade: BuyTrade,
    val sellTrade: SellTrade
)