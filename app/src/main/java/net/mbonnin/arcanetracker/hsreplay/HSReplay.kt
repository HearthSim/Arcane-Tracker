package net.mbonnin.arcanetracker.hsreplay

import android.content.Context
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.google.gson.Gson
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.hsreplay.model.Account
import net.mbonnin.arcanetracker.hsreplay.model.legacy.UploadRequest
import net.mbonnin.arcanetracker.hsreplay.model.legacy.UploadToken
import net.mbonnin.arcanetracker.hsreplay.model.legacy.UploadTokenRequest
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import ru.gildor.coroutines.retrofit.await
import timber.log.Timber
import java.io.IOException

class HSReplay(val context: Context, val userAgent: String) {
    private var legacyToken: String? = null

    private val S3Client by lazy {
        OkHttpClient.Builder()
                .addInterceptor(GzipInterceptor())
                .build()
    }
    private val mOauthervice by lazy {
        val oauthOkHttpClient = OkHttpClient.Builder()
                .addInterceptor(HsReplayInterceptor())
                .build()

        Retrofit.Builder()
                .baseUrl("https://api.hsreplay.net/v1/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create(Gson()))
                .client(oauthOkHttpClient)
                .build()
                .create(HsReplayService::class.java)
    }
    private val legacyService by lazy {
        val legacyClient = OkHttpClient.Builder()
                .addInterceptor(LegacyInterceptor(userAgent))
                .build()

        Retrofit.Builder()
                .baseUrl("https://hsreplay.net/api/v1/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create(Gson()))
                .client(legacyClient)
                .build()
                .create(LegacyService::class.java)
    }
    val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    init {

        legacyToken = sharedPreferences.getString(KEY_HSREPLAY_LEGACY_TOKEN, null)
        Timber.w("init token=${legacyToken != null}")

        if (HsReplayInterceptor.refreshToken != null) {
            GlobalScope.launch {
                if (legacyToken == null) {
                    // This should never happen as the legacyToken must be set at login time
                    Utils.reportNonFatal(Exception("no token for logged user ?"))
                    createLegacyToken().getOrNull()?.let {
                        legacyToken = it
                        sharedPreferences.edit {
                            putString(KEY_HSREPLAY_LEGACY_TOKEN, it)
                        }
                    }
                }
                val accountResult = account()
                accountResult.getOrNull()?.let {
                    sharedPreferences.edit {
                        putBoolean(KEY_HSREPLAY_PREMIUM, it.is_premium ?: false)
                        putString(KEY_HSREPLAY_BATTLETAG, it.battletag)
                        putString(KEY_HSREPLAY_USERNAME, it.username)
                    }
                }
            }
        }
    }

    suspend fun uploadGame(uploadRequest: UploadRequest, gameStr: String): Result<String> {

        Timber.w("uploadGame [token=$legacyToken]")

        if (legacyToken == null) {
            return Result.failure(Exception("no token"))
        }

        Timber.w("doUploadGame")

        val authorization = "Token $legacyToken"

        val upload = try {
            legacyService.createUpload("https://upload.hsreplay.net/api/v1/replay/upload/request", uploadRequest, authorization).await()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        Timber.w("url is ${upload.url}")
        Timber.w("put_url is ${upload.put_url}")

        val exception = withContext(Dispatchers.IO) {
            putToS3(upload.put_url, gameStr).exceptionOrNull()
        }
        if (exception != null) {
            return Result.failure(exception)
        }

        return Result.success(upload.url!!)
    }

    private fun putToS3(putUrl: String?, gameStr: String): Result<Unit> {

        if (putUrl == null) {
            return Result.failure(Exception("not put url"))
        }

        val body = RequestBody.create(MediaType.parse("text/plain"), gameStr)
        val request = Request.Builder()
                .put(body)
                .url(putUrl)
                .addHeader("User-Agent", userAgent)
                .build()

        try {
            val response = S3Client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("response not successful"))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    suspend fun uploadToken(): Result<UploadToken> {
        try {
            return Result.success(legacyService.getToken(legacyToken!!).await())
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun account(): Result<Account> {
        return try {
            Result.success(mOauthervice.account().await())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun claimToken(legacyToken: String): Result<Unit> = coroutineScope {
        val str = "{\"token\": \"$legacyToken\"}"
        val claim = async(Dispatchers.IO) {
            try {
                // The coroutines extensions for retrofit will fail on an empty body
                // https://github.com/JakeWharton/retrofit2-kotlin-coroutines-adapter/issues/5
                val response = mOauthervice.claimToken(RequestBody.create(MediaType.parse("application/json"), str)).execute()
                if (!response.isSuccessful) {
                    throw Exception("Cannot claim token: ${response.code()}")
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure<Unit>(e)
            }
        }

        claim.await()
    }

    suspend fun createLegacyToken(): Result<String> {
        val tokenRequest = UploadTokenRequest()

        return try {
            Result.success(legacyService.createToken(tokenRequest).await().key!!)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    // Not sure how different this is from username
    fun battleTag() = sharedPreferences.getString(KEY_HSREPLAY_BATTLETAG, null)

    fun username() = sharedPreferences.getString(KEY_HSREPLAY_USERNAME, null)
    fun isPremium() = sharedPreferences.getBoolean(KEY_HSREPLAY_PREMIUM, false)

    fun token(): String? {
        return legacyToken
    }

    fun logout() {
        HsReplayInterceptor.logout()
        legacyToken = null
        sharedPreferences.edit {
            remove(KEY_HSREPLAY_LEGACY_TOKEN)
            remove(KEY_HSREPLAY_PREMIUM)
            remove(KEY_HSREPLAY_BATTLETAG)
            remove(KEY_HSREPLAY_USERNAME)
        }
    }


    suspend fun login(code: String): Result<Unit> = coroutineScope {
        val tokenDeferred = async {
            createLegacyToken()
        }

        val oauthResult = HsReplayInterceptor.login(code)
        if (oauthResult.isFailure) {
            return@coroutineScope oauthResult.map { Unit }
        }
        val tokenResult = tokenDeferred.await()
        if (tokenResult.isFailure) {
            HsReplayInterceptor.logout()
            return@coroutineScope tokenResult.map { Unit }
        }

        val claimResult = claimToken(tokenResult.getOrNull()!!)
        if (claimResult.isFailure) {
            HsReplayInterceptor.logout()
            return@coroutineScope claimResult.map { Unit }
        }

        val accountResult = account()
        if (accountResult.isFailure) {
            HsReplayInterceptor.logout()
            return@coroutineScope accountResult.map { Unit }
        }
        accountResult.getOrNull()!!.let {
            sharedPreferences.edit {
                legacyToken = tokenResult.getOrNull()!!

                putString(KEY_HSREPLAY_LEGACY_TOKEN, legacyToken!!)

                putBoolean(KEY_HSREPLAY_PREMIUM, it.is_premium ?: false)
                putString(KEY_HSREPLAY_BATTLETAG, it.battletag)
                putString(KEY_HSREPLAY_USERNAME, it.username)
            }
        }
        return@coroutineScope Result.success(Unit)
    }


    companion object {
        const val KEY_HSREPLAY_LEGACY_TOKEN = "HSREPLAY_TOKEN"
        const val KEY_HSREPLAY_PREMIUM = "HSREPLAY_PREMIUM"
        const val KEY_HSREPLAY_BATTLETAG = "HSREPLAY_BATTLETAG"
        const val KEY_HSREPLAY_USERNAME = "HSREPLAY_USERNAME"
    }
}
