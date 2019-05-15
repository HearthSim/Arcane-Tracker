package net.mbonnin.arcanetracker.hsreplay

import okhttp3.Interceptor
import okhttp3.Response

class LegacyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val requestBuilder = request.newBuilder()
        requestBuilder.addHeader("X-Api-Key", "8b27e53b-0256-4ff1-b134-f531009c05a3")
        requestBuilder.addHeader("User-Agent", HSReplay.userAgent)
        request = requestBuilder.build()

        return chain.proceed(request)
    }
}