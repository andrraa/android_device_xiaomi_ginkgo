/*
 * Copyright (C) 2018-2019 The Xiaomi-SDM660 Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package org.lineageos.settings.device;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.os.SELinux;
import android.util.Log;
import android.widget.Toast;
import java.util.List;

import org.lineageos.settings.device.kcal.Utils;
import org.lineageos.settings.device.R;

import java.lang.Math.*;
import java.io.IOException;
import java.util.List;

public class BootReceiver extends BroadcastReceiver implements Utils {

    public static final  String HEADPHONE_GAIN_PATH = "/sys/kernel/sound_control/headphone_gain";
    public static final  String MIC_GAIN_PATH = "/sys/kernel/sound_control/mic_gain";
    
    private static final boolean DEBUG = false;

    private static final String PREF_SELINUX_MODE = "selinux_mode";
    private static final String TAG = "SettingsOnBoot";
    private boolean mSetupRunning = false;
    private Context settingsContext = null;
    private Context mContext;
    
    public void onReceive(Context context, Intent intent) {

    	if (DEBUG) Log.d(TAG, "Received boot completed intent");
    	mContext = context;
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procInfos =
                activityManager.getRunningAppProcesses();
        for(int i = 0; i < procInfos.size(); i++) {
            if(procInfos.get(i).processName.equals("com.google.android.setupwizard")) {
                mSetupRunning = true;
            }
        }

        if (DEBUG) Log.d(TAG, "We are" + mSetupRunning + "running in setup");

        if(!mSetupRunning) {
            try {
                settingsContext = context.createPackageContext("com.android.settings", 0);
            } catch (Exception e) {
                Log.e(TAG, "Package not found", e);
            }
            SharedPreferences sharedpreferences = context.getSharedPreferences("selinux_pref", Context.MODE_PRIVATE);

            if (DEBUG) Log.d(TAG, "sharedpreferences.contains(" + PREF_SELINUX_MODE + "): " + (sharedpreferences.contains(PREF_SELINUX_MODE) ? "True":"False"));

            if (sharedpreferences.contains(PREF_SELINUX_MODE)) {
                boolean currentIsSelinuxEnforcing = SELinux.isSELinuxEnforced();
                boolean isSelinuxEnforcing = sharedpreferences.getBoolean(PREF_SELINUX_MODE, currentIsSelinuxEnforcing);
                if (DEBUG) Log.d(TAG, String.format("currentIsSelinuxEnforcing: %s, isSelinuxEnforcing: %s", (currentIsSelinuxEnforcing ? "True" : "False"), (isSelinuxEnforcing ? "True" : "False")));
                try {
                    if (isSelinuxEnforcing) {
                        if (!currentIsSelinuxEnforcing) {
                            SuShell.runWithSuCheck("setenforce 1");
                            showToast(context.getString(R.string.selinux_enforcing_toast_title),
                                    context);
                        }
                    } else {
                        if (currentIsSelinuxEnforcing) {
                            SuShell.runWithSuCheck("setenforce 0");
                            showToast(context.getString(R.string.selinux_permissive_toast_title),
                                    context);
                        }
                    }
                } catch (SuShell.SuDeniedException e) {
                    showToast(context.getString(R.string.cannot_get_su), context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (Settings.Secure.getInt(context.getContentResolver(), PREF_ENABLED, 0) == 1) {
            FileUtils.setValue(KCAL_ENABLE, Settings.Secure.getInt(context.getContentResolver(),
                    PREF_ENABLED, 0));

            String rgbValue = Settings.Secure.getInt(context.getContentResolver(),
                    PREF_RED, RED_DEFAULT) + " " +
                    Settings.Secure.getInt(context.getContentResolver(), PREF_GREEN,
                            GREEN_DEFAULT) + " " +
                    Settings.Secure.getInt(context.getContentResolver(), PREF_BLUE,
                            BLUE_DEFAULT);

            FileUtils.setValue(KCAL_RGB, rgbValue);
            FileUtils.setValue(KCAL_MIN, Settings.Secure.getInt(context.getContentResolver(),
                    PREF_MINIMUM, MINIMUM_DEFAULT));
            FileUtils.setValue(KCAL_SAT, Settings.Secure.getInt(context.getContentResolver(),
                    PREF_GRAYSCALE, 0) == 1 ? 128 :
                    Settings.Secure.getInt(context.getContentResolver(),
                            PREF_SATURATION, SATURATION_DEFAULT) + SATURATION_OFFSET);
            FileUtils.setValue(KCAL_VAL, Settings.Secure.getInt(context.getContentResolver(),
                    PREF_VALUE, VALUE_DEFAULT) + VALUE_OFFSET);
            FileUtils.setValue(KCAL_CONT, Settings.Secure.getInt(context.getContentResolver(),
                    PREF_CONTRAST, CONTRAST_DEFAULT) + CONTRAST_OFFSET);
            FileUtils.setValue(KCAL_HUE, Settings.Secure.getInt(context.getContentResolver(),
                    PREF_HUE, HUE_DEFAULT));
        }

        FileUtils.setValue(DeviceSettings.VIBRATION_STRENGTH_PATH, Settings.Secure.getInt(
                context.getContentResolver(), DeviceSettings.PREF_VIBRATION_STRENGTH, 80) / 100.0 * (DeviceSettings.MAX_VIBRATION - DeviceSettings.MIN_VIBRATION) + DeviceSettings.MIN_VIBRATION);
        FileUtils.setValue(DeviceSettings.USB_FASTCHARGE_PATH, Settings.Secure.getInt(context.getContentResolver(),
                DeviceSettings.PREF_USB_FASTCHARGE, 0));
        context.startService(new Intent(context, DiracService.class));
    }
    
    private void showToast(String toastString, Context context) {
        Toast.makeText(context, toastString, Toast.LENGTH_SHORT)
                .show();
    }
}
