package at.gpro.arbitrader.entity

import java.math.BigDecimal

interface ScoredArbiTrade : ArbiTrade {
    val score: BigDecimal
}