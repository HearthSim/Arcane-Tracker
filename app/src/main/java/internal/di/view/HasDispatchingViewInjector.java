package internal.di.view;

import android.view.View;

import dagger.android.DispatchingAndroidInjector;

/**
 * Created by williamwebb on 4/9/17.
 */

public interface HasDispatchingViewInjector {
  DispatchingAndroidInjector<View> viewInjector();
}
