package internal.di;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import net.mbonnin.arcanetracker.ArcaneTrackerApplication;
import net.mbonnin.arcanetracker.CardDb;
import net.mbonnin.arcanetracker.DeckListManager;
import net.mbonnin.arcanetracker.FileTree;
import net.mbonnin.arcanetracker.MainViewCompanion;
import net.mbonnin.arcanetracker.PicassoCardRequestHandler;
import net.mbonnin.arcanetracker.QuitDetector;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Settings;
import net.mbonnin.arcanetracker.SettingsCompanion;
import net.mbonnin.arcanetracker.Toaster;
import net.mbonnin.arcanetracker.ViewManager;
import net.mbonnin.arcanetracker.hsreplay.HSReplay;
import net.mbonnin.arcanetracker.trackobot.Trackobot;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.paperdb.Book;
import io.paperdb.Paper;
import okhttp3.OkHttpClient;
import timber.log.Timber;

/**
 * Created by williamwebb on 4/9/17.
 */

@Module
class ApplicationModule {

  private final ArcaneTrackerApplication mApplication;

  public ApplicationModule(ArcaneTrackerApplication application) {
    mApplication = application;
  }

  @Provides
  @Singleton
  ArcaneTrackerApplication providesArcaneTrackerApplication() {
    return mApplication;
  }

  @Provides
  @Singleton
  Context providesContext() {
    return mApplication;
  }

  @Provides
  @Singleton
  Application providesApplication() {
    return mApplication;
  }

  @Provides
  @Singleton
  CardDb providesCardDb(@Named(CardDb.BOOK) Book book, Settings settings) {
    return new CardDb(book, settings);
  }

  @Provides
  @Singleton
  FileTree fileTree(Context context) {
    return new FileTree(context);
  }

  @Provides
  @Singleton
  Timber.Tree[] providesTimberTrees(FileTree fileTree) {
    return new Timber.Tree[] {
        fileTree,
        new Timber.DebugTree()
    };
  }

  @Provides
  @Singleton
  OkHttpClient okHttpClient() {
    return new OkHttpClient();
  }

  @Provides
  @Singleton
  PicassoCardRequestHandler picassoCardRequestHandler(Context context, OkHttpClient client) {
    return new PicassoCardRequestHandler(context, client, context.getCacheDir());
  }

  @Provides
  @Singleton
  Picasso providePicasso(Context context, OkHttpClient client, PicassoCardRequestHandler cardRequestHandler) {
    Picasso picasso = new Picasso.Builder(context)
        .downloader(new OkHttp3Downloader(client))
        .addRequestHandler(cardRequestHandler)
        .build();
    Picasso.setSingletonInstance(picasso);
    return picasso;
  }

  @Provides
  @Singleton
  HSReplay hsReplay(Settings settings) {
    return new HSReplay(settings);
  }

  @Provides
  @Singleton
  DeckListManager deckList(CardDb cardDb, Book book, Context context) {
    return new DeckListManager(cardDb, book, context.getString(R.string.yourDeck), context.getString(R.string.opponentsDeck));
  }

  @Provides
  @Singleton
  MainViewCompanion mainViewCompanion(Context context, DeckListManager deckListManager, ViewManager viewManager, FileTree fileTree, Toaster toaster, Settings settings, CardDb cardDb, Trackobot trackobot, Book book) {
    View view = LayoutInflater.from(context).inflate(R.layout.main_view, null);
    return new MainViewCompanion(view, deckListManager, viewManager, fileTree, toaster, settings, cardDb, trackobot, book);
  }

  @Provides
  @Singleton
  SettingsCompanion settingsCompanion(Context context, MainViewCompanion mainViewCompanion, ViewManager viewManager, FileTree fileTree, Toaster toaster, Settings settings, Trackobot trackobot) {
    View view = LayoutInflater.from(context).inflate(R.layout.settings_view, null);
    return new SettingsCompanion(view, mainViewCompanion, viewManager, fileTree, toaster, settings, trackobot);
  }

  @Singleton
  @Provides
  ViewManager viewManager(Context context) {
    return new ViewManager(context);
  }

  @Singleton
  @Provides
  QuitDetector quitDetector(MainViewCompanion mainViewCompanion, ViewManager viewManager, Settings settings) {
    return new QuitDetector(mainViewCompanion, viewManager, settings);
  }

  @Singleton
  @Provides
  Toaster toaster(Context context) {
    return new Toaster(context);
  }

  @Singleton
  @Provides
  Settings settings(Context context) {
    return new Settings(context);
  }

  @Singleton
  @Provides
  Trackobot trackobot(Context context, Book book) {
    return new Trackobot(context, book);
  }

  @SuppressLint("MissingPermission")
  @Provides
  @Singleton
  FirebaseAnalytics firebaseAnalytics(Context context) {
    return FirebaseAnalytics.getInstance(context);
  }

  @Provides
  @Singleton
  Book book(Context context) {
    Paper.init(context);
    return Paper.book();
  }

  @Provides
  @Singleton
  @Named(CardDb.BOOK)
  Book cardDbBook(Context context) {
    Paper.init(context);
    return Paper.book(CardDb.BOOK);
  }
}
