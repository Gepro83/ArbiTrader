package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.ScoredArbiTrade

interface TradePlacer {
    fun placeTrades(pair: CurrencyPair, buyExchange: Exchange, sellExchange: Exchange, trades: List<ScoredArbiTrade>)
}