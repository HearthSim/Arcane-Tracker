package net.mbonnin.arcanetracker;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private static final int REQUEST_CODE_GET_OVERLAY_PERMISSIONS = 2;
    public static final int REQUEST_CODE_MEDIAPROJECTION = 42;
    public static final String HEARTHSTONE_PACKAGE_ID = "com.blizzard.wtcg.hearthstone";
    View contentView;
    private Button button;
    private View permissions;
    private CheckBox checkbox;
    private Handler mHandler;
    private AlertDialog mDialog;
    private ScreenCapture mScreenCapture;
    private MediaProjectionManager mProjectionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Utils.logWithDate("MainActivity.onCreate");

        try {
            if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
                Timber.d("Firebase token: " + FirebaseInstanceId.getInstance().getToken());
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        mHandler = new Handler();

        contentView = findViewById(R.id.activity_main);
        button = (Button) findViewById(R.id.button);
        checkbox = (CheckBox) findViewById(R.id.checkbox);
        permissions = findViewById(R.id.permissions);

        boolean showNextTime = Settings.get(Settings.SHOW_NEXT_TIME, true);
        checkbox.setChecked(Settings.get(Settings.SHOW_NEXT_TIME, showNextTime));

        if (hasAllPermissions()) {
            permissions.setVisibility(View.GONE);
            button.setText(getString(R.string.play));
        } else {
            button.setText(getString(R.string.authorizeAndPlay));
        }

        button.setOnClickListener(v -> {
            tryToLaunchGame();
        });

        if (!showNextTime) {
            tryToLaunchGame();
            return;
        }
    }

    private boolean hasAllPermissions() {
        boolean has = checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            has &= android.provider.Settings.canDrawOverlays(this);
        }

        return has;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_MEDIAPROJECTION) {
            if (resultCode == RESULT_OK) {
                MediaProjection projection = mProjectionManager.getMediaProjection(resultCode, data);
                mScreenCapture = new ScreenCapture(this, projection);
            }

        } else {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                Snackbar.make(contentView, getString(R.string.pleaseEnablePermissions), Snackbar.LENGTH_LONG).show();
            } else {
                tryToLaunchGame();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(contentView, getString(R.string.pleaseEnablePermissions), Snackbar.LENGTH_LONG).show();
        } else {
            tryToLaunchGame();
        }
    }

    private void tryToLaunchGame() {
        /*
         * Do not use the application context, dialogs do not work with an application context
         */
        Context context = new ContextThemeWrapper(this, R.style.AppThemeLight);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
                return;
            } else if (!android.provider.Settings.canDrawOverlays(this)) {
                try {
                    Intent intent2 = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent2, REQUEST_CODE_GET_OVERLAY_PERMISSIONS);
                } catch (Exception e) {
                    mDialog = new AlertDialog.Builder(context)
                            .setTitle(getString(R.string.hi_there))
                            .setMessage(getString(R.string.overlay_explanation))
                            .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                                dialog.dismiss();
                            })
                            .show();
                }
                return;
            }
        }

        if (false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mScreenCapture == null) {
            mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIAPROJECTION);
            return;
        }
        if (Build.MANUFACTURER.toLowerCase().contains("xiaomi") && Settings.get(Settings.SHOW_XIAOMI_WARNING, true)) {
            mDialog = new AlertDialog.Builder(context)
                    .setTitle(getString(R.string.hi_there))
                    .setMessage(getString(R.string.xiaomi_explanation))
                    .setNeutralButton(getString(R.string.learn_more), (dialog, which) -> {
                        Utils.openLink("https://www.reddit.com/r/arcanetracker/comments/5nygi0/read_this_if_you_are_playing_on_a_xiaomi_device/");
                    })
                    .setPositiveButton(getString(R.string.gotIt), (dialog, which) -> {
                        dialog.dismiss();
                        Settings.set(Settings.SHOW_XIAOMI_WARNING, false);
                        tryToLaunchGame();
                    })
                    .show();
            return;
        }

        launchGame();
    }

    private void launchGame() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(HEARTHSTONE_PACKAGE_ID);
        if (launchIntent != null) {
            Settings.set(Settings.SHOW_NEXT_TIME, checkbox.isChecked());

            try {
                InputStream inputStream = getResources().openRawResource(R.raw.log_config);

                File file = new File(Utils.getHSExternalDir() + "log.config");
                FileOutputStream outputStream = new FileOutputStream(file);

                byte buffer[] = new byte[8192];

                while (true) {
                    int read = inputStream.read(buffer);
                    if (read == -1) {
                        break;
                    } else if (read > 0) {
                        outputStream.write(buffer, 0, read);
                    }
                }
            } catch (Exception e) {
                Snackbar.make(contentView, getString(R.string.cannot_locate_heathstone_install), Snackbar.LENGTH_LONG).show();
                Utils.reportNonFatal(e);
            }

            startActivity(launchIntent);
            finish();

            Overlay.get().show();

            Settings.set(Settings.CHECK_IF_RUNNING, false);
        } else {
            Snackbar.make(contentView, getString(R.string.cannot_launch), Snackbar.LENGTH_LONG).show();
            Utils.reportNonFatal(new Exception("no intent to launch game"));
        }
    }
}
