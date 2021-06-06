package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import java.util.*

interface TradePlacer {
    fun placeTrades(pair: CurrencyPair, buyExchange: Exchange, sellExchange: Exchange, trades: SortedSet<ArbiTrade>)
}