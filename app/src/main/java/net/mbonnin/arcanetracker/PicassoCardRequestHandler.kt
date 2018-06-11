package net.mbonnin.arcanetracker

import android.graphics.BitmapFactory
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import okhttp3.internal.cache.DiskLruCache
import okhttp3.internal.io.FileSystem
import okio.Okio
import timber.log.Timber
import java.io.*

class PicassoCardRequestHandler private constructor() : RequestHandler() {
    internal val cache: DiskLruCache

    init {
        val file = File(ArcaneTrackerApplication.context.cacheDir, "cardsCache")
        cache = DiskLruCache.create(FileSystem.SYSTEM, file, VERSION, ENTRY_COUNT, (250 * 1024 * 1024).toLong())
    }

    override fun canHandleRequest(data: Request): Boolean {
        return data.uri.scheme == "card"

    }

    @Throws(IOException::class)
    override fun load(request: Request, networkPolicy: Int): RequestHandler.Result? {

        val cardId = request.uri.path.substring(1)
        val langKey = request.uri.host
        var loadedFrom: Picasso.LoadedFrom = Picasso.LoadedFrom.DISK

        var inputStream: InputStream? = null
        if (true) {
            synchronized(cache) {
                try {
                    val snapshot = cache.get(getKey(cardId, langKey))
                    if (snapshot != null) {
                        inputStream = Okio.buffer(snapshot.getSource(0)).inputStream()
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }

            }
        }

        if (inputStream == null) {
            val outputStream = ByteArrayOutputStream()

            val result: CardRenderer.Result

            try {
                result = CardRenderer.get().renderCard(cardId, outputStream)
            } catch (e: Exception) {
                Timber.e(e)
                return null
            }

            val bytes = outputStream.toByteArray()

            if (result == CardRenderer.Result.SUCCESS) {
                synchronized(cache) {
                    val editor = cache.edit(getKey(cardId, langKey))
                    if (editor != null) {
                        val sink = editor.newSink(0)
                        val os = Okio.buffer(sink).outputStream()
                        os.write(bytes)
                        os.close()
                        editor.commit()
                    }
                }
            }
            inputStream = ByteArrayInputStream(bytes)
            outputStream.close()
            loadedFrom = Picasso.LoadedFrom.NETWORK
        }

        val result: RequestHandler.Result
        val b = BitmapFactory.decodeStream(inputStream)
        if (b != null) {
            result = RequestHandler.Result(b, loadedFrom)
        } else {
            throw IOException()
        }
        inputStream!!.close()

        return result
    }

    fun resetCache() {
        synchronized(cache) {
            try {
                cache.evictAll()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    companion object {
        private val VERSION = 8 // bump here to force flushing the cache
        private val ENTRY_COUNT = 1
        private var sHandler: PicassoCardRequestHandler? = null

        private fun getKey(cardId: String, langKey: String): String {
            val builder = StringBuilder()
            for (i in 0 until cardId.length) {
                val c = cardId[i]

                if (c >= 'A' && c <= 'Z') {
                    builder.append(Character.toLowerCase(c))
                } else if ((c >= '0') and (c <= '9')) {
                    builder.append(c)
                } else if (c >= 'a' && c <= 'z') {
                    builder.append(c)
                }
            }

            /**
             * XXX: this might become a bit wrong
             */
            builder.append("_")
            builder.append(langKey)
            return builder.toString()
        }

        fun get(): PicassoCardRequestHandler {
            if (sHandler == null) {
                sHandler = PicassoCardRequestHandler()
            }

            return sHandler!!
        }
    }
}
