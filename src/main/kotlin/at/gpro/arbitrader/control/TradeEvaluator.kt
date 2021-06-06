package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.ExchangeArbiTrade
import at.gpro.arbitrader.entity.ScoredArbiTrade

interface TradeEvaluator {
    fun isWorthy(trade: ExchangeArbiTrade): Boolean
    fun score(trade: ExchangeArbiTrade): ScoredArbiTrade
}
