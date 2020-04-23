package net.hearthsim.hsreplay

import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import kotlinx.coroutines.runBlocking
import net.hearthsim.analytics.DefaultAnalytics
import net.hearthsim.console.DefaultConsole
import net.hearthsim.hsreplay.interceptor.UserAgentInterceptor
import net.mbonnin.jolly.UrlEncoder
import org.junit.Test
import java.io.File
import java.net.InetSocketAddress
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager


class TokenRetrievalTest {
    fun openBrowser(url: String) {
        ProcessBuilder().command("open", url)
                .inheritIO()
                .start()
                .waitFor()
    }

    /**
     * A test to retrieve tokens
     */
    @Test
    fun getTokens() {
        val url = "https://hsreplay.net/oauth2/authorize/?utm_source=arcanetracker&utm_medium=client" +
                "&response_type=code" +
                "&client_id=${UrlEncoder.encode(testOauthParams.clientId)}" +
                "&redirect_uri=${UrlEncoder.encode(testOauthParams.redirectUri)}" +
                "&scope=fullaccess" +
                "&state=notsorandomstrin"

        openBrowser(url)

        println("At this stage, chrome will show you a security warning without a lot of options to proceed.")
        println("Take the url and open it in curl with the '-k' option instead.")
        val code = getCode()

        println("code=$code")

        val accessTokenProvider = AccessTokenProvider(
                analytics = DefaultAnalytics(),
                preferences = TestPreferences(),
                oauthParams = testOauthParams,
                userAgent = testUserAgent
        )

        val oauthApi = HsReplayOauthApi(
                oauthParams = testOauthParams,
                userAgentInterceptor = UserAgentInterceptor(testUserAgent)
        )
        val tokenResult = runBlocking {
            oauthApi.login(code)
        }

        if (tokenResult !is HSReplayResult.Success) {
            assert(false)
            return
        }


        println("access_token=${tokenResult.value.access_token}")
        println("refresh_token=${tokenResult.value.refresh_token}")
    }

    private fun getCode(): String {

        var code: String? = null
        val lock = ReentrantLock()
        val condition = lock.newCondition()

        val server: HttpsServer = HttpsServer.create(InetSocketAddress(9000), 0)
        server.createContext("/") { exchange ->
            val response = "This is the response"

            exchange.sendResponseHeaders(200, response.length.toLong())
            val os = exchange.getResponseBody()
            os.write(response.toByteArray())
            os.close()

            lock.lock()
            code = exchange.requestURI.query.split("&")
                    .map {
                        it.split("=")
                    }
                    .firstOrNull {
                        it[0] == "code"
                    }?.get(1)
            condition.signal()
            lock.unlock()
        }
        server.setExecutor(null) // creates a default executor

        val sslContext = getSslContext()
        server.setHttpsConfigurator (HttpsConfigurator(sslContext))
        server.start()

        lock.lock()
        while (code == null) {
            condition.await()
        }
        lock.unlock()
        return code!!
    }

    private fun getSslContext(): SSLContext {
        val context = SSLContext.getInstance("TLS")

        val ks = KeyStore.getInstance("JKS")
        ks.load(File(System.getProperty("user.dir"), "src/commonTest/resources/keystore.jks").inputStream(), "pass_store".toCharArray())

        val kmf = KeyManagerFactory.getInstance("SunX509")
        kmf.init(ks, "pass_key".toCharArray())

        context.init(kmf.keyManagers, arrayOf(trustManager), null)
        return context
    }

    private val trustManager =  object: X509TrustManager{
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return emptyArray()
        }
    }
}