package internal.di;

import com.google.firebase.analytics.FirebaseAnalytics;

import net.mbonnin.arcanetracker.CardDb;
import net.mbonnin.arcanetracker.DeckListManager;
import net.mbonnin.arcanetracker.MainViewCompanion;
import net.mbonnin.arcanetracker.ParserListenerArena;
import net.mbonnin.arcanetracker.ParserListenerLoadingScreen;
import net.mbonnin.arcanetracker.ParserListenerPower;
import net.mbonnin.arcanetracker.Settings;
import net.mbonnin.arcanetracker.hsreplay.HSReplay;
import net.mbonnin.arcanetracker.parser.ArenaParser;
import net.mbonnin.arcanetracker.parser.LoadingScreenParser;
import net.mbonnin.arcanetracker.parser.PowerParser;
import net.mbonnin.arcanetracker.parser.RawGameParser;
import net.mbonnin.arcanetracker.trackobot.Trackobot;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by williamwebb on 4/9/17.
 */

@Module
public class ParserModule {

  @Provides
  @Singleton
  ParserListenerLoadingScreen parserListenerLoadingScreen(MainViewCompanion mainViewCompanion, DeckListManager deckListManager) {
    return new ParserListenerLoadingScreen(mainViewCompanion, deckListManager);
  }

  @Provides
  @Singleton
  ArenaParser arenaParser(MainViewCompanion mainViewCompanion, DeckListManager deckListManager, CardDb cardDb) {
    return new ArenaParser(new ParserListenerArena(mainViewCompanion, deckListManager, cardDb));
  }

  @Provides
  @Singleton
  LoadingScreenParser loadingScreenParser(ParserListenerLoadingScreen parserListenerLoadingScreen) {
    return new LoadingScreenParser(parserListenerLoadingScreen);
  }

  @Provides
  @Singleton
  ParserListenerPower parserListenerPower(ParserListenerLoadingScreen parserListenerLoadingScreen, MainViewCompanion mainViewCompanion, DeckListManager deckListManager, Settings settings, Trackobot trackobot, FirebaseAnalytics firebaseAnalytics) {
    return new ParserListenerPower(parserListenerLoadingScreen, mainViewCompanion, deckListManager, settings, trackobot, firebaseAnalytics);
  }

  @Provides
  @Singleton
  PowerParser powerParser(CardDb cardDb, ParserListenerPower parserListenerPower) {
    return new PowerParser(cardDb, parserListenerPower);
  }

  @Provides
  @Singleton
  RawGameParser rawGameParser(HSReplay hsReplay, ParserListenerPower parserListenerPower) {
    return new RawGameParser(hsReplay, parserListenerPower);
  }

}
