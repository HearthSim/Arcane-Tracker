package internal.di.view;

import android.view.View;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import dagger.MapKey;

/**
 * Created by williamwebb on 4/9/17.
 */

@MapKey
@Target({ElementType.METHOD})
public @interface ViewKey {
  Class<? extends View> value();
}