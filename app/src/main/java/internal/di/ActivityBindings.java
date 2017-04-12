package internal.di;

import net.mbonnin.arcanetracker.MainActivity;
import net.mbonnin.arcanetracker.SettingsActivity;

import dagger.Module;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;

/**
 * Created by williamwebb on 4/10/17.
 */

@Module(subcomponents = {
    ActivityBindings.SettingsActivitySubComponent.class,
    ActivityBindings.MainActivitySubComponent.class
})
public interface ActivityBindings {
  @Subcomponent
  interface SettingsActivitySubComponent extends AndroidInjector<SettingsActivity> {
    @Subcomponent.Builder abstract class Builder extends AndroidInjector.Builder<SettingsActivity> { }
  }

  @Subcomponent
  interface MainActivitySubComponent extends AndroidInjector<MainActivity> {
    @Subcomponent.Builder abstract class Builder extends AndroidInjector.Builder<MainActivity> { }
  }
}