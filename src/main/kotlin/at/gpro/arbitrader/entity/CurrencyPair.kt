package at.gpro.arbitrader.entity

enum class CurrencyPair(
    val buyCurrency: Currency,
    val sellCurrency: Currency
) {
    BTC_EUR(Currency.BTC, Currency.EUR),
    ETH_EUR(Currency.ETH, Currency.EUR),
    BCH_EUR(Currency.BCH, Currency.EUR),
    XRP_EUR(Currency.XRP, Currency.EUR),
    ETH_BTC(Currency.ETH, Currency.BTC),
    XRP_BTC(Currency.XRP, Currency.BTC),
    XRP_ETH(Currency.XRP, Currency.ETH)
}

enum class Currency {
    BTC, EUR, ETH, BCH, XRP;
}
