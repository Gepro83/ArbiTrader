package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradePlacer
import at.gpro.arbitrader.entity.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode

class MarketPlacer(
    private val safePriceMargin : Double = 0.0,
    private val payBalanceMargin : Double = 0.0
) : TradePlacer {

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

    override fun placeTrades(
        pair: CurrencyPair,
        buyExchange: Exchange,
        sellExchange: Exchange,
        trades: List<ScoredArbiTrade>
    ) {
        val coroutines: MutableList<Deferred<Unit>> = ArrayList(trades.size * 2)

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
        with(BalanceKeeper(safePriceMargin, payBalanceMargin)) {
            var totalAmount = BigDecimal.ZERO

            for (trade in trades.sortedByDescending { it.score }) {
                val safeAmount = getSafeAmount(
                    buyExchange,
                    sellExchange,
                    pair,
                    trade
                )

                totalAmount += safeAmount
                reduceBalance(buyExchange, safeAmount.times(trade.buyPrice), pair.payCurrency)
                reduceBalance(sellExchange, safeAmount, pair.mainCurrency)

                if (safeAmount != trade.amount)
                    break
            }

            return totalAmount
        }
    }

    private fun placeAsync(order: Order, exchange: Exchange): Deferred<Unit> =
        scope.async {
            exchange.place(order)
        }

}