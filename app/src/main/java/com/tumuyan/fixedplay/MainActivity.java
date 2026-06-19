package com.tumuyan.fixedplay;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import android.support.v4.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;

public class MainActivity extends Activity {

    // 用户连续按 Home 的基础时间窗口。
    private static final long SECONDARY_LAUNCHER_USER_PRESS_WINDOW_MS = 500L;
    // 新版系统下，前后台切换和任务调度带来的额外容差时间。
    private static final long SECONDARY_LAUNCHER_SYSTEM_TOLERANCE_MS = 200L;
    // 旧版系统切换更慢，因此给更宽松的额外容差时间。
    private static final long SECONDARY_LAUNCHER_LEGACY_SYSTEM_TOLERANCE_MS = 300L;
    // 连续按下 3 次 Home 视为进入重新设置流程。
    private static final int SECONDARY_LAUNCHER_REQUIRED_PRESSES = 3;
    private static final String KEY_HOME_PRESS_COUNT = "combo";
    private static final String KEY_HOME_PRESS_START_TIME = "homePressStartTime";
    private static final String KEY_HOME_PRESS_TRIGGERED = "homePressTriggered";
    private static final String KEY_WAITING_FOR_SETTINGS_SCREEN = "waitingForSettingsScreen";

    PackageManager packageManager;
    final String THIS_PACKAGE = "com.tumuyan.fixedplay";
    long splash_time = 0;
    String mode = "r2", action = "";
    ImageView imgview = null;
    private boolean pendingHomeInvocation = true;
    private boolean pendingHomeInvocationCountsAsPress = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pendingHomeInvocationCountsAsPress = HomeLauncherEntry.isHomeIntent(
                getIntent().getAction(),
                getIntent().hasCategory(Intent.CATEGORY_HOME));
        packageManager = getPackageManager();
        Log.w("MainActivity", "Create");

