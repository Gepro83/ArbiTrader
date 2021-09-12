package at.gpro.arbitrader.entity

enum class CurrencyPair(
    val mainCurrency: Currency,
    val payCurrency: Currency
) {
    BTC_EUR(Currency.BTC, Currency.EUR),
    ETH_EUR(Currency.ETH, Currency.EUR),
    BCH_EUR(Currency.BCH, Currency.EUR),
    XRP_EUR(Currency.XRP, Currency.EUR),
    ETH_BTC(Currency.ETH, Currency.BTC),
    XRP_BTC(Currency.XRP, Currency.BTC),
    XRP_ETH(Currency.XRP, Currency.ETH)
}

enum class Currency(val scale: Int) {
    BTC(12), EUR(10), ETH(12), BCH(12), XRP(12);
}
