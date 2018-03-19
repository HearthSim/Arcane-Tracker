package net.mbonnin.arcanetracker.hsreplay

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import androidx.content.edit
import com.google.gson.Gson
import net.mbonnin.arcanetracker.hsreplay.model.Lce
import net.mbonnin.arcanetracker.hsreplay.model.Token
import net.mbonnin.arcanetracker.hsreplay.model.TokenRequest
import net.mbonnin.arcanetracker.hsreplay.model.UploadRequest
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rx.Observable
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber
import java.io.IOException

class HSReplay {
    private val mS3Client: OkHttpClient
    private var mToken: String? = null
    private val mService: Service

    fun claimUrl(): Observable<Lce<String>> = service().createClaim()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { claimResult -> Lce.data(claimResult.full_url!!) }
                .startWith(Lce.loading())
                .onErrorReturn({ Lce.error(it) })

    fun user(): Observable<Lce<Token>> = service().getToken(mToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map<Lce<Token>>({ Lce.data(it) })
                .onErrorReturn({ Lce.error(it) })
                .startWith(Lce.loading())

    val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun uploadGame(uploadRequest: UploadRequest, gameStr: String): Single<Lce<String>> {
        val hsReplayEnabled = token() != null

        Timber.w("uploadGame [hsReplayEnabled=%b] [token=%s]", hsReplayEnabled, mToken)

        if (mToken == null) {
            return Single.just(Lce.error(Exception("no token")))
        }

        if (!hsReplayEnabled) {
            return Single.just(Lce.error(Exception("hsreplay not enabled")))
        }

        Timber.w("doUploadGame")

        return service().createUpload("https://upload.hsreplay.net/api/v1/replay/upload/request", uploadRequest)
                .toSingle()
                .map {
                    Timber.w("url is " + it.url)
                    Timber.w("put_url is " + it.put_url)

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

    init {
        val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    var request = chain.request()

                    val requestBuilder = request.newBuilder()
                    requestBuilder.addHeader("X-Api-Key", "8b27e53b-0256-4ff1-b134-f531009c05a3")
                    requestBuilder.addHeader("User-Agent", userAgent)
                    if (mToken != null) {
                        requestBuilder.addHeader("Authorization", "Token " + mToken!!)
                    }
                    request = requestBuilder.build()

                    chain.proceed(request)
                }.build()

        val retrofit = Retrofit.Builder()
                .baseUrl("https://hsreplay.net/api/v1/")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create(Gson()))
                .client(client)
                .build()

        mService = retrofit.create(Service::class.java)

        mS3Client = OkHttpClient.Builder()
                .addInterceptor(GzipInterceptor())
                .build()

        mToken = sharedPreferences.getString(KEY_HSREPLAY_TOKEN, null)
        Timber.w("init token=" + mToken)
    }


    private fun service(): Service {
        return mService
    }

    fun token(): String? {
        return mToken
    }

    fun createToken(): Observable<Lce<String>> {
        val tokenRequest = TokenRequest()

        return service().createToken(tokenRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { token ->
                    if (token.key == null) {
                        throw RuntimeException("null key")
                    }
                    mToken = token.key
                    sharedPreferences.edit {
                        putString(KEY_HSREPLAY_TOKEN, token.key)
                    }
                    Lce.data<String>(mToken!!)
                }.onErrorReturn({ Lce.error(it) })
                .startWith(Lce.loading())
    }

    fun unlink() {
        mToken = null
        sharedPreferences.edit {
            remove(KEY_HSREPLAY_TOKEN)
        }
    }

    companion object {
        const val KEY_HSREPLAY_TOKEN = "HSREPLAY_TOKEN"

        @SuppressLint("StaticFieldLeak")
        private var sHSReplay: HSReplay? = null
        var userAgent=  "Arcane Tracker"
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
