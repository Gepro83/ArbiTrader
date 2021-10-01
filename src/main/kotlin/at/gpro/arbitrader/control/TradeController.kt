package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.CurrencyPair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class TradeController(
    private val updateProvider: UpdateProvider,
    private val tradeFinder: TradeFinder,
    private val tradeEvaluator: TradeEvaluator,
    private val tradePlacer: TradePlacer,
    private val currencyPairs: List<CurrencyPair>
) {

    private var isLogTime = false
    private var lastLog = System.currentTimeMillis()

    private val scope = CoroutineScope(newSingleThreadContext("ControllerThread"))

    fun run() {
        LOG.debug { "trade controller started" }
        scope.launch {
            while(true) {
//                isLogTime = (System.currentTimeMillis() - lastLog) > 5000

//                if(isLogTime) {
//                    LOG.debug { "start checking"}
//                    lastLog = System.currentTimeMillis()
//                }
                checkAllPairs()
//                if(isLogTime)
//                    LOG.debug { "done checking"}

                delay(50)
            }
        }
    }

    private fun checkAllPairs() {
        currencyPairs.forEach { checkPair(it) }
    }

    private fun checkPair(pair: CurrencyPair) {
        val orderBooks = updateProvider.getOrderBooks(pair)

//        if(isLogTime) {
//            LOG.debug { "${orderBooks.size} orderBooks" }
//            val now = System.currentTimeMillis()
//            val map = orderBooks.map { now - it.timestamp }
//            LOG.debug {
//                map
//            }
//        }

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