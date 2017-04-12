package internal.di;

import net.mbonnin.arcanetracker.MainService;

import dagger.Module;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;

/**
 * Created by williamwebb on 4/10/17.
 */

@Module(subcomponents = {ServiceBindings.MainServiceSubComponent.class})
public interface ServiceBindings {
  @Subcomponent
  interface MainServiceSubComponent extends AndroidInjector<MainService> {
    @Subcomponent.Builder abstract class Builder extends AndroidInjector.Builder<MainService> { }
  }
}