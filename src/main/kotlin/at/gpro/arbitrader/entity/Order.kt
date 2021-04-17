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

data class Order(
    val type: OrderType,
    val amount: BigDecimal,
    val pair: CurrencyPair
) {
    companion object {
        fun bid(amount: BigDecimal, pair: CurrencyPair) = Order(OrderType.BID, amount, pair)
        fun ask(amount: BigDecimal, pair: CurrencyPair) = Order(OrderType.ASK, amount, pair)
    }
}

enum class OrderType { ASK, BID }