package at.gpro.arbitrader.tradecontroller

abstract class Trade(
    val buyOffers : List<BuyOffer>,
    val sellOffers : List<SellOffer>
)