package net.mbonnin.jolly

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okio.Buffer
import okio.Source

enum class Method {
    GET,
    PUT,
    POST,
    DELETE,
}

data class JollyRequest(
        val method: Method,
        val url: String,
        val body: ByteArray?,
        val headers: Map<String, String>
)

data class JollyResponse(
        val statusCode: Int,
        val body: ByteArray?,
        val error: Exception?
)

expect class JollyEngine() {
    suspend fun execute(jollyRequest: JollyRequest): JollyResponse
}

abstract class JollyInterceptor {
    abstract suspend fun intercept(chain: JollyChain): JollyResponse
}

class JollyChain(val interceptors: List<JollyInterceptor>,
                 val index: Int,
                 val request: JollyRequest) {

    suspend fun proceed(jollyRequest: JollyRequest): JollyResponse {
        return interceptors.get(index).intercept(JollyChain(interceptors, index + 1, jollyRequest))
    }
}

class NetworkInterceptor : JollyInterceptor() {
    override suspend fun intercept(chain: JollyChain): JollyResponse {
        return JollyEngine().execute(chain.request)
    }

}

class JollyClient {
    val interceptors = mutableListOf<JollyInterceptor>()

    fun addInterceptor(interceptor: JollyInterceptor) {
        interceptors.add(interceptor)
    }

    suspend fun execute(jollyRequest: JollyRequest): JollyResponse {
        return JollyChain(interceptors + NetworkInterceptor(), 0, jollyRequest).proceed(jollyRequest)
    }
}

class JollyRequestBuilder internal constructor() {
    private var url: String? = null
    private var headers = mutableMapOf<String, String>()
    private var body: ByteArray? = null
    private var method: Method? = null

    fun url(url: String) {
        this.url = url
    }

    fun addHeader(key: String, value: String) {
        this.headers.put(key, value)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun body(contentType: String, body: String) {
        body(contentType, body.encodeToByteArray())
    }

    fun method(method: Method) {
        this.method = method
    }

    fun body(contentType: String, body: ByteArray) {
        this.headers.put("Content-Type", contentType)
        this.body = body
    }

    fun <T> body(serializer: SerializationStrategy<T>, t: T) {
        val json = Json(
                JsonConfiguration(
                        encodeDefaults = false,
                        ignoreUnknownKeys = true,
                        isLenient = true
                )
        )
        this.body("application/json", json.toJson(serializer, t).toString())
    }

    internal fun build(): JollyRequest {
        return JollyRequest(
                method = this.method!!,
                url = this.url!!,
                body = this.body,
                headers = this.headers
        )
    }
}

fun JollyRequest(block: JollyRequestBuilder.() -> Unit): JollyRequest {
    return JollyRequestBuilder().apply {
        this.block()
    }.build()
}