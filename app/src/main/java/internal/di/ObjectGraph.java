package internal.di;

import net.mbonnin.arcanetracker.ArcaneTrackerApplication;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by williamwebb on 4/9/17.
 */
@Singleton
@Component(modules = {
    ApplicationModule.class,
    ParserModule.class,
    BuilderModule.class
})
public interface ObjectGraph {

  final class Initializer {
    public static ObjectGraph init(ArcaneTrackerApplication app) {
      return DaggerObjectGraph.builder()
          .applicationModule(new ApplicationModule(app))
          .parserModule(new ParserModule())
          .build();
    }

    private Initializer() { } // No instances.
  }

  void inject(ArcaneTrackerApplication app);
}