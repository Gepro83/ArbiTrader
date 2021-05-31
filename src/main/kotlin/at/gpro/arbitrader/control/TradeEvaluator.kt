package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.ExchangeArbiTrade

interface TradeEvaluator {
    fun isWorthy(trade: ExchangeArbiTrade): Boolean
    fun score(trade: ExchangeArbiTrade): Double
}
