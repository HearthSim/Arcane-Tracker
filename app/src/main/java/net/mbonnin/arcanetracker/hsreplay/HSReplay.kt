package net.mbonnin.arcanetracker.hsreplay

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import net.mbonnin.arcanetracker.HDTApplication
import net.mbonnin.arcanetracker.hsreplay.model.*
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.IOException

class HSReplay {
    private val mS3Client: OkHttpClient
    private var mLegacyToken: String? = null
    private var mOauthervice: OauthService
    private val mLegacyService: LegacyService

    fun user(): Observable<Lce<Token>> = legacyService().getToken(mLegacyToken)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map<Lce<Token>> { Lce.data(it) }
            .onErrorReturn { Lce.error(it) }
            .startWith(Lce.loading())

    val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun uploadGame(uploadRequest: UploadRequest, gameStr: String): Single<Lce<String>> {
        val hsReplayEnabled = token() != null

        Timber.w("uploadGame [hsReplayEnabled=%b] [token=%s]", hsReplayEnabled, mLegacyToken)

        if (mLegacyToken == null) {
            return Single.just(Lce.error(Exception("no token")))
        }

        if (!hsReplayEnabled) {
            return Single.just(Lce.error(Exception("hsreplay not enabled")))
        }

        Timber.w("doUploadGame")
        FirebaseAnalytics.getInstance(HDTApplication.context).logEvent("hsreplay_upload", null)

        return legacyService().createUpload("https://upload.hsreplay.net/api/v1/replay/upload/request", uploadRequest)
                .firstOrError()
                .map {
                    Timber.w("url is ${it.url}")
                    Timber.w("put_url is ${it.put_url}")

                    putToS3(it.put_url, gameStr)
                    it
                }
                .map { Lce.data(it.url!!) }
                .onErrorReturn { Lce.error(it) }
    }

    private fun putToS3(putUrl: String?, gameStr: String) {

        if (putUrl == null) {
            throw Exception("no put_url")
        }

        val body = RequestBody.create(MediaType.parse("text/plain"), gameStr)
        val request = Request.Builder()
                .put(body)
                .url(putUrl)
                .addHeader("User-Agent", userAgent)
                .build()

        try {
            val response = mS3Client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("response not successful")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    inner class LegacyInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()

            val requestBuilder = request.newBuilder()
            requestBuilder.addHeader("X-Api-Key", "8b27e53b-0256-4ff1-b134-f531009c05a3")
            requestBuilder.addHeader("User-Agent", HSReplay.userAgent)
            if (mLegacyToken != null) {
                requestBuilder.addHeader("Authorization", "Token " + mLegacyToken!!)
            }
            request = requestBuilder.build()

            return chain.proceed(request)
        }
    }

    init {
        val legacyClient = OkHttpClient.Builder()
                .addInterceptor(LegacyInterceptor())
                .build()

        mLegacyService = Retrofit.Builder()
                .baseUrl("https://hsreplay.net/api/v1/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create(Gson()))
                .client(legacyClient)
                .build()
                .create(LegacyService::class.java)

        val oauthOkHttpClient = OkHttpClient.Builder()
                .addInterceptor(OauthInterceptor())
                .build()

        mOauthervice = Retrofit.Builder()
                .baseUrl("https://api.hsreplay.net/v1/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(io.reactivex.schedulers.Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create(Gson()))
                .client(oauthOkHttpClient)
                .build()
                .create(OauthService::class.java)


        mS3Client = OkHttpClient.Builder()
                .addInterceptor(GzipInterceptor())
                .build()

        mLegacyToken = sharedPreferences.getString(KEY_HSREPLAY_LEGACY_TOKEN, null)
        Timber.w("init token=$mLegacyToken")
    }

    fun getAccount(): Observable<Account>? {
        return mOauthervice.account()
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .doOnNext {
                    sharedPreferences.edit {
                        putBoolean(KEY_HSREPLAY_PREMIUM, it.is_premium ?: false)
                        putString(KEY_HSREPLAY_BATTLETAG, it.battletag)
                    }
                }
    }

    fun battleTag() = sharedPreferences.getString(KEY_HSREPLAY_BATTLETAG, null)
    fun isPremium() = sharedPreferences.getBoolean(KEY_HSREPLAY_PREMIUM, false)

    private fun legacyService(): LegacyService {
        return mLegacyService
    }

    fun token(): String? {
        return mLegacyToken
    }

    fun createToken(): Observable<Lce<String>> {
        val tokenRequest = TokenRequest()

        return legacyService().createToken(tokenRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { token ->
                    if (token.key == null) {
                        throw RuntimeException("null key")
                    }
                    mLegacyToken = token.key
                    sharedPreferences.edit {
                        putString(KEY_HSREPLAY_LEGACY_TOKEN, token.key)
                    }
                    Lce.data<String>(mLegacyToken!!)
                }.onErrorReturn({ Lce.error(it) })
                .startWith(Lce.loading())
    }

    fun unlink() {
        mLegacyToken = null
        sharedPreferences.edit {
            remove(KEY_HSREPLAY_LEGACY_TOKEN)
        }
    }

    companion object {
        const val KEY_HSREPLAY_LEGACY_TOKEN = "HSREPLAY_TOKEN"
        const val KEY_HSREPLAY_PREMIUM = "HSREPLAY_PREMIUM"
        const val KEY_HSREPLAY_BATTLETAG = "HSREPLAY_BATTLETAG"

        @SuppressLint("StaticFieldLeak")
        private var sHSReplay: HSReplay? = null
        var userAgent = "HDT"
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        fun get(): HSReplay {
            if (sHSReplay == null) {
                sHSReplay = HSReplay()
            }

            return sHSReplay!!
        }
    }
}
