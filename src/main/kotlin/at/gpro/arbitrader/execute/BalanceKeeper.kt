package at.gpro.arbitrader.execute

import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.Currency
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import java.math.BigDecimal
import java.math.RoundingMode

class BalanceKeeper(
    private val safePriceMargin: Double,
    private val balanceMargin: Double
    ) {
    private val balanceReduceMap : MutableMap<Exchange, MutableMap<Currency, BigDecimal>> = HashMap()

    fun getSafeAmount(
        buyExchange: Exchange,
        sellExchange: Exchange,
        pair: CurrencyPair,
        trade: ArbiTrade
    ): BigDecimal {

        val maxSellAmount = getMaxSellAmount(sellExchange, pair, trade.amount)

        if (buyExchange.getReducedBalance(pair.payCurrency) > getIncreasedTradePrice(maxSellAmount, trade.buyPrice))
            return maxSellAmount

        return buyExchange.getReducedBalance(pair.payCurrency)
            .setScale(pair.mainCurrency.scale)
            .divide(
                trade.buyPrice.increaseBy(safePriceMargin),
                RoundingMode.HALF_DOWN
            )
    }

    private fun getIncreasedTradePrice(
        amount: BigDecimal,
        price: BigDecimal
    ): BigDecimal = amount.times(price.increaseBy(balanceMargin))

    private fun BigDecimal.increaseBy(percent: Double) = this.times(BigDecimal(1.0 + percent))

    private fun getMaxSellAmount(
        sellExchange: Exchange,
        pair: CurrencyPair,
        tradeAmount: BigDecimal
    ): BigDecimal {
        val balance = sellExchange.getReducedBalance(pair.mainCurrency)
        
        return if (balance < tradeAmount)
            balance
        else
            tradeAmount
    }

    private fun Exchange.getReducedBalance(currency: Currency): BigDecimal =
        getBalance(currency).minus(balanceReduceMap[this]?.get(currency) ?: BigDecimal.ZERO)

    fun reduceBalance(exchange: Exchange, amount: BigDecimal, currency: Currency) {
        val currencyMap = balanceReduceMap.getOrPut(exchange) {
            HashMap()
        }

        currencyMap.merge(currency, amount, BigDecimal::plus)
    }

}
