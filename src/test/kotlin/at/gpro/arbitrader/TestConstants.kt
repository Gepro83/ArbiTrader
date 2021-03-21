package at.gpro.arbitrader

import at.gpro.arbitrader.entity.Currency
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.Order
import at.gpro.arbitrader.security.model.ApiKeyStore
import java.io.File
import java.math.BigDecimal

val TWO = BigDecimal(2)
val THREE = BigDecimal(3)
val FOUR = BigDecimal(4)
val FIVE = BigDecimal(5)
val SIX = BigDecimal(6)
val SEVEN = BigDecimal(7)
val NINE = BigDecimal(9)
val ELEVEN = BigDecimal(11)
val TWENTY = BigDecimal(20)

val EMPTY_TEST_EXCHANGE = object : Exchange {
    override fun getName(): String = "TestExchange"
    override fun getFee(): Double = 0.0
    override fun place(order: Order) {
        TODO("Not yet implemented")
    }

    override fun getBalance(pair: Currency): BigDecimal {
        TODO("Not yet implemented")
    }

    override fun toString(): String = "TestExchange"
}

val ANOTHER_EMPTY_TEST_EXCHANGE = object : Exchange {
    override fun getName(): String = "AnotherTestExchange"
    override fun getFee(): Double = 0.0
    override fun place(order: Order) {
        TODO("Not yet implemented")
    }

    override fun getBalance(pair: Currency): BigDecimal {
        TODO("Not yet implemented")
    }

    override fun toString(): String = "AnortherTestExchange"
}

val API_KEY_STORE = ApiKeyStore.from(File("/Users/gprohaska/Documents/crypto/ApiKeys.json"))

val COINBASEPRO_KEY = API_KEY_STORE?.getKey("CoinbasePro") ?: throw Exception("Could not find CoinbasePro key")
val KRAKEN_KEY = API_KEY_STORE?.getKey("Kraken") ?: throw Exception("Could not find Kraken key")

