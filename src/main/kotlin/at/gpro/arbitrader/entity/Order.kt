package at.gpro.arbitrader.entity

import at.gpro.arbitrader.execute.ArbiTrade
import java.math.BigDecimal

data class ExchangeArbiTrade(
    override val amount: BigDecimal,
    val buyExchangePrice: ExchangePrice,
    val sellExchangePrice: ExchangePrice
): ArbiTrade {
    constructor(amount: Int, buyPrice: ExchangePrice, sellPrice: ExchangePrice)
            : this(BigDecimal(amount), buyPrice, sellPrice)

    override val buyPrice: BigDecimal
        get() = buyExchangePrice.price
    override val sellPrice: BigDecimal
        get() = sellExchangePrice.price
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