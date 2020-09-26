package at.gpro.arbitrader.find

import at.gpro.arbitrader.entity.BuyOffer
import at.gpro.arbitrader.entity.SellOffer
import at.gpro.arbitrader.entity.Trade

data class ArbiTrade(
    val buyTrade: BuyOffer,
    val sellTrade: SellOffer
) : Trade(listOf(buyTrade), listOf(sellTrade))
