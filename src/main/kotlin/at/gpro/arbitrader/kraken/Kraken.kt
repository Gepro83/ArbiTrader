package at.gpro.arbitrader.kraken

import at.gpro.arbitrader.security.model.ApiKey
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.Order
import org.knowm.xchange.dto.trade.MarketOrder
import si.mazi.rescu.SynchronizedValueFactory
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class Kraken(private val apiKey: ApiKey) {

    var nonceFactory : SynchronizedValueFactory<Long> = SynchronizedValueFactory<Long> { 1 }

    private val LOG = KotlinLogging.logger {}

    val client = HttpClient(CIO)

    companion object {
        const val BASE_URL = "https://api.kraken.com"
    }

    fun place(order: MarketOrder): String {
        val endpoint = "/0/private/AddOrder"

        val orderId = Random().nextInt().toString()
        val response = authRequest(endpoint,
            mapOf("ordertype" to "market",
                "type" to order.type.let { when(it) {
                    Order.OrderType.ASK -> "sell"
                    Order.OrderType.BID -> "buy"
                    else -> throw UnsupportedOperationException(order.type.name)
                } },
                "userref" to orderId,
                "pair" to when(order.instrument) {
                    CurrencyPair.BTC_EUR -> "XXBTZEUR"
                    CurrencyPair.ETH_EUR -> "XETHZEUR"
                    else -> throw UnsupportedOperationException(order.instrument.toString())
                },
                "volume" to order.originalAmount.toString(),
                "nonce" to nonceFactory.createValue().toString()
            )
        )

        LOG.debug { "placed order response: $response" }
        return orderId
    }

    private fun authRequest(endpoint: String, parameters: Map<String, String> = mapOf()): String {

        val authent = signRequest(endpoint, parameters)

        return runBlocking {
            val httpResponse: HttpResponse = client.post(BASE_URL + endpoint) {
                body = FormDataContent(Parameters.build {
                    parameters.forEach {
                        append(it.key, it.value)
                    }
                })
                header("API-Key", apiKey.apiKey)
                header("API-Sign", authent)
                header("User-Agent", "georgpro")
            }
            httpResponse.receive()
        }
    }

    private fun signRequest(endpoint: String, parameters: Map<String, String>): String {
        val message = parameters["nonce"] + parameters.entries.joinToString(separator = "&")

        val hash = endpoint.toByteArray().plus(MessageDigest.getInstance("SHA-256").digest(message.toByteArray(StandardCharsets.UTF_8)))

        // step 3: base64 decode api secret
        val secretDecoded = Base64.getDecoder().decode(apiKey.secret)

        // step 4: use result of step 3 to hash the result of step 2 with
        // HMAC-SHA512
        val hmacsha512 = Mac.getInstance("HmacSHA512")
        hmacsha512.init(SecretKeySpec(secretDecoded, "HmacSHA512"))
        val hash2 = hmacsha512.doFinal(hash)

        // step 5: base64 encode the result of step 4 and return
        return Base64.getEncoder().encodeToString(hash2)
    }
}