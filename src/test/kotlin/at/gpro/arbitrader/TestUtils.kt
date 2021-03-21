package at.gpro.arbitrader

import at.gpro.arbitrader.entity.*
import java.math.BigDecimal

object TestUtils {

    fun newTestExchangeTrade(buyPrice: Int, sellPrice: Int, amount: Int, pair: CurrencyPair): CurrencyTrade =
        CurrencyTrade(
            pair,
            ArbiTrade(
                amount,
                ExchangePrice(buyPrice, EMPTY_TEST_EXCHANGE),
                ExchangePrice(sellPrice, EMPTY_TEST_EXCHANGE),
            )
        )

    fun newTestExchange(name: String, fee: Double = 0.0): Exchange =
        object : Exchange {
            override fun getName(): String = name
            override fun getFee(): Double = fee
            override fun place(order: Order) {
                TODO("Not yet implemented")
            }

            override fun getBalance(pair: Currency): BigDecimal {
                TODO("Not yet implemented")
            }
        }
}