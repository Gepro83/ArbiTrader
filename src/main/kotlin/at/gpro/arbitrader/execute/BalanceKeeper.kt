package at.gpro.arbitrader.execute

import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.Currency
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import mu.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode

class BalanceKeeper(
    private val safePriceMargin: Double,
    private val balanceMargin: Double
    ) {
    private val balanceReduceMap : MutableMap<Exchange, MutableMap<Currency, BigDecimal>> = HashMap()

    companion object {
        private val LOG = KotlinLogging.logger {}
    }

    fun getSafeAmount(
        buyExchange: Exchange,
        sellExchange: Exchange,
        pair: CurrencyPair,
        trade: ArbiTrade
    ): BigDecimal {
        val maxSellAmount = getMaxSellAmount(sellExchange, pair, trade.amount)

        if (buyExchange.getReducedBalance(pair.payCurrency) > getIncreasedTradePrice(maxSellAmount, trade.buyPrice))
            return maxSellAmount

        val safeAmount = calculateSafeAmount(buyExchange, pair, trade)

        return if (safeAmount < BigDecimal.ZERO) BigDecimal.ZERO else safeAmount
    }

    private fun calculateSafeAmount(
        buyExchange: Exchange,
        pair: CurrencyPair,
        trade: ArbiTrade
    ): BigDecimal = buyExchange.getReducedBalance(pair.payCurrency)
        .setScale(pair.mainCurrency.scale, RoundingMode.HALF_DOWN)
        .divide(trade.buyPrice.increaseBy(safePriceMargin), RoundingMode.HALF_DOWN)

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

        if (exchange.getReducedBalance(currency) < BigDecimal.ZERO) {
            LOG.warn { "Balance of ${exchange.getName()} was tried to be reduced below 0!" }
            currencyMap[currency] = exchange.getBalance(currency)
        }
    }

}
