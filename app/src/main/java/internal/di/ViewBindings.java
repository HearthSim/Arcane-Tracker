package internal.di;

import net.mbonnin.arcanetracker.CreateDeckView;
import net.mbonnin.arcanetracker.DeckEditorView;
import net.mbonnin.arcanetracker.HandleView;
import net.mbonnin.arcanetracker.HandlesView;

import dagger.Module;
import dagger.Subcomponent;
import dagger.android.AndroidInjector;

/**
 * Created by williamwebb on 4/9/17.
 */
@Module(subcomponents = {
    ViewBindings.HandlesViewSubComponent.class,
    ViewBindings.HandleViewSubComponent.class,
    ViewBindings.DeckEditorViewSubComponent.class,
    ViewBindings.CreateDeckViewSubComponent.class
})
public interface ViewBindings {
  @Subcomponent
  interface HandlesViewSubComponent extends AndroidInjector<HandlesView> {
    @Subcomponent.Builder abstract class Builder extends AndroidInjector.Builder<HandlesView> { }
  }

  @Subcomponent
  interface HandleViewSubComponent extends AndroidInjector<HandleView> {
    @Subcomponent.Builder abstract class Builder extends AndroidInjector.Builder<HandleView> { }
  }

  @Subcomponent
  interface DeckEditorViewSubComponent extends AndroidInjector<DeckEditorView> {
    @Subcomponent.Builder abstract class Builder extends AndroidInjector.Builder<DeckEditorView> { }
  }

  @Subcomponent
  interface CreateDeckViewSubComponent extends AndroidInjector<CreateDeckView> {
    @Subcomponent.Builder abstract class Builder extends AndroidInjector.Builder<CreateDeckView> { }
  }
}