package at.gpro.arbitrader.xchange.utils

import at.gpro.arbitrader.entity.CurrencyPair

class CurrencyPairConverter {
    fun convert(pair: XchangePair) = when(pair) {
        XchangePair.BTC_EUR -> CurrencyPair.BTC_EUR
        XchangePair.BCH_EUR -> CurrencyPair.BCH_EUR
        XchangePair.ETH_EUR -> CurrencyPair.ETH_EUR
        XchangePair.XRP_EUR -> CurrencyPair.XRP_EUR
        XchangePair.ETH_BTC -> CurrencyPair.ETH_BTC
        XchangePair.XRP_BTC -> CurrencyPair.XRP_BTC
        XchangePair.XRP_ETH -> CurrencyPair.XRP_ETH
        else -> throw IllegalArgumentException("No internal pair existing for : $pair")
    }

    fun convert(pair: CurrencyPair) = when(pair) {
        CurrencyPair.BTC_EUR -> XchangePair.BTC_EUR
        CurrencyPair.BCH_EUR -> XchangePair.BCH_EUR
        CurrencyPair.ETH_EUR -> XchangePair.ETH_EUR
        CurrencyPair.XRP_EUR -> XchangePair.XRP_EUR
        CurrencyPair.ETH_BTC -> XchangePair.ETH_BTC
        CurrencyPair.XRP_BTC -> XchangePair.XRP_BTC
        CurrencyPair.XRP_ETH -> XchangePair.XRP_ETH
    }

    fun convertToCurrencyPair(pairs: List<XchangePair>) = pairs.map { convert(it) }
    fun convertToXchangePair(pairs: List<CurrencyPair>) = pairs.map { convert(it) }
}