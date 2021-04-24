package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.CurrencyPair

interface TradePlacer {
    fun placeTrades(pair: CurrencyPair, trades: List<ArbiTrade>)
}