        SharedPreferences read = getSharedPreferences("setting", MODE_PRIVATE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        // WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final String splash_img = read.getString("splash_img", "");

        splash_time = read.getInt("splash_time", 0);

        Log.w("MainActivity", "Create, splash_time = " + splash_time);
        // if (System.currentTimeMillis() - SystemClock.elapsedRealtime() >60000 ){
        // splash_time =0;
        // }
        if (splash_time > 0) {
            handler.sendEmptyMessageDelayed(GO, splash_time);
            setContentView(R.layout.splash_activity);
            imgview = findViewById(R.id.splash_img);
            imgview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    skip_splash();
                }
            });
            Glide.with(this)
                    .load(splash_img)
                    .placeholder(R.drawable.unknow)
                    // .asGif()
                    .into(imgview);
        }

        handlePendingHomeInvocation();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        pendingHomeInvocation = true;
        pendingHomeInvocationCountsAsPress = HomeLauncherEntry.isHomeIntent(
                intent.getAction(),
                intent.hasCategory(Intent.CATEGORY_HOME));
        Log.i("MainActivity", "Received new HOME intent");
        handlePendingHomeInvocation();
    }

    private void skip_splash() {
        if (imgview != null) {
            boolean countAsHomePress = pendingHomeInvocationCountsAsPress;
            handler.removeMessages(GO);
            splash_time = 0;
            imgview.setImageDrawable(null);
            pendingHomeInvocation = false;
            pendingHomeInvocationCountsAsPress = false;
            go(countAsHomePress);
        }
    }

    @Override
    public void onStart() {
        Log.w("MainActivity", "Start");
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.w("MainActivity", String.format("Resume, splash %d", splash_time));
        if (pendingHomeInvocation) {
            handlePendingHomeInvocation();
            return;
        }
        redirectToConfiguredAppIfNeeded();
    }

    private void handlePendingHomeInvocation() {
        if (pendingHomeInvocation && splash_time <= 0) {
            boolean countAsHomePress = pendingHomeInvocationCountsAsPress;
            pendingHomeInvocation = false;
            pendingHomeInvocationCountsAsPress = false;
            go(countAsHomePress);
        }
    }

    private void redirectToConfiguredAppIfNeeded() {
        SharedPreferences read = getSharedPreferences("setting", MODE_PRIVATE);
        String app = read.getString("app", "");
        String className = read.getString("class", "");
        String uri = read.getString("uri", "");
        boolean waitingForSettingsScreen = read.getBoolean(KEY_WAITING_FOR_SETTINGS_SCREEN, false);
        if (HomeLauncherEntry.shouldRedirectToConfiguredApp(
                pendingHomeInvocation,
                waitingForSettingsScreen,
                app,
                THIS_PACKAGE)) {
            Log.i("MainActivity", "Redirecting trampoline back to configured app");
            mode = read.getString("mode", "r2");
            action = read.getString("action", "");
            launchConfiguredApp(app, className, uri);
        }
    }

    private void openSettings() {
        openSettings(false);
    }

    private boolean openSettings(boolean clearSequenceOnSuccess) {
        Intent intent = new Intent(MainActivity.this, SettingActivity.class);
        try {
            startActivity(intent);
            if (clearSequenceOnSuccess) {
                clearHomePressSequence();
            }
            return true;
        } catch (Exception e) {
            Log.e("MainActivity", "Could not open settings", e);
            return false;
        }
    }

    private void openSettingsWithError() {
        openSettingsWithError(false);
    }

    private void openSettingsWithError(boolean clearSequenceOnSuccess) {
        Toast.makeText(MainActivity.this, R.string.error_could_not_start, Toast.LENGTH_SHORT).show();
        openSettings(clearSequenceOnSuccess);
    }

    private boolean startIntentOrOpenSettings(Intent intent) {
        return startIntentOrOpenSettings(intent, false);
    }

    private boolean startIntentOrOpenSettings(Intent intent, boolean clearSequenceOnSuccess) {
        if (IntentLaunchHelper.tryStartActivity(this, packageManager, intent, "MainActivity")) {
            if (clearSequenceOnSuccess) {
                clearHomePressSequence();
            }
            return true;
        }
        openSettingsWithError(clearSequenceOnSuccess);
        return false;
    }

    private void clearHomePressSequence() {
        getSharedPreferences("setting", MODE_PRIVATE)
                .edit()
                .remove(KEY_HOME_PRESS_COUNT)
                .remove(KEY_HOME_PRESS_START_TIME)
                .remove(KEY_HOME_PRESS_TRIGGERED)
                .remove(KEY_WAITING_FOR_SETTINGS_SCREEN)
                .remove("lastTime")
                .commit();
    }

    private void setWaitingForSettingsScreen(boolean waiting) {
        getSharedPreferences("setting", MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_WAITING_FOR_SETTINGS_SCREEN, waiting)
                .commit();
    }

    private boolean openSettingsAndHoldLauncher() {
        setWaitingForSettingsScreen(true);
        if (openSettings(false)) {
            return true;
        }
        setWaitingForSettingsScreen(false);
        return false;
    }

    private boolean handleSecondaryLauncher(SharedPreferences read, boolean countAsHomePress) {
        boolean apply2nd = read.getBoolean("apply2nd", false);
        final String app2nd = read.getString("app_2nd", "");
        final String class2nd = read.getString("class_2nd", "");
        boolean previousTriggered = read.getBoolean(KEY_HOME_PRESS_TRIGGERED, false);
        int previousCount = read.getInt(KEY_HOME_PRESS_COUNT, 0);
        long previousStartTime = read.getLong(KEY_HOME_PRESS_START_TIME, 0);
        long now = SystemClock.elapsedRealtime();
        HomePressSequence.Result sequence = HomeLauncherEntry.next(
                countAsHomePress,
                previousTriggered,
                previousCount,
                previousStartTime,
                now,
                getSecondaryLauncherDetectionWindow(),
                SECONDARY_LAUNCHER_REQUIRED_PRESSES);

        SharedPreferences.Editor editor = getSharedPreferences("setting", MODE_PRIVATE).edit();
        editor.putInt(KEY_HOME_PRESS_COUNT, sequence.count);
        editor.putLong(KEY_HOME_PRESS_START_TIME, sequence.startTime);
        editor.putBoolean(KEY_HOME_PRESS_TRIGGERED, sequence.triggered);
        editor.remove("lastTime");
        editor.commit();

        Log.i("HomePressSequence", "count=" + sequence.count
                + ", triggered=" + sequence.triggered);

        if (!sequence.triggered) {
            return false;
        }

        if (HomeLauncherEntry.actionForTrigger(apply2nd,
                app2nd) == HomeLauncherEntry.TriggerAction.LAUNCH_SECONDARY_APP) {
            launchSecondaryApp(app2nd, class2nd);
        } else {
            openSettingsAndHoldLauncher();
        }
        return true;
    }

    private void launchSecondaryApp(String app2nd, String class2nd) {
        Intent intent = packageManager.getLaunchIntentForPackage(app2nd);
        if (intent != null) {
            intent.addCategory(Intent.CATEGORY_HOME);
            Log.w("2nd2", "length>0 -> intent not null");
            startIntentOrOpenSettings(intent, true);
            return;
        }

        intent = IntentLaunchHelper.buildMainLaunchIntent(app2nd, class2nd);
        startIntentOrOpenSettings(intent, true);
    }

    private void launchConfiguredApp(String app, String className, String uri) {
        switch (mode) {
            case "r2":
                launchDefaultApp(app);
                break;
            case "r1":
                launchExplicitOrDefaultApp(app, className);
                break;
            case "beta":
                launchBetaApp(app, className);
                break;
            case "uri":
                launchUriApp(app, className, uri);
                break;
            case "uri_dail":
                launchDialApp(app, className, uri);
                break;
            case "uri_file":
                launchFileApp(app, className, uri);
                break;
            default:
                openSettings();
                break;
        }
    }

    private void launchDefaultApp(String app) {
        Log.w("MainActivity mode2", mode);
        Intent intent = packageManager.getLaunchIntentForPackage(app);
        startIntentOrOpenSettings(intent);
    }

    private void launchExplicitOrDefaultApp(String app, String className) {
        if (IntentLaunchHelper.hasLaunchClass(className)) {
            Intent intent = new Intent();
            intent.setClassName(app, className);
            startIntentOrOpenSettings(intent);
            return;
        }

        Intent intent = packageManager.getLaunchIntentForPackage(app);
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        startIntentOrOpenSettings(intent);
    }

    private void launchBetaApp(String app, String className) {
        Intent intent = new Intent();
        if (action.length() > 0 && !"none".equals(action)) {
            intent.setAction(action);
        }

        if (IntentLaunchHelper.hasLaunchClass(className)) {
            intent.setClassName(app, className);
        } else {
            intent = packageManager.getLaunchIntentForPackage(app);
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
        }

        if (!IntentLaunchHelper.tryStartActivity(this, packageManager, intent, "MainActivity")) {
            Toast.makeText(this, getString(R.string.toast_main_start_error, mode), Toast.LENGTH_SHORT).show();
            openSettings();
        }
    }

    private void launchUriApp(String app, String className, String uri) {
        Uri u = Uri.parse(uri);
        Intent intent = new Intent(Intent.ACTION_VIEW, u);
        IntentLaunchHelper.applyTarget(intent, app, className);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startIntentOrOpenSettings(intent);
    }

    private void launchDialApp(String app, String className, String uri) {
        Uri u = Uri.parse(uri);
        Intent intent = new Intent(Intent.ACTION_DIAL, u);
        IntentLaunchHelper.applyTarget(intent, app, className);
        startIntentOrOpenSettings(intent);
    }

    private void launchFileApp(String app, String className, String uri) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        IntentLaunchHelper.applyTarget(intent, app, className);
        Uri u = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                new File(uri));
        intent.setDataAndType(u, "*/*");
        if (!IntentLaunchHelper.tryStartActivity(this, packageManager, intent, "MainActivity")) {
            Toast.makeText(this, R.string.error_could_not_start, Toast.LENGTH_SHORT).show();
        }
    }

    public void go() {
        go(false);
    }

    private void go(boolean countAsHomePress) {
        SharedPreferences read = getSharedPreferences("setting", MODE_PRIVATE);
        String app = read.getString("app", "");
        String className = read.getString("class", "");
        String uri = read.getString("uri", "");
        mode = read.getString("mode", "r2");
        action = read.getString("action", "");
        Log.i("MainActivity.go()", "mode=" + mode + ", packagename=" + app);

        if (handleSecondaryLauncher(read, countAsHomePress)) {
            return;
        }

        if (app.length() > 0 && !THIS_PACKAGE.equals(app)) {
            launchConfiguredApp(app, className, uri);
        } else {
            openSettings();
        }
    }

    private long getSecondaryLauncherDetectionWindow() {
        long systemTolerance = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M
                ? SECONDARY_LAUNCHER_LEGACY_SYSTEM_TOLERANCE_MS
                : SECONDARY_LAUNCHER_SYSTEM_TOLERANCE_MS;
        return SECONDARY_LAUNCHER_USER_PRESS_WINDOW_MS + systemTolerance;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 禁止使用返回键返回到上一页,但是可以直接退出程序
            return true;// 不执行父类点击事件
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            // 跳过
            skip_splash();
            return true;
        }
        return super.onKeyDown(keyCode, event);// 继续执行父类其他点击事件
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (imgview != null) {
            imgview.setImageDrawable(null);
        }
        super.onDestroy();
    }

    private static final int GO = 1;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GO:
                    splash_time = 0;
                    handlePendingHomeInvocation();
                    break;
            }
        }
    };
}
