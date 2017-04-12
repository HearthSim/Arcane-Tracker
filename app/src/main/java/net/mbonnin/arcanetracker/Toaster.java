package net.mbonnin.arcanetracker;

import android.content.Context;
import android.support.annotation.StringRes;
import android.widget.Toast;

/**
 * Created by williamwebb on 4/11/17.
 */

public class Toaster {

  private final Context context;

  public Toaster(Context context) {
    this.context = context;
  }

  public void toast(String message, int length) {
    Toast.makeText(context, message, length).show();
  }

  public void toast(@StringRes int message, int length) {
    Toast.makeText(context, message, length).show();
  }

}
