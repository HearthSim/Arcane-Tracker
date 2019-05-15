package net.mbonnin.arcanetracker.hsreplay

import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import okhttp3.Interceptor
import okhttp3.Response

class LegacyInterceptor(val userAgent: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val requestBuilder = request.newBuilder()
        requestBuilder.addHeader("X-Api-Key", "8b27e53b-0256-4ff1-b134-f531009c05a3")
        requestBuilder.addHeader("User-Agent", userAgent)
        request = requestBuilder.build()

        return chain.proceed(request)
    }
}