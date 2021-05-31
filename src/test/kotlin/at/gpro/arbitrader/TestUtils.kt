package at.gpro.arbitrader

import at.gpro.arbitrader.entity.*
import java.math.BigDecimal

object TestUtils {
    fun newTestExchangeTrade(buyPrice: Int, sellPrice: Int, amount: Int): ExchangeArbiTrade =
            ExchangeArbiTrade(
                amount,
                ExchangePrice(buyPrice, EMPTY_TEST_EXCHANGE),
                ExchangePrice(sellPrice, EMPTY_TEST_EXCHANGE),
            )

    fun newTestExchange(name: String, fee: Double = 0.0, balance: Int = Int.MAX_VALUE): Exchange =
        object : Exchange {
            override fun getName(): String = name
            override fun getFee(): Double = fee
            override fun place(order: Order) {
                TODO("Not yet implemented")
            }
            override fun getBalance(currency: Currency): BigDecimal = BigDecimal(balance)
        }
}