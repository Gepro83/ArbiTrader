package at.gpro.arbitrader.security.model

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.File

internal class ApiKeyStoreTest {

    @Test
    internal fun from() {
        val keyStore = ApiKeyStore.from(File("src/test/resources/apiKeyStoreTest.json"))
        val coinbaseApiKey = keyStore?.getKey("CoinbasePro")?.apiKey
        val coinbaseParameterValue = keyStore?.getKey("CoinbasePro")?.specificParameter?.value
        val krakenSecret = keyStore?.getKey("Kraken")?.secret
        val krakenParameter = keyStore?.getKey("Kraken")?.specificParameter

        assertThat(coinbaseApiKey, `is`(equalTo("coinbaseTestKey")))
        assertThat(coinbaseParameterValue, `is`(equalTo("aphrase")))
        assertThat(krakenSecret, `is`(equalTo("krakenSecret")))
        assertNull(krakenParameter)
    }

    @Test
    internal fun `exception for invalid json`() {
        assertThrows(Exception::class.java) {
            ApiKeyStore.from(File("src/test/resources/invalidApiKeyStoreTest.json"))
        }
    }
}