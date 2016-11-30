package net.mbonnin.arcanetracker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.esotericsoftware.kryo.io.Input;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;
import java.io.InputStream;

public class PicassoBarRequestHandler extends RequestHandler {
    @Override
    public boolean canHandleRequest(Request data) {
        if (data.uri.getScheme().equals("bar")) {
            return true;
        }

        return false;
    }

    @Override
    public Result load(Request request, int networkPolicy) throws IOException {

        String cardId = request.uri.getHost();
        Picasso.LoadedFrom loadedFrom = Picasso.LoadedFrom.DISK;

        InputStream inputStream = ArcaneTrackerApplication.getContext().getAssets().open("bars/" + cardId + ".webp");
        if (inputStream == null) {
            throw  new IOException();
        }

        Bitmap b = BitmapFactory.decodeStream(inputStream);
        inputStream.close();

        return new Result(b, loadedFrom);
    }
}
