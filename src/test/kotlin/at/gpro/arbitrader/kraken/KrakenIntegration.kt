package at.gpro.arbitrader.kraken

import at.gpro.arbitrader.security.model.ApiKeyStore
import org.junit.jupiter.api.Test
import org.knowm.xchange.currency.CurrencyPair
import java.io.File

internal class KrakenIntegration {
    val API_KEY_STORE = ApiKeyStore.from(File("/Users/gprohaska/Documents/crypto/ApiKeys.json"))
    val KRAKEN_KEY = API_KEY_STORE?.getKey("Kraken") ?: throw Exception("Could not find Kraken key")

    @Test
    internal fun `getOrderBooks`() {
        println(Kraken(KRAKEN_KEY).getOrderBook(CurrencyPair.BTC_EUR))
    }
}