package com.rdm.client.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.rdm.client.models.ApiResponse;
import com.rdm.client.models.DeviceInfo;
import com.rdm.client.network.ApiService;
import com.rdm.client.network.RetrofitClient;
import com.rdm.client.utils.DeviceHelper;
import com.rdm.client.utils.LocationHelper;
import com.rdm.client.utils.NetworkHelper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Background worker for syncing device data with backend
 * Runs periodically to send device status updates
 */
public class DataSyncWorker extends Worker {
    
    private static final String TAG = "DataSyncWorker";
    private static final String PREFS_NAME = "RDMPrefs";
    private static final String KEY_LAST_SYNC = "last_sync";
    private static final String KEY_API_KEY = "api_key";
    private static final String DEFAULT_API_KEY = "rdm-client-key-2024"; // TODO: Use secure key management
    
    private DeviceHelper deviceHelper;
    private LocationHelper locationHelper;
    private NetworkHelper networkHelper;
    private ApiService apiService;
    private SharedPreferences prefs;
    
    public DataSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        
        // Initialize helpers and services
        deviceHelper = new DeviceHelper(context);
        locationHelper = new LocationHelper(context);
        networkHelper = new NetworkHelper(context);
        apiService = RetrofitClient.getInstance().getApiService();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting data sync work...");
        
        try {
            // Collect device information
            DeviceInfo deviceInfo = collectDeviceInfo();
            
            if (deviceInfo == null) {
                Log.e(TAG, "Failed to collect device info");
                return Result.retry();
            }
            
            // Sync with backend
            boolean syncSuccess = syncWithBackend(deviceInfo);
            
            if (syncSuccess) {
                // Update last sync time
                prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply();
                Log.d(TAG, "Data sync completed successfully");
                return Result.success();
            } else {
                Log.e(TAG, "Data sync failed");
                return Result.retry();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error during data sync", e);
            return Result.failure();
        }
    }
    
    /**
     * Collect current device information and status
     * @return DeviceInfo object with current data
     */
    private DeviceInfo collectDeviceInfo() {
        try {
            DeviceInfo deviceInfo = new DeviceInfo();
            
            // Basic device information
            deviceInfo.setDeviceId(deviceHelper.getDeviceId());
            deviceInfo.setModel(deviceHelper.getDeviceModel());
            deviceInfo.setManufacturer(deviceHelper.getDeviceManufacturer());
            deviceInfo.setOsVersion(deviceHelper.getOSVersion());
            deviceInfo.setAppVersion(deviceHelper.getAppVersion());
            
            // Current status
            deviceInfo.setBatteryLevel(deviceHelper.getBatteryLevel());
            deviceInfo.setNetworkStatus(networkHelper.getNetworkStatus());
            deviceInfo.setLocalIp(networkHelper.getLocalIpAddress());
            
            // Get location (with timeout)
            CountDownLatch locationLatch = new CountDownLatch(1);
            final double[] coordinates = new double[2]; // [latitude, longitude]
            
            locationHelper.getCurrentLocation(location -> {
                if (location != null) {
                    coordinates[0] = location.getLatitude();
                    coordinates[1] = location.getLongitude();
                    Log.d(TAG, "Location obtained: " + coordinates[0] + ", " + coordinates[1]);
                } else {
                    Log.w(TAG, "Location not available");
                    coordinates[0] = 0.0;
                    coordinates[1] = 0.0;
                }
                locationLatch.countDown();
            });
            
            // Wait for location (max 10 seconds)
            try {
                locationLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Log.w(TAG, "Location timeout", e);
            }
            
            deviceInfo.setLatitude(coordinates[0]);
            deviceInfo.setLongitude(coordinates[1]);
            
            // Get public IP (optional, may fail)
            try {
                String publicIp = networkHelper.getPublicIpAddress();
                deviceInfo.setPublicIp(publicIp);
            } catch (Exception e) {
                Log.w(TAG, "Failed to get public IP", e);
                deviceInfo.setPublicIp("unknown");
            }
            
            Log.d(TAG, "Device info collected: " + deviceInfo.toString());
            return deviceInfo;
            
        } catch (Exception e) {
            Log.e(TAG, "Error collecting device info", e);
            return null;
        }
    }
    
    /**
     * Sync device information with backend server
     * @param deviceInfo Device information to sync
     * @return true if sync successful, false otherwise
     */
    private boolean syncWithBackend(DeviceInfo deviceInfo) {
        try {
            String apiKey = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY);
            
            Call<ApiResponse> call = apiService.syncDeviceData(apiKey, deviceInfo);
            Response<ApiResponse> response = call.execute();
            
            if (response.isSuccessful() && response.body() != null) {
                ApiResponse apiResponse = response.body();
                if (apiResponse.isSuccess()) {
                    Log.d(TAG, "Sync successful: " + apiResponse.getMessage());
                    return true;
                } else {
                    Log.e(TAG, "Sync failed: " + apiResponse.getMessage());
                    return false;
                }
            } else {
                Log.e(TAG, "Sync request failed: " + response.code() + " - " + response.message());
                
                // Log response body for debugging
                if (response.errorBody() != null) {
                    try {
                        String errorBody = response.errorBody().string();
                        Log.e(TAG, "Error response body: " + errorBody);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to read error body", e);
                    }
                }
                return false;
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Network error during sync", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during sync", e);
            return false;
        }
    }
}