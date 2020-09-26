package at.gpro.arbitrader.entity

abstract class Trade(
    val buyOffers : List<BuyOffer>,
    val sellOffers : List<SellOffer>
)