package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradeExecutor
import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.CurrencyTrade
import at.gpro.arbitrader.evaluate.SpreadCalculator
import mu.KotlinLogging
import java.io.File

private val LOG = KotlinLogging.logger {}

class CsvLogger(
    private val file: File,
    private val delayBetweenExecuteMS: Long
): TradeExecutor {

    init {
        if(!file.exists()) {
            LOG.debug { "HIHIHI" }
            file.createNewFile()
            file.writeText(CSV_HEADER + System.lineSeparator())
        }
    }

    companion object {
        val CSV_HEADER = "TIMESTAMP;SPREAD;AMOUNT;BUY_PRICE;BUY_EXCHANGE;SELL_PRICE;SELL_EXCHANGE;PAIR"
    }

    override fun executeTrades(trades: List<CurrencyTrade>) {
        LOG.info { "executing $trades" }

        file.appendText(trades.joinToString(System.lineSeparator()) { getLineFor(it) })
    }

    private fun getLineFor(trade: CurrencyTrade): String =
        listOf(
            System.currentTimeMillis(),
            SpreadCalculator.calculateSpread(trade.trade),
            trade.trade.amount,
            trade.trade.buyPrice.price,
            trade.trade.buyPrice.exchange.getName(),
            trade.trade.sellPrice.price,
            trade.trade.sellPrice.exchange.getName(),
            trade.pair,
        ).joinToString(";")
}