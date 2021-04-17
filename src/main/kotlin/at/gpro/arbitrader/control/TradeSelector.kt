package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.CurrencyPair

interface TradeSelector {
    fun selectTrades(pair: CurrencyPair, trades: List<ArbiTrade>) : List<ArbiTrade>
}