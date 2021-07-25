package at.gpro.arbitrader.security.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type


data class ApiKeyStore(
    val apiKeys : List<ApiKey>
) {
    companion object {
        fun from(file: File): ApiKeyStore? {
            val listType: Type = object : TypeToken<ArrayList<ApiKey>>() {}.type
            val apiKeys: List<ApiKey>? = Gson().fromJson(file.readText(), listType)
            return apiKeys?.let { ApiKeyStore(it) }
        }
    }
    fun getKey(exchange: String) = apiKeys.firstOrNull { it.exchange == exchange }
}

data class ApiKey(
    val exchange : String,
    val apiKey : String,
    val secret : String,
    val userName : String?,
    val specificParameter : SpecificParameter?
)

data class SpecificParameter(
    val key: String,
    val value: String
)
