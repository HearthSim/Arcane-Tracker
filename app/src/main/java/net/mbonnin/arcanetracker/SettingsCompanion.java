package net.mbonnin.arcanetracker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.support.v4.graphics.drawable.DrawableCompat;
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
import net.mbonnin.arcanetracker.hsreplay.model.ClaimResult;
import net.mbonnin.arcanetracker.hsreplay.model.Token;
import net.mbonnin.arcanetracker.trackobot.Trackobot;
import net.mbonnin.arcanetracker.trackobot.Url;
import net.mbonnin.arcanetracker.trackobot.User;
import net.mbonnin.arcanetracker.trackobot.model.HistoryList;

import java.io.File;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by martin on 10/24/16.
 */

public class SettingsCompanion {
    private LoadableButtonCompanion mClaimViewCompanion;
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

    private final Observer<Token> mHSReplayTokenObserver = new Observer<Token>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            Timber.e(e);
        }

        @Override
        public void onNext(Token token) {
            if (token.user != null) {
                mClaimViewCompanion.button.setText(settingsView.getContext().getString(R.string.hsReplayClaimed, token.user.username));
            } else {
                mClaimViewCompanion.button.setText(settingsView.getContext().getString(R.string.hsReplayClaim));
                mClaimViewCompanion.button.setEnabled(true);
                DrawableCompat.setTint(mClaimViewCompanion.button.getBackground(), settingsView.getContext().getResources().getColor(R.color.colorPrimary));
                mClaimViewCompanion.button.setOnClickListener(v -> {
                    mClaimViewCompanion.startLoading(HSReplay.get().service().createClaim(), mHSReplayClaimObserver);
                });
            }
        }
    };

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

    private final Observer<ClaimResult> mHSReplayClaimObserver = new Observer<ClaimResult>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            Timber.e("claim error", e);
            Toast.makeText(ArcaneTrackerApplication.getContext(), ArcaneTrackerApplication.getContext().getString(R.string.hsReplayClaimFailed), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onNext(ClaimResult claimResult) {
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.setData(Uri.parse(claimResult.full_url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ArcaneTrackerApplication.getContext().startActivity(i);
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
            signupProgressBar.setVisibility(GONE);
            signupButton.setVisibility(VISIBLE);
            signinButton.setEnabled(true);

            Context context = ArcaneTrackerApplication.getContext();
            if (user == null) {
                Toast.makeText(context, context.getString(R.string.trackobotSignupError), Toast.LENGTH_LONG).show();
            } else {
                Trackobot.get().setUser(user);

                FirebaseAnalytics.getInstance(context).logEvent("track_o_bot_signup", null);
                updateTrackobot(settingsView);
            }
        }
    };

    private Observer<HistoryList> mSigninObserver = new Observer<HistoryList>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(HistoryList historyList) {
            signinProgressBar.setVisibility(GONE);
            signinButton.setVisibility(VISIBLE);
            signupButton.setEnabled(true);

            if (historyList == null) {
                Toast.makeText(ArcaneTrackerApplication.getContext(), ArcaneTrackerApplication.getContext().getString(R.string.cannotLinkTrackobot), Toast.LENGTH_LONG).show();
                Trackobot.get().setUser(null);
            } else {
                FirebaseAnalytics.getInstance(ArcaneTrackerApplication.getContext()).logEvent("track_o_bot_signin", null);
                updateTrackobot(settingsView);
            }

        }
    };

    private Observer<HistoryList> mImportObserver = new Observer<HistoryList>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
            importProgressBar.setVisibility(GONE);
            importButton.setVisibility(VISIBLE);
            importButton.setEnabled(true);

            Toast.makeText(ArcaneTrackerApplication.getContext(), ArcaneTrackerApplication.getContext().getString(R.string.cannotLinkTrackobot), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onNext(HistoryList historyList) {
            importProgressBar.setVisibility(GONE);
            importButton.setVisibility(VISIBLE);
            importButton.setEnabled(true);

            if (historyList == null) {
                Toast.makeText(ArcaneTrackerApplication.getContext(), ArcaneTrackerApplication.getContext().getString(R.string.cannotLinkTrackobot), Toast.LENGTH_LONG).show();
                Trackobot.get().setUser(null);
            } else {
                FirebaseAnalytics.getInstance(ArcaneTrackerApplication.getContext()).logEvent("track_o_bot_signin", null);
                updateTrackobot(settingsView);
            }
        }
    };

    private View.OnClickListener mSigninButtonClicked = v -> {
        signinButton.setVisibility(GONE);
        signinProgressBar.setVisibility(VISIBLE);
        signupButton.setEnabled(false);

        User user = new User();
        user.username = usernameEditText.getText().toString();
        user.password = passwordEditText.getText().toString();
        Trackobot.get().setUser(user);

        Trackobot.get().service().getHistoryList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mSigninObserver);
    };

    private View.OnClickListener mSignupButtonClicked = v -> {

        signupButton.setVisibility(GONE);
        signupProgressBar.setVisibility(VISIBLE);
        signinButton.setEnabled(false);

        Trackobot.get().service().createUser()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mSignupObserver);
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

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url.url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ArcaneTrackerApplication.getContext().startActivity(i);
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
            importButton.setVisibility(GONE);
            importProgressBar.setVisibility(VISIBLE);
            importButton.setEnabled(false);

            Trackobot.get().setUser(user);

            Trackobot.get().service().getHistoryList()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(mImportObserver);
        }
    };
    private boolean firstTime;


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
        importProgressBar = (ProgressBar)view.findViewById(R.id.importProgressBar);
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

        TextView appVersion = (TextView) view.findViewById(R.id.appVersion);
        appVersion.setText(view.getContext().getString(R.string.thisIsArcaneTracker, BuildConfig.VERSION_NAME, Utils.isAppDebuggable() ? " (debug)":""));

        Button feedbackButton = (Button)view.findViewById(R.id.feedBackButton);
        feedbackButton.setOnClickListener(v -> {
            ViewManager.get().removeView(settingsView);

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"support@arcanetracker.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Arcane Tracker Feedback");
            emailIntent.putExtra(Intent.EXTRA_TEXT, view.getContext().getString(R.string.decribeYourProblem));

            FileTree.get().sync();
            Uri uri = FileProvider.getUriForFile(view.getContext(), "net.mbonnin.arcanetracker.fileprovider", FileTree.get().getFile());
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);

            ArcaneTrackerApplication.getContext().startActivity(emailIntent);
        });

        Button resetCacheButton = (Button)view.findViewById(R.id.resetCache);
        resetCacheButton.setOnClickListener(v -> {
            PicassoCardRequestHandler.get().resetCache();
        });

        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekBar);
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
        for (Language language: Language.allLanguages) {
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
                String newKey = position == 0 ? null: Language.allLanguages.get(position-1).key;
                String oldKey = Settings.get(Settings.LANGUAGE, null);
                boolean areSame = newKey == null ? oldKey == null : newKey.equals(oldKey);
                if (!firstTime && !areSame) {
                    Settings.set(Settings.LANGUAGE, newKey);
                    View view2 = LayoutInflater.from(context).inflate(R.layout.please_restart, null);
                    view2.findViewById(R.id.ok).setOnClickListener(v3 -> {
                        ViewManager.get().removeView(view2);
                    });

                    ViewManager.Params params = new ViewManager.Params();
                    params.w = (int) (ViewManager.get().getWidth() * 0.6f);
                    params.h = ViewManager.get().getHeight() / 2;
                    params.x = (ViewManager.get().getWidth() -params.w)/ 2;
                    params.y = ViewManager.get().getHeight() / 4;

                    ViewManager.get().addModalView(view2, params);
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

        CheckBox hsReplay = (CheckBox) view.findViewById(R.id.hsReplay);
        hsReplay.setChecked(Settings.get(Settings.HSREPLAY, Settings.DEFAULT_HSREPLAY));
        hsReplay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Settings.set(Settings.HSREPLAY, isChecked);
        });
        View hsReplayHistory = view.findViewById(R.id.hsReplayHistory);
        if (hsReplayHistory != null) {
            hsReplayHistory.setOnClickListener(v -> HistoryCompanion.show());
        }

        View claimView = view.findViewById(R.id.hsreplayClaim);

        String token = HSReplay.get().getToken();
        if (token == null) {
            claimView.setVisibility(GONE);
        } else {
            mClaimViewCompanion = new LoadableButtonCompanion(claimView);
            mClaimViewCompanion.button.setEnabled(false);

            HSReplay.get().service().getToken(token)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(mHSReplayTokenObserver);
        }
    }


    public static void show() {
        Context context = ArcaneTrackerApplication.getContext();
        ViewManager viewManager = ViewManager.get();
        View view2 = LayoutInflater.from(context).inflate(R.layout.settings_view, null);

        new SettingsCompanion(view2);

        ViewManager.Params params = new ViewManager.Params();
        params.x = viewManager.getWidth() / 4;
        params.y = viewManager.getHeight() / 16;
        params.w = viewManager.getWidth() / 2;
        params.h = 7 * viewManager.getHeight() / 8;

        viewManager.addModalAndFocusableView(view2, params);
    }
}
