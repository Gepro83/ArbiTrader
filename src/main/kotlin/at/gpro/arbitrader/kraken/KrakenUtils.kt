package at.gpro.arbitrader.kraken

import java.util.*

object KrakenUtils {

    private var nonce = 0

    fun getNonce(): String {
        return String.format("%s%04d", Date().time, nonce++)
    }

}