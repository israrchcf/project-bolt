package com.rdm.client.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/**
 * Helper class for device-related operations
 * Provides methods to get device information and status
 */
public class DeviceHelper {
    
    private static final String TAG = "DeviceHelper";
    private final Context context;
    
    public DeviceHelper(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Get unique device identifier
     * @return Device ID string
     */
    public String getDeviceId() {
        try {
            // Use Android ID as device identifier
            String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
            );
            
            if (androidId != null && !androidId.isEmpty()) {
                return androidId;
            } else {
                // Fallback to a combination of device info
                return Build.MANUFACTURER + "_" + Build.MODEL + "_" + Build.SERIAL;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting device ID", e);
            return "unknown_device";
        }
    }
    
    /**
     * Get device model
     * @return Device model string
     */
    public String getDeviceModel() {
        return Build.MODEL;
    }
    
    /**
     * Get device manufacturer
     * @return Manufacturer string
     */
    public String getDeviceManufacturer() {
        return Build.MANUFACTURER;
    }
    
    /**
     * Get Android OS version
     * @return OS version string
     */
    public String getOSVersion() {
        return Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
    }
    
    /**
     * Get app version
     * @return App version string
     */
    public String getAppVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName + " (" + packageInfo.versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version", e);
            return "unknown";
        }
    }
    
    /**
     * Get current battery level
     * @return Battery level percentage (0-100)
     */
    public int getBatteryLevel() {
        try {
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, intentFilter);
            
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                
                if (level != -1 && scale != -1) {
                    return (int) ((level / (float) scale) * 100);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting battery level", e);
        }
        
        return -1; // Unknown battery level
    }
    
    /**
     * Get battery status information
     * @return Battery status string
     */
    public String getBatteryStatus() {
        try {
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, intentFilter);
            
            if (batteryStatus != null) {
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                
                switch (status) {
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                        return "Charging";
                    case BatteryManager.BATTERY_STATUS_DISCHARGING:
                        return "Discharging";
                    case BatteryManager.BATTERY_STATUS_FULL:
                        return "Full";
                    case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                        return "Not Charging";
                    case BatteryManager.BATTERY_STATUS_UNKNOWN:
                    default:
                        return "Unknown";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting battery status", e);
        }
        
        return "Unknown";
    }
    
    /**
     * Check if device is charging
     * @return true if charging, false otherwise
     */
    public boolean isCharging() {
        try {
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, intentFilter);
            
            if (batteryStatus != null) {
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                       status == BatteryManager.BATTERY_STATUS_FULL;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking charging status", e);
        }
        
        return false;
    }
    
    /**
     * Get device hardware information
     * @return Hardware info string
     */
    public String getHardwareInfo() {
        return "Board: " + Build.BOARD + 
               ", Brand: " + Build.BRAND + 
               ", Device: " + Build.DEVICE + 
               ", Hardware: " + Build.HARDWARE;
    }
    
    /**
     * Get device build information
     * @return Build info string
     */
    public String getBuildInfo() {
        return "Build ID: " + Build.ID + 
               ", Build Time: " + Build.TIME + 
               ", Build Type: " + Build.TYPE + 
               ", Build User: " + Build.USER;
    }
    
    /**
     * Get total device storage
     * @return Storage info string
     */
    public String getStorageInfo() {
        try {
            android.os.StatFs stat = new android.os.StatFs(android.os.Environment.getDataDirectory().getPath());
            long totalBytes = stat.getTotalBytes();
            long availableBytes = stat.getAvailableBytes();
            
            return "Total: " + formatBytes(totalBytes) + 
                   ", Available: " + formatBytes(availableBytes);
        } catch (Exception e) {
            Log.e(TAG, "Error getting storage info", e);
            return "Unknown";
        }
    }
    
    /**
     * Format bytes to human readable format
     * @param bytes Bytes to format
     * @return Formatted string
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}