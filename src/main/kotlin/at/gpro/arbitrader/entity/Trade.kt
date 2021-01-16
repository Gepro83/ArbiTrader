package at.gpro.arbitrader.entity

import java.math.BigDecimal

data class ArbiTrade(
    val amount: BigDecimal,
    val buyPrice: ExchangePrice,
    val sellPrice: ExchangePrice
) {
    constructor(amount: Int, buyPrice: ExchangePrice, sellPrice: ExchangePrice)
            : this(BigDecimal(amount), buyPrice, sellPrice)
}

data class ExchangePrice(
    val price: BigDecimal,
    val exchange: Exchange
) {
    constructor(price: Int, exchange: Exchange) : this(BigDecimal(price), exchange)
}

data class CurrencyTrade(
    val trade: ArbiTrade,
    val pair: CurrencyPair
)