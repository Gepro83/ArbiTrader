package at.gpro.arbitrader.kraken

import at.gpro.arbitrader.security.model.ApiKey
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.Order
import org.knowm.xchange.dto.marketdata.OrderBook
import org.knowm.xchange.dto.trade.LimitOrder
import org.knowm.xchange.dto.trade.MarketOrder
import org.knowm.xchange.instrument.Instrument
import org.knowm.xchange.utils.nonce.CurrentTimeIncrementalNonceFactory
import si.mazi.rescu.SynchronizedValueFactory
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class Kraken(private val apiKey: ApiKey) {

    private val nonceFactory : SynchronizedValueFactory<Long> = CurrentTimeIncrementalNonceFactory(TimeUnit.MILLISECONDS)

    private val LOG = KotlinLogging.logger {}

    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true

            })
        }
    }

    companion object {
        const val BASE_URL = "https://api.kraken.com"
    }

    fun getOrderBook(pair: CurrencyPair): OrderBook? {
        val endpoint = "/0/public/Depth"

        val beforeRequest = Date()

        val responseDTO = authRequest<KrakenOrderbookDTO>(
            endpoint,
            mapOf(
                "pair" to pair.toKrakenPair(),
                "nonce" to nonceFactory.createValue().toString(),
                "count" to "50"
            )
        )

        if (responseDTO.error.isNotEmpty())
            LOG.warn { "Error from kraken: ${responseDTO.error}" }

        val asks = when(pair) {
            CurrencyPair.BTC_EUR -> responseDTO.result.XXBTZEUR?.asks
            CurrencyPair.ETH_EUR -> responseDTO.result.XETHZEUR?.asks
            CurrencyPair.ETH_BTC -> responseDTO.result.XETHXXBT?.asks
            else -> null
        } ?: return null

        val bids = when(pair) {
            CurrencyPair.BTC_EUR -> responseDTO.result.XXBTZEUR?.bids
            CurrencyPair.ETH_EUR -> responseDTO.result.XETHZEUR?.bids
            CurrencyPair.ETH_BTC -> responseDTO.result.XETHXXBT?.bids
            else -> null
        } ?: return null

        return OrderBook(
            beforeRequest,
            asks.map {
                LimitOrder.Builder(Order.OrderType.ASK, pair)
                    .limitPrice(it[0].toBigDecimal())
                    .originalAmount(it[1].toBigDecimal())
                    .build()
            },
            bids.map {
                LimitOrder.Builder(Order.OrderType.BID, pair)
                    .limitPrice(it[0].toBigDecimal())
                    .originalAmount(it[1].toBigDecimal())
                    .build()
            }
        )

    }

    private fun Instrument.toKrakenPair(): String =
        when(this) {
            CurrencyPair.BTC_EUR -> "XXBTZEUR"
            CurrencyPair.ETH_EUR -> "XETHZEUR"
            CurrencyPair.ETH_BTC -> "XETHXXBT"
            else -> throw UnsupportedOperationException(this.toString())
        }

    fun place(order: MarketOrder): String {
        val endpoint = "/0/private/AddOrder"

        val orderId = Random().nextInt().toString()
        val response = authRequest<String>(endpoint,
            mapOf("ordertype" to "market",
                "type" to order.type.let { when(it) {
                    Order.OrderType.ASK -> "sell"
                    Order.OrderType.BID -> "buy"
                    else -> throw UnsupportedOperationException(order.type.name)
                } },
                "userref" to orderId,
                "pair" to order.instrument.toKrakenPair(),
                "volume" to order.originalAmount.toString(),
                "nonce" to nonceFactory.createValue().toString()
            )
        )

        LOG.debug { "placed order response: $response" }
        return orderId
    }

    private inline fun <reified T> authRequest(endpoint: String, parameters: Map<String, String> = mapOf()): T {

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