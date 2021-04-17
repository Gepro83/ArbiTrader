package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.CurrencyPair

interface TradeExecutor {
    fun executeTrades(pair: CurrencyPair, trades: List<ArbiTrade>)
}