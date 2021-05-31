package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.execute.ArbiTrade
import java.util.*

interface TradePlacer {
    fun placeTrades(pair: CurrencyPair, buyExchange: Exchange, sellExchange: Exchange, trades: SortedSet<ArbiTrade>)
}