package at.gpro.arbitrader.control

import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class TradeController(
    private val tradeFinder: TradeFinder,
    private val updateProvider: UpdateProvider,
    private val tradeExecutor: TradeExecutor,
    private val tradeSelector: TradeSelector
) {

    fun runUntil(isStopped: () -> Boolean) {
        LOG.debug { "trade controller started" }
        while(!isStopped())
            runMainLoop()
    }

    private fun runMainLoop() {
        TODO("needs testing for different currencypairs")
//        val orderBooks = updateProvider.getOrderBooks()

//        val trades = tradeFinder.findTrades(orderBooks)
//
//        val selectedTrades = tradeSelector.selectTrades(trades)
//
//        if (selectedTrades.isNotEmpty()) {
//            LOG.debug { "${selectedTrades.size} trades found. executing..." }
//            tradeExecutor.executeTrades(selectedTrades)
//        }
    }
}