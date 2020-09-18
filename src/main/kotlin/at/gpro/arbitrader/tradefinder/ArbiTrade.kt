package at.gpro.arbitrader.tradefinder

import at.gpro.arbitrader.tradecontroller.BuyOffer
import at.gpro.arbitrader.tradecontroller.SellOffer
import at.gpro.arbitrader.tradecontroller.Trade

data class ArbiTrade(
    val buyTrade: BuyOffer,
    val sellTrade: SellOffer
) : Trade(listOf(buyTrade), listOf(sellTrade))
