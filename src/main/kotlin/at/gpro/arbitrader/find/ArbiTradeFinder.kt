package at.gpro.arbitrader.find

import at.gpro.arbitrader.control.TradeFinder
import at.gpro.arbitrader.entity.BuyOffer
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.SellOffer
import at.gpro.arbitrader.entity.Trade
import at.gpro.arbitrader.entity.order.Offer
import at.gpro.arbitrader.entity.order.OrderBook
import mu.KotlinLogging
import java.math.BigDecimal

private val LOG = KotlinLogging.logger {}

class ArbiTradeFinderFacade() : TradeFinder {
    override fun findTrades(orderBooks: List<OrderBook>): List<Trade> {
        if (orderBooks.size != 2)
            throw IllegalArgumentException("ArbitTradeFinder needs exactly 2 orderbooks")

        return ArbiTradeFinder(orderBooks[0], orderBooks[0]).findTrades()
    }
}

internal class ArbiTradeFinder(orderBook: OrderBook, compareOrderBook: OrderBook) {
    private val buyExchange: Exchange
    private val sellExchange: Exchange
    private val buyOffers : List<Offer>
    private val sellOffers : List<Offer>
    private val arbiTrades = ArrayList<ArbiTrade>()

    private var currentBuyPrice = BigDecimal.ZERO
    private var currentSellPrice = BigDecimal.ZERO
    private var buyOffersIndex : Int
    private var sellOffersIndex : Int
    private var sellOfferFilledAmount = BigDecimal.ZERO
    private var buyOfferFilledAmount = BigDecimal.ZERO

    init {
        LOG.debug { "New Arbitarder created for orderbooks $orderBook and $compareOrderBook" }
        val sortedOrderBook = orderBook.asSorted()
        val sortedCompareOderBook = compareOrderBook.asSorted()
        if (areAnyOffersEmpty(sortedOrderBook, sortedCompareOderBook)) {
            LOG.warn { "some offerlist is empty" }
            buyExchange = orderBook.exchange
            sellExchange = compareOrderBook.exchange
            buyOffers = emptyList()
            sellOffers = emptyList()
            buyOffersIndex = -1
            sellOffersIndex = -1

        } else {
            val (buyOrderBook, sellOrderBook) = determinaBuyAndSellOrderBooks(sortedOrderBook, sortedCompareOderBook)
            buyExchange = buyOrderBook.exchange
            sellExchange = sellOrderBook.exchange
            buyOffers = buyOrderBook.buyOffers
            sellOffers = sellOrderBook.sellOffers

            buyOffersIndex = 0
            sellOffersIndex = 0

            setCurrentPrices()
        }
    }

    private fun areAnyOffersEmpty(
        orderBook: OrderBook,
        compareOrderBook: OrderBook
    ) =
        orderBook.buyOffers.isEmpty() || orderBook.sellOffers.isEmpty() ||
        compareOrderBook.buyOffers.isEmpty() || compareOrderBook.sellOffers.isEmpty()

    private fun determinaBuyAndSellOrderBooks(
        orderBook: OrderBook,
        compareOrderBook: OrderBook
    ): Pair<OrderBook, OrderBook> =
        if (orderBook.buyOffers.first().price > compareOrderBook.sellOffers.first().price)
            orderBook to compareOrderBook
        else
            compareOrderBook to orderBook

    fun findTrades(): List<ArbiTrade> {
        while (currentBuyPrice > currentSellPrice) {
            val remainingBuyOfferAmount = getRemainingBuyOfferAmount()
            val remainingSellOfferAmount = getRemainingSellOfferAmount()

            val amount = when {
                remainingBuyOfferAmount > remainingSellOfferAmount -> {
                    sellOfferFilledAmount = BigDecimal.ZERO
                    buyOfferFilledAmount = buyOfferFilledAmount.plus(remainingSellOfferAmount)
                    sellOffersIndex++
                    remainingSellOfferAmount
                }
                remainingBuyOfferAmount < remainingSellOfferAmount -> {
                    buyOfferFilledAmount = BigDecimal.ZERO
                    sellOfferFilledAmount = sellOfferFilledAmount.plus(remainingBuyOfferAmount)
                    buyOffersIndex++
                    remainingBuyOfferAmount
                }
                else -> { // equal amounts
                    sellOfferFilledAmount = BigDecimal.ZERO
                    buyOfferFilledAmount = BigDecimal.ZERO
                    sellOffersIndex++
                    buyOffersIndex++
                    remainingBuyOfferAmount
                }
            }

            arbiTrades.add(
                ArbiTrade(
                    BuyOffer(
                        Offer(amount, currentSellPrice),
                        sellExchange
                    ),
                    SellOffer(
                        Offer(amount, currentBuyPrice),
                        buyExchange
                    )
                )
            )

            setCurrentPrices()
        }
        LOG.info { "found trades: $arbiTrades" }
        return arbiTrades
    }

    private fun getRemainingSellOfferAmount() = sellOffers[sellOffersIndex].amount.minus(sellOfferFilledAmount)

    private fun getRemainingBuyOfferAmount() = buyOffers[buyOffersIndex].amount.minus(buyOfferFilledAmount)


    private fun setCurrentPrices() {
        currentBuyPrice = buyOffers[buyOffersIndex].price
        currentSellPrice = sellOffers[sellOffersIndex].price
    }

}