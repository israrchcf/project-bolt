package com.rdm.client.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.rdm.client.services.DataSyncWorker;

import java.util.concurrent.TimeUnit;

/**
 * Boot receiver to restart monitoring after device reboot
 * Ensures background sync continues after system restart
 */
public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    private static final String PREFS_NAME = "RDMPrefs";
    private static final String KEY_MONITORING_ACTIVE = "monitoring_active";
    private static final String WORK_NAME = "DataSyncWork";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            
            // Check if monitoring was active before reboot
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean wasMonitoringActive = prefs.getBoolean(KEY_MONITORING_ACTIVE, false);
            
            if (wasMonitoringActive) {
                Log.d(TAG, "Restarting monitoring after boot...");
                restartMonitoring(context);
            } else {
                Log.d(TAG, "Monitoring was not active, skipping restart");
            }
        }
    }
    
    /**
     * Restart background monitoring service
     * @param context Application context
     */
    private void restartMonitoring(Context context) {
        try {
            PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                DataSyncWorker.class, 15, TimeUnit.MINUTES)
                .build();
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                syncWorkRequest
            );
            
            Log.d(TAG, "Monitoring restarted successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart monitoring", e);
        }
    }
}