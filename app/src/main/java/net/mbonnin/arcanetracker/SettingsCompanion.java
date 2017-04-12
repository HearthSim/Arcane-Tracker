package net.mbonnin.arcanetracker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.crash.FirebaseCrash;

import net.mbonnin.arcanetracker.trackobot.model.HistoryList;
import net.mbonnin.arcanetracker.trackobot.Trackobot;
import net.mbonnin.arcanetracker.trackobot.Url;
import net.mbonnin.arcanetracker.trackobot.User;

import java.io.File;

import javax.inject.Inject;

import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by martin on 10/24/16.
 */

public class SettingsCompanion {
    public final View settingsView;
    private final Toaster toaster;
    private final ViewManager viewManager;
    private Button signinButton;
    private Button signupButton;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private ProgressBar signupProgressBar;
    private ProgressBar signinProgressBar;
    private Button importButton;
    private ProgressBar importProgressBar;
    private Settings settings;
    private final Trackobot trackobot;

    private void handleSigninButtonClick() {
        signinButton.setVisibility(GONE);
        signinProgressBar.setVisibility(VISIBLE);
        signupButton.setEnabled(false);

        User user = new User();
        user.username = usernameEditText.getText().toString();
        user.password = passwordEditText.getText().toString();
        trackobot.setUser(user);

        trackobot.service().getHistoryList()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(historyList -> {
                signinProgressBar.setVisibility(GONE);
                signinButton.setVisibility(VISIBLE);
                signupButton.setEnabled(true);

                if (historyList == null) {
                    toaster.toast(R.string.cannotLinkTrackobot, Toast.LENGTH_LONG);
                    trackobot.setUser(null);
                } else {
                    updateTrackobot(settingsView);
                }
            });
    }

    private void handleSignupButtonClick() {
        signupButton.setVisibility(GONE);
        signupProgressBar.setVisibility(VISIBLE);
        signinButton.setEnabled(false);

        trackobot.service().createUser()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(user -> {
                signupProgressBar.setVisibility(GONE);
                signupButton.setVisibility(VISIBLE);
                signinButton.setEnabled(true);
                if (user == null) {
                    toaster.toast("Cannot create trackobot account :(", Toast.LENGTH_LONG);
                } else {
                    trackobot.setUser(user);
                    updateTrackobot(settingsView);
                }
            });
    };

    private void handleImportButtonClick() {
        File f = Trackobot.findTrackobotFile();
        if (f == null) {
            toaster.toast(R.string.couldNotFindTrackobotFile, Toast.LENGTH_LONG);
            return;
        }

        User user = Trackobot.parseTrackobotFile(f);
        if (user == null) {
            toaster.toast(R.string.couldNotOpenTrackobotFile, Toast.LENGTH_LONG);
            return;
        }
        importButton.setVisibility(GONE);
        importProgressBar.setVisibility(VISIBLE);
        importButton.setEnabled(false);

        trackobot.setUser(user);

        trackobot.service().getHistoryList()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(historyList -> {
                importProgressBar.setVisibility(GONE);
                importButton.setVisibility(VISIBLE);
                importButton.setEnabled(true);

                if (historyList == null) {
                    toaster.toast(R.string.cannotLinkTrackobot, Toast.LENGTH_LONG);
                    trackobot.setUser(null);
                } else {
                    updateTrackobot(settingsView);
                }
            }, e -> {
                e.printStackTrace();
                importProgressBar.setVisibility(GONE);
                importButton.setVisibility(VISIBLE);
                importButton.setEnabled(true);

                toaster.toast(R.string.cannotLinkTrackobot, Toast.LENGTH_LONG);
            });
    }

    private void updateTrackobot(View view) {
        signupButton = (Button) view.findViewById(R.id.trackobotSignup);
        signinButton = (Button) view.findViewById(R.id.trackobotSignin);
        TextView trackobotText = ((TextView) (view.findViewById(R.id.trackobotText)));
        passwordEditText = (EditText) view.findViewById(R.id.password);
        usernameEditText = (EditText) view.findViewById(R.id.username);
        signinProgressBar = (ProgressBar) view.findViewById(R.id.signinProgressBar);
        signupProgressBar = (ProgressBar) view.findViewById(R.id.signupProgressBar);
        View retrievePassword = view.findViewById(R.id.retrievePassword);
        importButton = (Button) view.findViewById(R.id.trackobotImport);
        importProgressBar = (ProgressBar)view.findViewById(R.id.importProgressBar);
        View importExplanation = view.findViewById(R.id.importExplanation);

        User user = trackobot.getUser();
        if (user == null) {
            trackobotText.setText(view.getContext().getString(R.string.trackobotExplanation));
            view.findViewById(R.id.or).setVisibility(VISIBLE);

            usernameEditText.setEnabled(true);
            passwordEditText.setEnabled(true);

            signinButton.setText(view.getContext().getString(R.string.linkAccount));
            signinButton.setOnClickListener(v -> handleSigninButtonClick());

            signupButton.setText(view.getContext().getString(R.string.createAccount));
            signupButton.setOnClickListener(v -> handleSignupButtonClick());

            retrievePassword.setVisibility(VISIBLE);

            importButton.setText(view.getContext().getString(R.string.importFromStorage));
            importButton.setOnClickListener(v -> handleImportButtonClick());
            importButton.setEnabled(true);
            importButton.setVisibility(VISIBLE);
            view.findViewById(R.id.or2).setVisibility(VISIBLE);
            importExplanation.setVisibility(VISIBLE);

        } else {
            trackobotText.setVisibility(GONE);
            view.findViewById(R.id.or).setVisibility(GONE);

            usernameEditText.setText(user.username);
            passwordEditText.setText(user.password);
            usernameEditText.setEnabled(false);
            passwordEditText.setEnabled(false);

            signinButton.setText(view.getContext().getString(R.string.unlinkAccount));
            signinButton.setOnClickListener(v -> {
                trackobot.setUser(null);
                usernameEditText.setText("");
                passwordEditText.setText("");
                updateTrackobot(settingsView);
            });

            signupButton.setText(view.getContext().getString(R.string.openInBrowser));
            signupButton.setOnClickListener(v -> {
                signupProgressBar.setVisibility(VISIBLE);
                signupButton.setVisibility(GONE);

                trackobot.service().createOneTimeAuth()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(url -> {
                            viewManager.removeView(settingsView);

                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url.url));
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            view.getContext().startActivity(i);
                        }, e -> {
                            toaster.toast(R.string.couldNotGetProfile, Toast.LENGTH_LONG);
                            signupButton.setVisibility(VISIBLE);
                            signupProgressBar.setVisibility(GONE);
                            Timber.e(e);
                        });
            });

