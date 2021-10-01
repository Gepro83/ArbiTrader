package at.gpro.arbitrader.kraken

import kotlinx.serialization.Serializable

@Serializable
internal data class KrakenOrderbookDTO(
    val error: List<String>,
    val result: Result
)

@Serializable
internal data class Result(
    val XXBTZEUR: XXBTZEUR? = null,
    val XETHZEUR: XETHZEUR? = null,
    val XETHXXBT: XETHXXBT? = null,
)

@Serializable
internal data class XXBTZEUR(
    val asks : List<List<Double>>,
    val bids : List<List<Double>>
)

@Serializable
internal data class XETHZEUR(
    val asks : List<List<Double>>,
    val bids : List<List<Double>>
)

@Serializable
internal data class XETHXXBT(
    val asks : List<List<Double>>,
    val bids : List<List<Double>>
)