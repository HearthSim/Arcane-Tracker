package net.mbonnin.arcanetracker;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private static final int REQUEST_CODE_GET_OVERLAY_PERMISSIONS = 2;
    public static final String HEARTHSTONE_PACKAGE_ID = "com.blizzard.wtcg.hearthstone";
    View contentView;
    private Button button;
    private View permissions;
    private CheckBox checkbox;
    private Handler mHandler;
    private AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Snackbar.make(contentView, getString(R.string.pleaseEnablePermissions), Snackbar.LENGTH_LONG).show();
        } else {
            tryToLaunchGame();
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

    Runnable mCardDbRunnable = new Runnable() {

        @Override
        public void run() {
            if (CardDb.isReady()) {
                if (mDialog != null) {
                    mDialog.dismiss();
                }
                launchGame();
            } else {
                mHandler.postDelayed(this, 500);
            }
        }
    };

    private void tryToLaunchGame() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
                return;
            } else if (!android.provider.Settings.canDrawOverlays(this)) {
                Intent intent2 = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent2, REQUEST_CODE_GET_OVERLAY_PERMISSIONS);
                return;
            }
        }

        if (!CardDb.isReady()) {
            Context context = new ContextThemeWrapper(this, R.style.AppThemeLight);
            View view = LayoutInflater.from(context).inflate(R.layout.waiting_cards_view, null);
            mDialog = new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setView(view)
                    .show();

            mCardDbRunnable.run();
            return;
        }

        launchGame();
    }

    private void launchGame() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(HEARTHSTONE_PACKAGE_ID);
        if (launchIntent != null) {
            try {
                InputStream inputStream = getResources().openRawResource(R.raw.log_config);

                File file = new File(Utils.getHearthstoneFilesDir() + "log.config");
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

            if (Settings.get(Settings.CHECK_IF_RUNNING, true)) {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfo = am.getRunningAppProcesses();

                for (int i = 0; i < runningAppProcessInfo.size(); i++) {
                    if (runningAppProcessInfo.get(i).processName.equals(HEARTHSTONE_PACKAGE_ID)) {
                        Toast.makeText(this, "Hearthstone was already running, you might have to kill it and restart it", Toast.LENGTH_LONG).show();
                    }
                }
                Settings.set(Settings.CHECK_IF_RUNNING, false);
            }
            Settings.set(Settings.SHOW_NEXT_TIME, checkbox.isChecked());

            startActivity(launchIntent);
            finish();

            Intent serviceIntent = new Intent();
            serviceIntent.setClass(this, MainService.class);
            startService(serviceIntent);

        } else {
            Snackbar.make(contentView, getString(R.string.cannot_launch), Snackbar.LENGTH_LONG).show();
            Utils.reportNonFatal(new Exception("no intent to launch game"));
        }
    }
}
