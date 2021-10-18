package at.gpro.arbitrader.entity

import java.math.BigDecimal

enum class CurrencyPair(
    val mainCurrency: Currency,
    val payCurrency: Currency,
    val minTradeAmount: BigDecimal
) {
    BTC_EUR(Currency.BTC, Currency.EUR, BigDecimal("0.001")),
    ETH_EUR(Currency.ETH, Currency.EUR, BigDecimal("0.02")),
    BCH_EUR(Currency.BCH, Currency.EUR, BigDecimal("0.001")),
    XRP_EUR(Currency.XRP, Currency.EUR, BigDecimal("0.001")),
    ETH_BTC(Currency.ETH, Currency.BTC, BigDecimal("0.001")),
    XRP_BTC(Currency.XRP, Currency.BTC, BigDecimal("0.001")),
    XRP_ETH(Currency.XRP, Currency.ETH, BigDecimal("0.001"))
}

enum class Currency(val scale: Int) {
    BTC(5), EUR(10), ETH(3), BCH(5), XRP(5);
}
