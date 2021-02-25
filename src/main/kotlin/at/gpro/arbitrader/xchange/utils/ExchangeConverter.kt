package at.gpro.arbitrader.xchange.utils

import at.gpro.arbitrader.entity.Exchange

class ExchangeConverter {
    fun convert(xchange: XchangeExchange, fee: Double) =
        object : Exchange {
            override fun getName(): String = xchange.exchangeSpecification.exchangeName
            override fun getFee(): Double = fee
            override fun toString(): String = getName()
        }
}