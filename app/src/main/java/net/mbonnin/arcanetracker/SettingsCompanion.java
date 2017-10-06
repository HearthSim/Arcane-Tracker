package net.mbonnin.arcanetracker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

import net.mbonnin.arcanetracker.hsreplay.HSReplay;
import net.mbonnin.arcanetracker.trackobot.Trackobot;
import net.mbonnin.arcanetracker.trackobot.Url;
import net.mbonnin.arcanetracker.trackobot.User;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import rx.Completable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by martin on 10/24/16.
 */

public class SettingsCompanion {
    private LoadableButtonCompanion mHsReplayCompanion1;
    View settingsView;
    private TextView trackobotText;
    private Button signinButton;
    private Button signupButton;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private ProgressBar signupProgressBar;
    private ProgressBar signinProgressBar;
    private View retrievePassword;
    private Button importButton;
    private View importExplanation;
    private ProgressBar importProgressBar;
    private boolean firstTime;
    private LoadableButtonCompanion mHsReplayCompanion2;

    private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            MainViewCompanion.get().setAlphaSetting(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    Observer<User> mSignupObserver = new Observer<User>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            Timber.e(e);
        }

        @Override
        public void onNext(User user) {
        }
    };

    private View.OnClickListener mSigninButtonClicked = v -> {
        User user = new User();
        user.username = usernameEditText.getText().toString();
        user.password = passwordEditText.getText().toString();

        Trackobot.get().testUser(user)
                .subscribe(lce -> {
                    if (lce.isLoading()) {
                        signinButton.setVisibility(GONE);
                        signinProgressBar.setVisibility(VISIBLE);
                        signupButton.setEnabled(false);
                    } else {
                        signinProgressBar.setVisibility(GONE);
                        signinButton.setVisibility(VISIBLE);
                        signupButton.setEnabled(true);
                        if (lce.getError() != null) {
                            Toast.makeText(ArcaneTrackerApplication.getContext(), ArcaneTrackerApplication.getContext().getString(R.string.cannotLinkTrackobot), Toast.LENGTH_LONG).show();
                            Utils.reportNonFatal(lce.getError());
                        } else {
                            FirebaseAnalytics.getInstance(ArcaneTrackerApplication.getContext()).logEvent("track_o_bot_signin", null);
                            updateTrackobot(settingsView);
                        }
                    }
                });
    };

    private View.OnClickListener mSignupButtonClicked = v -> {
        Trackobot.get().createUser()
                .subscribe(lce -> {
                    if (lce.isLoading()) {
                        signupButton.setVisibility(GONE);
                        signupProgressBar.setVisibility(VISIBLE);
                        signinButton.setEnabled(false);
                    } else {
                        signupProgressBar.setVisibility(GONE);
                        signupButton.setVisibility(VISIBLE);
                        signinButton.setEnabled(true);

                        Context context = ArcaneTrackerApplication.getContext();
                        if (lce.getError() != null) {
                            Toast.makeText(context, context.getString(R.string.trackobotSignupError), Toast.LENGTH_LONG).show();
                            Utils.reportNonFatal(lce.getError());
                        } else {
                            Trackobot.get().setUser(lce.getData());

                            FirebaseAnalytics.getInstance(context).logEvent("track_o_bot_signup", null);
                            updateTrackobot(settingsView);
                        }
                    }
                });
    };

    private Observer<? super Url> mOneTimeAuthObserver = new Observer<Url>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            Context context = ArcaneTrackerApplication.getContext();
            Toast.makeText(context, context.getString(R.string.couldNotGetProfile), Toast.LENGTH_LONG).show();
            signupButton.setVisibility(VISIBLE);
            signupProgressBar.setVisibility(GONE);
            Timber.e(e);
        }

        @Override
        public void onNext(Url url) {
            ViewManager.get().removeView(settingsView);

            Utils.openLink(url.url);
        }
    };

    private View.OnClickListener mImportButtonClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Context context = ArcaneTrackerApplication.getContext();
            File f = Trackobot.findTrackobotFile();
            if (f == null) {
                Toast.makeText(context, context.getString(R.string.couldNotFindTrackobotFile), Toast.LENGTH_LONG).show();
                return;
            }

            User user = Trackobot.parseTrackobotFile(f);
            if (user == null) {
                Toast.makeText(context, context.getString(R.string.couldNotOpenTrackobotFile), Toast.LENGTH_LONG).show();
                return;
            }

            Trackobot.get().testUser(user)
                    .subscribe(lce -> {
                        if (lce.isLoading()) {
                            importButton.setVisibility(GONE);
                            importProgressBar.setVisibility(VISIBLE);
                            importButton.setEnabled(false);
                        } else {
                            importProgressBar.setVisibility(GONE);
                            importButton.setVisibility(VISIBLE);
                            importButton.setEnabled(true);

                            if (lce.getError() != null) {
                                Toast.makeText(ArcaneTrackerApplication.getContext(), ArcaneTrackerApplication.getContext().getString(R.string.cannotLinkTrackobot), Toast.LENGTH_LONG).show();
                                Utils.reportNonFatal(lce.getError());
                            } else {
                                FirebaseAnalytics.getInstance(ArcaneTrackerApplication.getContext()).logEvent("track_o_bot_import", null);
                                updateTrackobot(settingsView);
                            }
                        }
                    });
        }
    };


    private void updateTrackobot(View view) {
        signupButton = (Button) view.findViewById(R.id.trackobotSignup);
        signinButton = (Button) view.findViewById(R.id.trackobotSignin);
        trackobotText = ((TextView) (view.findViewById(R.id.trackobotText)));
        passwordEditText = (EditText) view.findViewById(R.id.password);
        usernameEditText = (EditText) view.findViewById(R.id.username);
        signinProgressBar = (ProgressBar) view.findViewById(R.id.signinProgressBar);
        signupProgressBar = (ProgressBar) view.findViewById(R.id.signupProgressBar);
        retrievePassword = view.findViewById(R.id.retrievePassword);
        importButton = (Button) view.findViewById(R.id.trackobotImport);
        importProgressBar = (ProgressBar) view.findViewById(R.id.importProgressBar);
        importExplanation = view.findViewById(R.id.importExplanation);

        User user = Trackobot.get().getUser();
        if (user == null) {
            trackobotText.setText(view.getContext().getString(R.string.trackobotExplanation));
            view.findViewById(R.id.or).setVisibility(VISIBLE);

            usernameEditText.setEnabled(true);
            passwordEditText.setEnabled(true);

            signinButton.setText(view.getContext().getString(R.string.linkAccount));
            signinButton.setOnClickListener(mSigninButtonClicked);

            signupButton.setText(view.getContext().getString(R.string.createAccount));
            signupButton.setOnClickListener(mSignupButtonClicked);

            retrievePassword.setVisibility(VISIBLE);

            importButton.setText(view.getContext().getString(R.string.importFromStorage));
            importButton.setOnClickListener(mImportButtonClicked);
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
                Trackobot.get().setUser(null);
                usernameEditText.setText("");
                passwordEditText.setText("");
                updateTrackobot(settingsView);
            });

            signupButton.setText(view.getContext().getString(R.string.openInBrowser));
            signupButton.setOnClickListener(v -> {
                signupProgressBar.setVisibility(VISIBLE);
                signupButton.setVisibility(GONE);

                Trackobot.get().service().createOneTimeAuth()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(mOneTimeAuthObserver);
            });

            retrievePassword.setVisibility(GONE);

            importExplanation.setVisibility(GONE);
            importButton.setVisibility(GONE);
            view.findViewById(R.id.or2).setVisibility(GONE);

        }
    }

    public SettingsCompanion(View view) {
        settingsView = view;
        init();
    }

    private void init() {
        View view = settingsView;
        Context context = ArcaneTrackerApplication.getContext();

        updateTrackobot(view);

        TextView appVersion = view.findViewById(R.id.appVersion);
        appVersion.setText(view.getContext().getString(R.string.thisIsArcaneTracker, BuildConfig.VERSION_NAME, Utils.isAppDebuggable() ? " (debug)" : ""));

        Button feedbackButton = view.findViewById(R.id.feedBackButton);
        feedbackButton.setOnClickListener(v -> {
            ViewManager.get().removeView(settingsView);


            ArrayList<Uri> arrayUri = new ArrayList<>();
            FileTree.get().sync();
            arrayUri.add(FileProvider.getUriForFile(view.getContext(), "net.mbonnin.arcanetracker.fileprovider", FileTree.get().getFile()));

            Completable completable;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && ScreenCapture.Companion.get() != null) {
                /* 1s is hopefully enough for the settings view to disappear */
                Toast.makeText(view.getContext(), Utils.getString(R.string.preparingEmail), Toast.LENGTH_SHORT).show();

                completable = Completable.timer(1, TimeUnit.SECONDS)
                        .andThen(ScreenCapture.Companion.get().screenShotSingle())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(file -> arrayUri.add(FileProvider.getUriForFile(view.getContext(), "net.mbonnin.arcanetracker.fileprovider", file)))
                        .toCompletable();
            } else {
                completable = Completable.complete();
            }

            completable.observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                        emailIntent.setType("text/plain");
                        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@arcanetracker.com"});
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Arcane Tracker Feedback");
                        emailIntent.putExtra(Intent.EXTRA_TEXT, view.getContext().getString(R.string.decribeYourProblem) + "\n\n");
                        emailIntent.putExtra(Intent.EXTRA_STREAM, arrayUri);

                        try {
                            ArcaneTrackerApplication.getContext().startActivity(emailIntent);
                        } catch (Exception e) {
                            Utils.reportNonFatal(e);
                            Toast.makeText(ArcaneTrackerApplication.getContext(), Utils.getString(R.string.noEmailFound), Toast.LENGTH_LONG).show();
                        }

                    });
        });

        Button resetCacheButton = view.findViewById(R.id.resetCache);
        resetCacheButton.setOnClickListener(v -> {
            PicassoCardRequestHandler.get().resetCache();
        });

        SeekBar seekBar = view.findViewById(R.id.seekBar);
        seekBar.setMax(100);
        seekBar.setProgress(MainViewCompanion.get().getAlphaSetting());
        seekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);

        MainViewCompanion c = MainViewCompanion.get();
        seekBar = (SeekBar) view.findViewById(R.id.drawerSizeBar);
        seekBar.setMax(MainViewCompanion.get().getMaxDrawerWidth() - MainViewCompanion.get().getMinDrawerWidth());
        seekBar.setProgress(MainViewCompanion.get().getDrawerWidth() - MainViewCompanion.get().getMinDrawerWidth());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MainViewCompanion.get().setDrawerWidth(progress + MainViewCompanion.get().getMinDrawerWidth());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        Spinner spinner = (Spinner) view.findViewById(R.id.languageSpinner);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        int selectedPosition = 0;
        int i = 1;
        String l = Settings.get(Settings.LANGUAGE, null);
        adapter.add(context.getString(R.string._default));
        for (Language language : Language.allLanguages) {
            adapter.add(language.friendlyName);
            if (l != null && language.key.equals(l)) {
                selectedPosition = i;
            }
            i++;
        }

        spinner.setAdapter(adapter);
        spinner.setSelection(selectedPosition);
        firstTime = true;
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newKey = position == 0 ? null : Language.allLanguages.get(position - 1).key;
                String oldKey = Settings.get(Settings.LANGUAGE, null);
                boolean areSame = newKey == null ? oldKey == null : newKey.equals(oldKey);
                if (!firstTime && !areSame) {
                    Settings.set(Settings.LANGUAGE, newKey);

                    showRestartDialog();
                }
                firstTime = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        seekBar = (SeekBar) view.findViewById(R.id.buttonSizeBar);
        seekBar.setMax(c.getMaxButtonWidth() - c.getMinButtonWidth());
        seekBar.setProgress(c.getButtonWidth() - c.getMinButtonWidth());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                c.setButtonWidth(progress + c.getMinButtonWidth());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        CheckBox screenCapture = (CheckBox) view.findViewById(R.id.screenCaptureCheckBox);
        screenCapture.setChecked(Settings.get(Settings.SCREEN_CAPTURE_ENABLED, true));
        screenCapture.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Settings.set(Settings.SCREEN_CAPTURE_ENABLED, isChecked);
            showRestartDialog();
        });

        CheckBox autoQuit = (CheckBox) view.findViewById(R.id.autoQuit);
        autoQuit.setChecked(Settings.get(Settings.AUTO_QUIT, true));
        autoQuit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Settings.set(Settings.AUTO_QUIT, isChecked);
        });

        CheckBox autoSelectDeck = (CheckBox) view.findViewById(R.id.autoSelectDeck);
        autoSelectDeck.setChecked(Settings.get(Settings.AUTO_SELECT_DECK, true));
        autoSelectDeck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Settings.set(Settings.AUTO_SELECT_DECK, isChecked);
        });

        CheckBox autoAddCards = (CheckBox) view.findViewById(R.id.autoAddCards);
        autoAddCards.setChecked(Settings.get(Settings.AUTO_ADD_CARDS, true));
        autoAddCards.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Settings.set(Settings.AUTO_ADD_CARDS, isChecked);
        });

        View hsReplay1 = view.findViewById(R.id.hsReplayButton1);
        View hsReplay2 = view.findViewById(R.id.hsReplayButton2);

        mHsReplayCompanion1 = new LoadableButtonCompanion(hsReplay1);
        mHsReplayCompanion2 = new LoadableButtonCompanion(hsReplay2);

        mHsReplayState = new HsReplayState();
        mHsReplayState.token = HSReplay.get().token();
        if (mHsReplayState.token != null) {
            checkUserName();
        }
        updateHsReplay();
    }

    private void showRestartDialog() {
        View view2 = LayoutInflater.from(settingsView.getContext()).inflate(R.layout.please_restart, null);
        view2.findViewById(R.id.ok).setOnClickListener(v3 -> {
            ViewManager.get().removeView(view2);
        });

        ViewManager.Params params = new ViewManager.Params();
        params.w = (int) (ViewManager.get().getWidth() * 0.6f);
        params.h = ViewManager.get().getHeight() / 2;
        params.x = (ViewManager.get().getWidth() - params.w) / 2;
        params.y = ViewManager.get().getHeight() / 4;

        ViewManager.get().addModalView(view2, params);
    }

    private void checkUserName() {
        HSReplay.get().getUser()
                .subscribe(lce -> {
                    if (lce.isLoading()) {
                        mHsReplayState.userNameLoading = true;
                    } else if (lce.getData() != null) {
                        mHsReplayState.userNameLoading = false;
                        if (lce.getData().user != null && lce.getData().user.username != null) {
                            mHsReplayState.userName = lce.getData().user.username;
                        }
                    } else if (lce.getError() != null) {
                        mHsReplayState.userNameLoading = false;
                        Utils.reportNonFatal(new Exception("HsReplay username error", lce.getError()));
                        Toast.makeText(settingsView.getContext(), Utils.getString(R.string.hsReplayUsernameError), Toast.LENGTH_LONG).show();
                    }
                    updateHsReplay();
                });
    }

    static class HsReplayState {
        public String token;
        public boolean tokenLoading;
        public String userName;
        public boolean userNameLoading;
        public boolean claimUrlLoading;
    }

    HsReplayState mHsReplayState;

    private void handleTokenLce(Lce<String> lce) {
        if (lce.isLoading()) {
            mHsReplayState.tokenLoading = true;
        } else if (lce.getError() != null) {
            mHsReplayState.tokenLoading = false;
            Utils.reportNonFatal(new Exception("HsReplay token error", lce.getError()));
            Toast.makeText(settingsView.getContext(), Utils.getString(R.string.hsReplayTokenError), Toast.LENGTH_LONG).show();
        } else {
            mHsReplayState.tokenLoading = false;
            mHsReplayState.token = lce.getData();
        }
        updateHsReplay();

    }

    private void updateHsReplay() {

        TextView hsReplayDescription = (TextView) settingsView.findViewById(R.id.hsReplayDescription);

        /*
         * state of the description
         */
        if (mHsReplayState.token == null) {
            hsReplayDescription.setText(Utils.getString(R.string.hsReplayExplanation));
        } else {
            if (mHsReplayState.userName == null) {
                hsReplayDescription.setText(Utils.getString(R.string.hsReplayExplanationNoUserName));
            } else {
                hsReplayDescription.setText(Utils.getString(R.string.hsReplayExplanationWithUserName, mHsReplayState.userName));
            }
        }

        /*
         * state of the 2nd button
         * we do this one first because this is the enable/disable one (most important goes last)
         */
        if (mHsReplayState.token == null) {
            if (mHsReplayState.tokenLoading) {
                mHsReplayCompanion2.setLoading();
            } else {
                mHsReplayCompanion2.setText(Utils.getString(R.string.hsReplayEnable), v -> {
                    HSReplay.get()
                            .createToken()
                            .subscribe(this::handleTokenLce);
                });
            }
        } else {
            mHsReplayCompanion2.setText(Utils.getString(R.string.hsReplayDisable), v -> {
                mHsReplayState.userName = null;
                mHsReplayState.token = null;
                HSReplay.get().unlink();
                updateHsReplay();
            });
        }

        /*
         * state of the 1st button
         */
        if (mHsReplayState.token == null) {
            mHsReplayCompanion1.view().setVisibility(GONE);
        } else {
            mHsReplayCompanion1.view().setVisibility(VISIBLE);
            if (mHsReplayState.userNameLoading || mHsReplayState.claimUrlLoading) {
                mHsReplayCompanion1.setLoading();
            } else if (mHsReplayState.userName != null) {
                mHsReplayCompanion1.setText(Utils.getString(R.string.openInBrowser), v -> {
                    ViewManager.get().removeView(settingsView);

                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("https://hsreplay.net/games/mine/"));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ArcaneTrackerApplication.getContext().startActivity(i);
                });
            } else {
                mHsReplayCompanion1.setText(Utils.getString(R.string.hsReplayClaim), v -> {
                    HSReplay.get()
                            .getClaimUrl()
                            .subscribe(this::handleClaimUrlLce);
                });

            }
        }
    }

    private void handleClaimUrlLce(Lce<String> lce) {
        if (lce.isLoading()) {
            mHsReplayState.claimUrlLoading = true;
        } else if (lce.getError() != null) {
            mHsReplayState.claimUrlLoading = false;
            Toast.makeText(ArcaneTrackerApplication.getContext(), Utils.getString(R.string.hsReplayClaimFailed), Toast.LENGTH_LONG).show();
            Utils.reportNonFatal(new Exception("HSReplay claim url", lce.getError()));
        } else if (lce.getData() != null) {
            mHsReplayState.claimUrlLoading = false;

            ViewManager.get().removeView(settingsView);

            Utils.openLink(lce.getData());
        }

        updateHsReplay();
    }

    public static void show() {
        Context context = ArcaneTrackerApplication.getContext();
        ViewManager viewManager = ViewManager.get();
        View view2 = LayoutInflater.from(context).inflate(R.layout.settings_view, null);

        new SettingsCompanion(view2);

        ViewManager.Params params = new ViewManager.Params();
        params.x = (int) (viewManager.getWidth() * 0.15);
        params.y = viewManager.getHeight() / 16;
        params.w = (int) (viewManager.getWidth() * 0.70);
        params.h = 7 * viewManager.getHeight() / 8;

        viewManager.addModalAndFocusableView(view2, params);
    }
}
