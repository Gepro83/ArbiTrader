package at.gpro.arbitrader

import at.gpro.arbitrader.entity.*
import java.math.BigDecimal

object TestUtils {

    fun newTestExchangeTrade(buyPrice: Int, sellPrice: Int, amount: Int, pair: CurrencyPair): CurrencyTrade =
        CurrencyTrade(
            ArbiTrade(
                amount,
                ExchangePrice(buyPrice, EMPTY_TEST_EXCHANGE),
                ExchangePrice(sellPrice, EMPTY_TEST_EXCHANGE),
            ),
            pair
        )

    fun newTestExchange(name: String, fee: BigDecimal = BigDecimal.ZERO): Exchange =
        object : Exchange {
            override fun getName(): String = name
            override fun getFee(): BigDecimal = fee
        }
}