            retrievePassword.setVisibility(GONE);

            importExplanation.setVisibility(GONE);
            importButton.setVisibility(GONE);
            view.findViewById(R.id.or2).setVisibility(GONE);

        }
    }

    public SettingsCompanion(View view, MainViewCompanion mainViewCompanion, ViewManager viewManager, FileTree tree, Toaster toaster, Settings settings, Trackobot trackobot) {
        this.settingsView = view;
        this.toaster = toaster;
        this.viewManager = viewManager;
        this.settings = settings;
        this.trackobot = trackobot;

        updateTrackobot(view);

        TextView appVersion = (TextView) view.findViewById(R.id.appVersion);
        appVersion.setText(view.getContext().getString(R.string.thisIsArcaneTracker, BuildConfig.VERSION_NAME, Utils.isAppDebuggable() ? " (debug)":""));

        Button feedbackButton = (Button)view.findViewById(R.id.feedBackButton);
        feedbackButton.setOnClickListener(v -> {
            viewManager.removeView(settingsView);

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"support@arcanetracker.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Arcane Tracker Feedback");
            emailIntent.putExtra(Intent.EXTRA_TEXT, view.getContext().getString(R.string.decribeYourProblem));

            tree.sync();
            Uri uri = FileProvider.getUriForFile(view.getContext(), "net.mbonnin.arcanetracker.fileprovider", tree.getFile());
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);

            feedbackButton.getContext().startActivity(emailIntent);
        });
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        seekBar.setMax(100);
        seekBar.setProgress(mainViewCompanion.getAlphaSetting());
        SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mainViewCompanion.setAlphaSetting(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        };
        seekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

        seekBar = (SeekBar) view.findViewById(R.id.drawerSizeBar);
        seekBar.setMax(mainViewCompanion.getMaxDrawerWidth() - mainViewCompanion.getMinDrawerWidth());
        seekBar.setProgress(mainViewCompanion.getDrawerWidth() - mainViewCompanion.getMinDrawerWidth());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mainViewCompanion.setDrawerWidth(progress + mainViewCompanion.getMinDrawerWidth());
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {  }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {  }
        });

        seekBar = (SeekBar) view.findViewById(R.id.buttonSizeBar);
        seekBar.setMax(mainViewCompanion.getMaxButtonWidth() - mainViewCompanion.getMinButtonWidth());
        seekBar.setProgress(mainViewCompanion.getButtonWidth() - mainViewCompanion.getMinButtonWidth());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mainViewCompanion.setButtonWidth(progress + mainViewCompanion.getMinButtonWidth());
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        CheckBox autoQuit = (CheckBox) view.findViewById(R.id.autoQuit);
        autoQuit.setChecked(settings.get(Settings.AUTO_QUIT, true));
        autoQuit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.set(Settings.AUTO_QUIT, isChecked);
        });

        CheckBox autoSelectDeck = (CheckBox) view.findViewById(R.id.autoSelectDeck);
        autoSelectDeck.setChecked(settings.get(Settings.AUTO_SELECT_DECK, true));
        autoSelectDeck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.set(Settings.AUTO_SELECT_DECK, isChecked);
        });

        CheckBox autoAddCards = (CheckBox) view.findViewById(R.id.autoAddCards);
        autoAddCards.setChecked(settings.get(Settings.AUTO_ADD_CARDS, true));
        autoAddCards.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settings.set(Settings.AUTO_ADD_CARDS, isChecked);
        });

        view.findViewById(R.id.quit).setOnClickListener(v -> MainService.stop(view.getContext()));
    }

    public static void show(Context context, MainViewCompanion mainViewCompanion) {
        View view2 = LayoutInflater.from(context).inflate(R.layout.settings_view, null);

        ViewManager viewManager = mainViewCompanion.getViewManager();
        FileTree fileTree = mainViewCompanion.getTree();
        Toaster toaster = mainViewCompanion.getToaster();
        Settings settings = mainViewCompanion.getSettings();
        Trackobot trackobot = mainViewCompanion.getTrackobot();

        // TODO: why???
        new SettingsCompanion(view2, mainViewCompanion, viewManager, fileTree, toaster, settings, trackobot);

        ViewManager.Params params = new ViewManager.Params();
        params.x = viewManager.getWidth() / 4;
        params.y = viewManager.getHeight() / 16;
        params.w = viewManager.getWidth() / 2;
        params.h = 7 * viewManager.getHeight() / 8;

        viewManager.addModalAndFocusableView(view2, params);
    }
}
