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
    private var isStopped: () -> Boolean = { true }

    fun run() {
        LOG.debug { "trade controller started" }
        this.isStopped = isStopped
        updateProvider.onUpdate { onOrderBookUpdate() }
    }

    private fun onOrderBookUpdate() {
        currencyPairs.forEach { checkPair(it) }
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


//        tradePlacer.placeTrades(pair, trades.filter { tradeEvaluator.isWorthy(it) })
    }
}