package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.CurrencyPair
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class TradeController(
    private val updateProvider: UpdateProvider,
    private val tradeFinder: TradeFinder,
    private val tradeEvaluator: TradeEvaluator,
    private val tradePlacer: TradePlacer,
    private val currencyPairs: List<CurrencyPair>
) {
    @Volatile
    private var checking = false

    fun run() {
        LOG.debug { "trade controller started" }
        updateProvider.onUpdate { onOrderBookUpdate() }
    }

    private fun onOrderBookUpdate() {
        if (!checking) {
            checking = true
            currencyPairs.forEach { checkPair(it) }
            checking = false
        }
    }

    private fun checkPair(pair: CurrencyPair) {
        val orderBooks = updateProvider.getOrderBooks(pair)

        if(orderBooks.size < 3)
            return

        val trades = ArrayList(tradeFinder.findTrades(orderBooks[0], orderBooks[1]))
        trades.addAll(tradeFinder.findTrades(orderBooks[1], orderBooks[2]))
        trades.addAll(tradeFinder.findTrades(orderBooks[0], orderBooks[2]))

        if (orderBooks.size == 4) {
            trades.addAll(tradeFinder.findTrades(orderBooks[3], orderBooks[0]))
            trades.addAll(tradeFinder.findTrades(orderBooks[3], orderBooks[1]))
            trades.addAll(tradeFinder.findTrades(orderBooks[3], orderBooks[2]))
        }

        trades
            .filter { tradeEvaluator.isWorthy(it) }
            .groupBy { it.buyExchangePrice.exchange to it.sellExchangePrice.exchange }
            .forEach { (exchangePair, trades) ->

                tradePlacer.placeTrades(
                    pair,
                    exchangePair.first,
                    exchangePair.second,
                    trades.map { tradeEvaluator.score(it) }
                )
            }
    }
}