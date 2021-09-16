package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradePlacer
import at.gpro.arbitrader.entity.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode

class MarketPlacer(safePriceMargin : Double = 0.0) : TradePlacer {

    private val safeFactor : Double = 1.0 + safePriceMargin

    private var lastLog = 0L

    companion object {
        private val LOG = KotlinLogging.logger {}
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun averageBuyPrice(trades: List<ScoredArbiTrade>): BigDecimal =
        trades.sumOf { it.buyPrice }
            .divide(trades.size.toBigDecimal(), RoundingMode.HALF_DOWN)

    private fun averageSellPrice(trades: List<ScoredArbiTrade>): BigDecimal =
        trades.sumOf { it.sellPrice }
            .divide(trades.size.toBigDecimal(), RoundingMode.HALF_DOWN)

    private fun averageScore(trades: List<ScoredArbiTrade>): BigDecimal =
        trades.sumOf { it.score }
            .divide(trades.size.toBigDecimal(), RoundingMode.HALF_DOWN)

    private var isLogTime = true

    override fun placeTrades(
        pair: CurrencyPair,
        buyExchange: Exchange,
        sellExchange: Exchange,
        trades: List<ScoredArbiTrade>
    ) {
        val coroutines: MutableList<Deferred<Unit>> = ArrayList(trades.size * 2)

        isLogTime = (System.currentTimeMillis() - lastLog) > 5000

        if(isLogTime) {
            LOG.debug { "Placable trades found:  amount (${trades.sumOf { it.amount }}) avgScore: ${averageScore(trades).setScale(5, RoundingMode.DOWN)} avgBuy: ${averageBuyPrice(trades)} avgSell: ${averageSellPrice(trades)} buy at ${buyExchange.getName()} sell at ${sellExchange.getName()}" }
            lastLog = System.currentTimeMillis()
        }

        val amount = calculateSafeAmount(
            buyExchange,
            sellExchange,
            pair,
            trades
        )

        if (amount > pair.minTradeAmount) {
            LOG.debug {
                "Found tradeable amount ($amount) avgScore: ${averageScore(trades)} avgBuy: ${averageBuyPrice(trades)} avgSell: ${averageSellPrice(trades)}"
            }
            coroutines.add(placeAsync(Order(OrderType.ASK, amount, pair), sellExchange))
            coroutines.add(placeAsync(Order(OrderType.BID, amount, pair), buyExchange))
        } else {
            LOG.debug { "Amount $amount too low"}
        }

        runBlocking {
            coroutines.awaitAll()
        }
    }

    private fun calculateSafeAmount(
        buyExchange: Exchange,
        sellExchange: Exchange,
        pair: CurrencyPair,
        trades: List<ScoredArbiTrade>
    ): BigDecimal {

        val totalTradeAmount = trades.sumOf { it.amount.setScale(pair.mainCurrency.scale) }

        val averagePrice = trades.sumOf { it.buyPrice.setScale(pair.payCurrency.scale).times(it.amount) }
            .divide(totalTradeAmount, RoundingMode.HALF_UP)

        val sellBalance = sellExchange.getBalance(pair.mainCurrency).setScale(pair.mainCurrency.scale)

        val maxSellAmount = if(totalTradeAmount < sellBalance)
            totalTradeAmount
        else
            sellBalance

        val safePrice = averagePrice.times(safeFactor.toBigDecimal())

        val maxPrice = maxSellAmount.times(safePrice)

        if (isLogTime)
            LOG.debug { """calculate safe amount
                total trade amount $totalTradeAmount
                average price $averagePrice
                maxsellamount $maxSellAmount
                safePrice $safePrice
                maxprice $maxPrice 
            """.trimIndent() }

        val buyBalance = buyExchange.getBalance(pair.payCurrency)

        if (maxPrice < buyBalance)
            return maxSellAmount

        val safeAmount = buyBalance.setScale(pair.payCurrency.scale)
            .divide(safePrice, RoundingMode.HALF_DOWN)
            .setScale(pair.mainCurrency.scale, RoundingMode.HALF_DOWN)

        return safeAmount

    }

    private fun placeAsync(order: Order, exchange: Exchange): Deferred<Unit> =
        scope.async {
            exchange.place(order)
        }

}