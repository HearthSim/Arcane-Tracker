package internal.di.view;

import android.app.Application;
import android.view.View;

import dagger.android.DispatchingAndroidInjector;

import static dagger.internal.Preconditions.checkNotNull;

/**
 * Created by williamwebb on 4/9/17.
 */

public class AndroidViewInjection {

  public static void inject(View view) {
    checkNotNull(view, "view");
    Application application = (Application) view.getContext().getApplicationContext();
    if (!(application instanceof HasDispatchingViewInjector)) {
      throw new RuntimeException(
          String.format(
              "%s does not implement %s",
              application.getClass().getCanonicalName(),
              HasDispatchingViewInjector.class.getCanonicalName()));
    }

    DispatchingAndroidInjector<View> viewInjector = ((HasDispatchingViewInjector) application).viewInjector();
    checkNotNull(
        viewInjector,
        "%s.viewInjector() returned null",
        application.getClass().getCanonicalName());

    viewInjector.inject(view);
  }

}
