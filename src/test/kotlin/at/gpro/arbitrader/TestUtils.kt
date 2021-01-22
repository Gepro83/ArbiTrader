package at.gpro.arbitrader

import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.CurrencyTrade
import at.gpro.arbitrader.entity.ExchangePrice

object TestUtils {

    fun testExchangeTrade(buyPrice: Int, sellPrice: Int, amount: Int, pair: CurrencyPair): CurrencyTrade =
        CurrencyTrade(
            ArbiTrade(
                amount,
                ExchangePrice(buyPrice, EMPTY_TEST_EXCHANGE),
                ExchangePrice(sellPrice, EMPTY_TEST_EXCHANGE),
            ),
            pair
        )


}