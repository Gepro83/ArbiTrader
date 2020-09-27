package at.gpro.arbitrader.utils.xchange

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.update.XchangePair

class CurrencyPairConverter {
    fun convert(pair: XchangePair) = when(pair) {
        XchangePair.BTC_EUR -> CurrencyPair.BTC_EUR
        XchangePair.BCH_EUR -> CurrencyPair.BCH_EUR
        XchangePair.ETH_EUR-> CurrencyPair.ETH_EUR
        XchangePair.XRP_EUR-> CurrencyPair.XRP_EUR
        else -> throw IllegalArgumentException("No internal pair existing for : $pair")
    }

    fun convert(pairs: List<XchangePair>) = pairs.map { convert(it) }
}