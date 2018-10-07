package net.mbonnin.arcanetracker

import android.graphics.BitmapFactory
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import java.io.IOException

class PicassoBarRequestHandler : RequestHandler() {
    override fun canHandleRequest(data: Request): Boolean {
        return data.uri.scheme == "bar"

    }

    @Throws(IOException::class)
    override fun load(request: Request, networkPolicy: Int): RequestHandler.Result {

        val cardId = request.uri.host
        val loadedFrom = Picasso.LoadedFrom.DISK

        val inputStream = HDTApplication.context.getAssets().open("bars/$cardId.webp") ?: throw IOException()

        val b = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        return RequestHandler.Result(b, loadedFrom)
    }
}
