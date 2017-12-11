package net.mbonnin.arcanetracker.hsreplay

import android.os.Build
import android.widget.Toast
import com.google.gson.Gson
import net.mbonnin.arcanetracker.*
import net.mbonnin.arcanetracker.hsreplay.model.Token
import net.mbonnin.arcanetracker.hsreplay.model.TokenRequest
import net.mbonnin.arcanetracker.hsreplay.model.UploadRequest
import net.mbonnin.arcanetracker.model.GameSummary
import net.mbonnin.arcanetracker.parser.Game
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func1
import rx.schedulers.Schedulers
import timber.log.Timber
import java.io.IOException
import java.util.*

/**
 * Created by martin on 11/29/16.
 */

class HSReplay {
    private val mS3Client: OkHttpClient
    private val mUserAgent: String
    private var mToken: String? = null
    var gameSummary: ArrayList<GameSummary>? = null
        private set
    private val mService: Service

    val claimUrl: Observable<Lce<String>>
        get() = service().createClaim()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { claimResult -> Lce.data(claimResult.full_url) }
                .startWith(Lce.loading())
                .onErrorReturn(Func1<Throwable, Lce<String>> { Lce.error(it) })

    val user: Observable<Lce<Token>>
        get() = service().getToken(mToken)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map<Lce<Token>>(Func1<Token, Lce<Token>> { Lce.data(it) })
                .onErrorReturn(Func1<Throwable, Lce<Token>> { Lce.error(it) })
                .startWith(Lce.loading())

    fun doUploadGame(matchStart: String, friendlyPlayerId: String, game: Game, summary: GameSummary, gameStr: String) {
        Timber.w("doUploadGame")

        val uploadRequest = UploadRequest()
        uploadRequest.match_start = matchStart
        uploadRequest.build = 20022
        uploadRequest.spectator_mode = game.spectator
        uploadRequest.friendly_player = friendlyPlayerId
        uploadRequest.game_type = summary.bnetGameType
        if (game.rank > 0) {
            if (friendlyPlayerId == "1") {
                uploadRequest.player1.rank = game.rank
            } else {
                uploadRequest.player2.rank = game.rank
            }
        }

        service().createUpload("https://upload.hsreplay.net/api/v1/replay/upload/request", uploadRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { upload ->
                    Timber.w("url is " + upload.url)
                    Timber.w("put_url is " + upload.put_url)

                    if (upload.put_url == null) {
                        return@flatMap Observable.error<Throwable>(Exception("no put_url"))
                    }

                    summary.hsreplayUrl = upload.url
                    PaperDb.write<ArrayList<GameSummary>>(KEY_GAME_LIST, gameSummary!!)

                    putToS3(upload.put_url, gameStr).subscribeOn(Schedulers.io())
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ unused ->
                    Timber.d("hsreplay upload success")
                    Toast.makeText(ArcaneTrackerApplication.context, ArcaneTrackerApplication.context.getString(R.string.hsreplaySuccess), Toast.LENGTH_LONG).show()
                }) { error ->
                    Timber.e(error)
                    Toast.makeText(ArcaneTrackerApplication.context, ArcaneTrackerApplication.context.getString(R.string.hsreplayError), Toast.LENGTH_LONG).show()
                }
    }

    fun uploadGame(matchStart: String, game: Game?, gameStr: String) {
        val hsReplayEnabled = HSReplay.get().token() != null

        Timber.w("uploadGame [game=%s] [hsReplayEnabled=%b] [token=%s]", game, hsReplayEnabled, mToken)
        if (game == null) {
            return
        }

        val summary = GameSummary()
        summary.coin = game.getPlayer().hasCoin
        summary.win = game.victory
        summary.hero = game.player.classIndex()
        summary.opponentHero = game.opponent.classIndex()
        summary.date = Utils.ISO8601DATEFORMAT.format(Date())
        summary.deckName = MainViewCompanion.getPlayerCompanion().deck.name
        summary.bnetGameType = game.bnetGameType.intValue

        gameSummary!!.add(0, summary)
        PaperDb.write<ArrayList<GameSummary>>(KEY_GAME_LIST, gameSummary!!)

        if (mToken == null) {
            return
        }

        if (hsReplayEnabled) {
            doUploadGame(matchStart, game.player.entity.PlayerID, game, summary, gameStr)
        }
    }

    fun putToS3(putUrl: String, gameStr: String): Observable<Unit> {
        return Observable.fromCallable {
            val body = RequestBody.create(null, gameStr)
            val request = Request.Builder()
                    .put(body)
                    .url(putUrl)
                    .header("Content-Type", "text/plain")
                    .addHeader("User-Agent", mUserAgent)
                    .build()

            try {
                val response = mS3Client.newCall(request).execute()
                if (!response.isSuccessful) {
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    init {
        gameSummary = PaperDb.read<ArrayList<GameSummary>>(KEY_GAME_LIST)
        if (gameSummary == null) {
            gameSummary = ArrayList()
        }

        mUserAgent = (ArcaneTrackerApplication.context.getPackageName() + "/" + BuildConfig.VERSION_NAME
                + "; Android " + Build.VERSION.RELEASE + ";")

        val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    var request = chain.request()

                    val requestBuilder = request.newBuilder()
                    requestBuilder.addHeader("X-Api-Key", "8b27e53b-0256-4ff1-b134-f531009c05a3")
                    requestBuilder.addHeader("User-Agent", mUserAgent)
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

        mToken = Settings.get(Settings.HSREPLAY_TOKEN, null)
        Timber.w("init token=" + mToken!!)

        //doUploadGame(Utils.ISO8601DATEFORMAT.format(new Date()), "1", null, "toto");
    }

    fun service(): Service {
        return mService
    }

    fun token(): String? {
        return mToken
    }

    fun createToken(): Observable<Lce<String>> {
        val tokenRequest = TokenRequest()
        tokenRequest.test_data = Utils.isAppDebuggable
        return service().createToken(tokenRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { token ->
                    if (token.key == null) {
                        throw RuntimeException("null key")
                    }
                    mToken = token.key
                    Settings.set(Settings.HSREPLAY_TOKEN, token.key)
                    Lce.data<String>(mToken)
                }.onErrorReturn(Func1<Throwable, Lce<String>> { Lce.error(it) })
                .startWith(Lce.loading())
    }

    fun unlink() {
        mToken = null
        Settings.set(Settings.HSREPLAY_TOKEN, null)
    }

    fun eraseGameSummary() {
        gameSummary!!.clear()
        PaperDb.write<ArrayList<GameSummary>>(KEY_GAME_LIST, gameSummary!!)
    }

    companion object {
        private val KEY_GAME_LIST = "KEY_GAME_LIST"
        private var sHSReplay: HSReplay? = null

        fun get(): HSReplay {
            if (sHSReplay == null) {
                sHSReplay = HSReplay()
            }

            return sHSReplay!!
        }
    }
}
