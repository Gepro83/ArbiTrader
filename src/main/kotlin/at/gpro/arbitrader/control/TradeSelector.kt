package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.ArbiTrade

fun interface TradeSelector {
    fun isWorthy(trade: ArbiTrade): Boolean
}

object EverythingSelector : TradeSelector {
    override fun isWorthy(trade: ArbiTrade): Boolean = true
}