package internal.di;

import android.app.Activity;
import android.app.Service;
import android.view.View;

import net.mbonnin.arcanetracker.CreateDeckView;
import net.mbonnin.arcanetracker.DeckEditorView;
import net.mbonnin.arcanetracker.HandleView;
import net.mbonnin.arcanetracker.HandlesView;
import net.mbonnin.arcanetracker.MainActivity;
import net.mbonnin.arcanetracker.MainService;
import net.mbonnin.arcanetracker.SettingsActivity;

import dagger.Binds;
import dagger.Module;
import dagger.android.ActivityKey;
import dagger.android.AndroidInjector;
import dagger.android.ServiceKey;
import dagger.multibindings.IntoMap;
import internal.di.view.ViewKey;

import static internal.di.ActivityBindings.*;
import static internal.di.ServiceBindings.*;
import static internal.di.ViewBindings.*;

/**
 * Created by williamwebb on 4/9/17.
 */

@Module(includes = {
    ViewBindings.class,
    ActivityBindings.class,
    ServiceBindings.class
})
abstract class BuilderModule {
  @Binds
  @IntoMap
  @ViewKey(HandlesView.class)
  abstract AndroidInjector.Factory<? extends View> handlesView(HandlesViewSubComponent.Builder builder);

  @Binds
  @IntoMap
  @ViewKey(HandleView.class)
  abstract AndroidInjector.Factory<? extends View> handleView(HandleViewSubComponent.Builder builder);

  @Binds
  @IntoMap
  @ViewKey(DeckEditorView.class)
  abstract AndroidInjector.Factory<? extends View> deckEditorView(DeckEditorViewSubComponent.Builder builder);

  @Binds
  @IntoMap
  @ViewKey(CreateDeckView.class)
  abstract AndroidInjector.Factory<? extends View> createDeckView(CreateDeckViewSubComponent.Builder builder);

  @Binds
  @IntoMap
  @ServiceKey(MainService.class)
  abstract AndroidInjector.Factory<? extends Service> mainService(MainServiceSubComponent.Builder builder);

  @Binds
  @IntoMap
  @ActivityKey(SettingsActivity.class)
  abstract AndroidInjector.Factory<? extends Activity> settingActivity(SettingsActivitySubComponent.Builder builder);

  @Binds
  @IntoMap
  @ActivityKey(MainActivity.class)
  abstract AndroidInjector.Factory<? extends Activity> mainActivity(MainActivitySubComponent.Builder builder);
}