package net.mbonnin.arcanetracker.ui.main

import android.annotation.SuppressLint
import android.net.Uri
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.hsreplay.HSReplay
import net.mbonnin.arcanetracker.hsreplay.OauthInterceptor
import okhttp3.HttpUrl
import java.util.*

@SuppressLint("SetJavaScriptEnabled")
class OauthCompanion(val view: View, val successCallback: (View) -> Unit, val cancelCallback: (View) -> Unit) {
    fun random(): String {
        val generator = Random()
        val randomStringBuilder = StringBuilder()
        val c = "abcdefghijklmnopqrstuvwxyz0123456789"

        for (i in 0 until 16) {
            randomStringBuilder.append(c[generator.nextInt(c.length)])
        }
        return randomStringBuilder.toString()
    }


    init {
        val webView = view.findViewById<WebView>(R.id.webView)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(webView2: WebView?, url: String): Boolean {
                if (url.startsWith(OauthInterceptor.CALLBACK_URL)) {
                    val uri = Uri.parse(url)

                    val code = uri?.getQueryParameter("code")
                    if (code != null) {
                        val d = Completable.fromAction {
                            OauthInterceptor.exchangeCode(code)
                        }.andThen(HSReplay.get().user())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    successCallback(view)
                                }
                        return true
                    } else {
                        return false
                    }
                }

                return false
            }
        }

        val url = HttpUrl.parse(OauthInterceptor.AUTHORIZE_URL)!!
                .newBuilder()
                .addQueryParameter("response_type", "code")
                .addQueryParameter("client_id", OauthInterceptor.A)
                .addQueryParameter("redirect_uri", OauthInterceptor.CALLBACK_URL)
                .addQueryParameter("scope", "fullaccess")
                .addQueryParameter("state", random())
        webView.loadUrl(url.toString())

        view.setOnClickListener {
            cancelCallback(view)
        }
    }
}