package com.tumuyan.fixedplay;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

final class IntentLaunchHelper {

    private IntentLaunchHelper() {
    }

    static boolean tryStartActivity(Activity activity, PackageManager packageManager, Intent intent,
                                    String logTag) {
        if (intent == null || packageManager.resolveActivity(intent, 0) == null) {
            return false;
        }
        try {
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.w(logTag, "start failed: " + intent, e);
            return false;
        }
    }

    static boolean startActivityOrShowError(Activity activity, PackageManager packageManager, Intent intent,
                                            int errorResId, String logTag) {
        if (tryStartActivity(activity, packageManager, intent, logTag)) {
            return true;
        }
        Toast.makeText(activity, errorResId, Toast.LENGTH_SHORT).show();
        return false;
    }

    static boolean hasLaunchClass(String className) {
        return className != null && className.length() > 5;
    }

    static boolean hasTargetClass(String className) {
        return className != null && className.length() > 0;
    }

    static void applyTarget(Intent intent, String app, String className) {
        if (hasTargetClass(className)) {
            intent.setClassName(app, className);
        } else {
            intent.setPackage(app);
        }
    }

    static Intent buildMainLaunchIntent(String app, String className) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        if (hasLaunchClass(className)) {
            intent.setClassName(app, className);
        } else {
            intent.setPackage(app);
        }
        return intent;
    }
}
