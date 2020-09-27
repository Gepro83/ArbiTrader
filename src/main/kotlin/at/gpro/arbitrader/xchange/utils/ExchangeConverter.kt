package at.gpro.arbitrader.xchange.utils

import at.gpro.arbitrader.entity.Exchange

class ExchangeConverter {
    fun convert(xchange: XchangeExchange) =
        object : Exchange {
            override fun getName(): String = xchange.exchangeSpecification.exchangeName
            override fun toString(): String = getName()
        }
}