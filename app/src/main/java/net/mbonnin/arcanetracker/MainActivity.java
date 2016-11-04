package net.mbonnin.arcanetracker;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.firebase.crash.FirebaseCrash;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private static final int REQUEST_CODE_GET_OVERLAY_PERMISSIONS = 2;
    public static final String HEARTHSTONE_FILES_DIR = "/sdcard/Android/data/com.blizzard.wtcg.hearthstone/files/";
    public static final String HEARTHSTONE_PACKAGE_ID = "com.blizzard.wtcg.hearthstone";
    View contentView;
    private Button button;
    private View permissions;
    private CheckBox checkbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentView = findViewById(R.id.activity_main);
        button = (Button) findViewById(R.id.button);
        checkbox = (CheckBox) findViewById(R.id.checkbox);
        permissions = findViewById(R.id.permissions);

        if (!Settings.get(Settings.SHOW_NEXT_TIME, true)) {
            launchGame();
            return;
        }
        checkbox.setChecked(Settings.get(Settings.SHOW_NEXT_TIME, true));

        if (hasAllPermissions()) {
            permissions.setVisibility(View.GONE);
            button.setText(getString(R.string.play));
        } else {
            button.setText(getString(R.string.authorizeAndPlay));
        }

        button.setOnClickListener(v -> {
            tryToLaunchGame();
        });
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

    private void tryToLaunchGame() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            launchGame();
        } else if (checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
        } else if (!android.provider.Settings.canDrawOverlays(this)) {
            Intent intent2 = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent2, REQUEST_CODE_GET_OVERLAY_PERMISSIONS);
        } else {
            launchGame();
        }
    }

    private void launchGame() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(HEARTHSTONE_PACKAGE_ID);
        if (launchIntent != null) {
            try {
                InputStream inputStream = getResources().openRawResource(R.raw.log_config);

                File file = new File(HEARTHSTONE_FILES_DIR + "log.config");
                FileOutputStream outputStream = new FileOutputStream(file);

                byte buffer[] = new byte[8192];

                while (true) {
                    int read = inputStream.read(buffer);
                    if (read == -1) {
                        break;
                    } else if (read > 0) {
                        outputStream.write(buffer, 0,read);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Snackbar.make(contentView, getString(R.string.cannot_locate_heathstone_install), Snackbar.LENGTH_LONG).show();
                FirebaseCrash.report(e);
            }

            if (Settings.get(Settings.CHECK_IF_RUNNING, true)) {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfo = am.getRunningAppProcesses();

                for (int i = 0; i < runningAppProcessInfo.size(); i++) {
                    if(runningAppProcessInfo.get(i).processName.equals(HEARTHSTONE_PACKAGE_ID)) {
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
            FirebaseCrash.report(new Exception("no intent to launch game"));
        }
    }
}
