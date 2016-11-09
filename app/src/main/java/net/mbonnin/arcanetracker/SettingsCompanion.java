package net.mbonnin.arcanetracker;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
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

import net.mbonnin.arcanetracker.trackobot.HistoryList;
import net.mbonnin.arcanetracker.trackobot.Trackobot;
import net.mbonnin.arcanetracker.trackobot.Url;
import net.mbonnin.arcanetracker.trackobot.User;

import java.io.File;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by martin on 10/24/16.
 */

public class SettingsCompanion {
    View settingsView;
    private TextView trackobotText;
    private Button signinButton;
    private Button signupButton;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private ProgressBar signupProgressBar;
    private ProgressBar signinProgressBar;

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

        }

        @Override
        public void onNext(User user) {
            signupProgressBar.setVisibility(GONE);
            signupButton.setVisibility(VISIBLE);
            signinButton.setEnabled(true);
            if (user == null) {
                Toast.makeText(ArcaneTrackerApplication.getContext(), "Cannot create trackobot account :(", Toast.LENGTH_LONG).show();
            } else {
                Trackobot.get().setUser(user);

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
                Toast.makeText(ArcaneTrackerApplication.getContext(), "Cannot link trackobot account :(", Toast.LENGTH_LONG).show();
                Trackobot.get().setUser(null);
            } else {
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
            Toast.makeText(ArcaneTrackerApplication.getContext(), "Could not get profile link, please try again", Toast.LENGTH_LONG).show();
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
    private View retrievePassword;


    private void updateTrackobot(View view) {
        signupButton = (Button) view.findViewById(R.id.trackobotSignup);
        signinButton = (Button) view.findViewById(R.id.trackobotSignin);
        trackobotText = ((TextView) (view.findViewById(R.id.trackobotText)));
        passwordEditText = (EditText) view.findViewById(R.id.password);
        usernameEditText = (EditText) view.findViewById(R.id.username);
        signinProgressBar = (ProgressBar) view.findViewById(R.id.signinProgressBar);
        signupProgressBar = (ProgressBar) view.findViewById(R.id.signupProgressBar);
        retrievePassword = view.findViewById(R.id.retrievePassword);

        User user = Trackobot.get().getUser();
        if (user == null) {
            trackobotText.setText("Linking a Track-o-bot account will allow you to track your stats on https://trackobot.com");
            view.findViewById(R.id.or).setVisibility(VISIBLE);

            usernameEditText.setEnabled(true);
            passwordEditText.setEnabled(true);

            signinButton.setText("Link existing account");
            signinButton.setOnClickListener(mSigninButtonClicked);

            signupButton.setText("Create new account");
            signupButton.setOnClickListener(mSignupButtonClicked);

            retrievePassword.setVisibility(VISIBLE);

        } else {
            trackobotText.setVisibility(GONE);
            view.findViewById(R.id.or).setVisibility(GONE);

            usernameEditText.setText(user.username);
            passwordEditText.setText(user.password);
            usernameEditText.setEnabled(false);
            passwordEditText.setEnabled(false);

            signinButton.setText("Unlink account");
            signinButton.setOnClickListener(v -> {
                Trackobot.get().setUser(null);
                usernameEditText.setText("");
                passwordEditText.setText("");
                updateTrackobot(settingsView);
            });

            signupButton.setText("Open profile (external browser)");
            signupButton.setOnClickListener(v -> {
                signupProgressBar.setVisibility(VISIBLE);
                signupButton.setVisibility(GONE);

                Trackobot.get().service().createOneTimeAuth()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(mOneTimeAuthObserver);
            });

            retrievePassword.setVisibility(GONE);

        }
    }

    public SettingsCompanion(View view) {
        settingsView = view;

        updateTrackobot(view);

        TextView appVersion = (TextView) view.findViewById(R.id.appVersion);
        appVersion.setText(String.format("This is Arcane Tracker version %s%s.", BuildConfig.VERSION_NAME, Utils.isAppDebuggable() ? " (debug)":""));

        Button feedbackButton = (Button)view.findViewById(R.id.feedBackButton);
        feedbackButton.setOnClickListener(v -> {
            ViewManager.get().removeView(settingsView);

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"support@arcanetracker.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Arcane Tracker Feedback");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Please describe your problem here");

            FileTree.get().sync();
            Uri uri = FileProvider.getUriForFile(view.getContext(), "net.mbonnin.arcanetracker.fileprovider", FileTree.get().getFile());
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);

            ArcaneTrackerApplication.getContext().startActivity(emailIntent);
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

        view.findViewById(R.id.quit).setOnClickListener(v -> MainService.stop());
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
