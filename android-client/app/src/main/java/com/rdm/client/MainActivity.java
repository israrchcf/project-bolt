package com.rdm.client;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.rdm.client.models.ApiResponse;
import com.rdm.client.models.DeviceInfo;
import com.rdm.client.network.ApiService;
import com.rdm.client.network.RetrofitClient;
import com.rdm.client.services.DataSyncWorker;
import com.rdm.client.utils.DeviceHelper;
import com.rdm.client.utils.LocationHelper;
import com.rdm.client.utils.NetworkHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Main Activity for RDM Client
 * Handles device registration, monitoring controls, and UI updates
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "RDMPrefs";
    private static final String KEY_DEVICE_REGISTERED = "device_registered";
    private static final String KEY_MONITORING_ACTIVE = "monitoring_active";
    private static final String KEY_LAST_SYNC = "last_sync";
    private static final String WORK_NAME = "DataSyncWork";
    
    // UI Components
    private TextView tvDeviceId, tvDeviceModel, tvDeviceManufacturer;
    private TextView tvBatteryLevel, tvNetworkStatus, tvLocationStatus, tvLastSync;
    private TextView tvMonitoringStatus;
    private Button btnToggleMonitoring, btnSyncNow;
    
    // Helpers
    private DeviceHelper deviceHelper;
    private LocationHelper locationHelper;
    private NetworkHelper networkHelper;
    private ApiService apiService;
    private SharedPreferences prefs;
    
    // Permission launcher
    private ActivityResultLauncher<String[]> permissionLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeComponents();
        setupPermissionLauncher();
        initializeUI();
        checkPermissionsAndStart();
    }
    
    /**
     * Initialize all components and helpers
     */
    private void initializeComponents() {
        // Initialize UI components
        tvDeviceId = findViewById(R.id.tvDeviceId);
        tvDeviceModel = findViewById(R.id.tvDeviceModel);
        tvDeviceManufacturer = findViewById(R.id.tvDeviceManufacturer);
        tvBatteryLevel = findViewById(R.id.tvBatteryLevel);
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        tvLastSync = findViewById(R.id.tvLastSync);
        tvMonitoringStatus = findViewById(R.id.tvMonitoringStatus);
        btnToggleMonitoring = findViewById(R.id.btnToggleMonitoring);
        btnSyncNow = findViewById(R.id.btnSyncNow);
        
        // Initialize helpers
        deviceHelper = new DeviceHelper(this);
        locationHelper = new LocationHelper(this);
        networkHelper = new NetworkHelper(this);
        apiService = RetrofitClient.getInstance().getApiService();
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Set button listeners
        btnToggleMonitoring.setOnClickListener(v -> toggleMonitoring());
        btnSyncNow.setOnClickListener(v -> performManualSync());
    }
    
    /**
     * Setup permission launcher for runtime permissions
     */
    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                
                if (allGranted) {
                    Log.d(TAG, "All permissions granted");
                    initializeApp();
                } else {
                    Log.w(TAG, "Some permissions denied");
                    showPermissionDialog();
                }
            }
        );
    }
    
    /**
     * Initialize UI with device information
     */
    private void initializeUI() {
        // Display device information
        tvDeviceId.setText(deviceHelper.getDeviceId());
        tvDeviceModel.setText(deviceHelper.getDeviceModel());
        tvDeviceManufacturer.setText(deviceHelper.getDeviceManufacturer());
        
        // Update monitoring status
        updateMonitoringStatus();
        updateLastSyncTime();
        
        // Start periodic UI updates
        startUIUpdates();
    }
    
    /**
     * Check required permissions and request if needed
     */
    private void checkPermissionsAndStart() {
        String[] requiredPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET
        };
        
        // Add background location permission for Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String[] extendedPermissions = new String[requiredPermissions.length + 1];
            System.arraycopy(requiredPermissions, 0, extendedPermissions, 0, requiredPermissions.length);
            extendedPermissions[requiredPermissions.length] = Manifest.permission.ACCESS_BACKGROUND_LOCATION;
            requiredPermissions = extendedPermissions;
        }
        
        boolean allPermissionsGranted = true;
        for (String permission : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        
        if (allPermissionsGranted) {
            initializeApp();
        } else {
            permissionLauncher.launch(requiredPermissions);
        }
    }
    
    /**
     * Initialize app after permissions are granted
     */
    private void initializeApp() {
        checkBatteryOptimization();
        
        if (!prefs.getBoolean(KEY_DEVICE_REGISTERED, false)) {
            registerDevice();
        }
        
        updateStatusInformation();
    }
    
    /**
     * Check and request battery optimization exemption
     */
    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                showBatteryOptimizationDialog();
            }
        }
    }
    
    /**
     * Show battery optimization dialog
     */
    private void showBatteryOptimizationDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.battery_optimization_title)
            .setMessage(R.string.battery_optimization_message)
            .setPositiveButton("Settings", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    /**
     * Show permission explanation dialog
     */
    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app requires location and network permissions to function properly. Please grant all permissions.")
            .setPositiveButton("Retry", (dialog, which) -> checkPermissionsAndStart())
            .setNegativeButton("Exit", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }
    
    /**
     * Register device with backend server
     */
    private void registerDevice() {
        Log.d(TAG, "Registering device...");
        
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId(deviceHelper.getDeviceId());
        deviceInfo.setModel(deviceHelper.getDeviceModel());
        deviceInfo.setManufacturer(deviceHelper.getDeviceManufacturer());
        deviceInfo.setOsVersion(deviceHelper.getOSVersion());
        deviceInfo.setAppVersion(deviceHelper.getAppVersion());
        
        Call<ApiResponse> call = apiService.registerDevice(deviceInfo);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Log.d(TAG, "Device registered successfully");
                        prefs.edit().putBoolean(KEY_DEVICE_REGISTERED, true).apply();
                        Toast.makeText(MainActivity.this, R.string.device_registered, Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Registration failed: " + apiResponse.getMessage());
                        Toast.makeText(MainActivity.this, R.string.registration_failed, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Registration failed: " + response.code());
                    Toast.makeText(MainActivity.this, R.string.registration_failed, Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.e(TAG, "Registration failed", t);
                Toast.makeText(MainActivity.this, R.string.registration_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Toggle monitoring on/off
     */
    private void toggleMonitoring() {
        boolean isActive = prefs.getBoolean(KEY_MONITORING_ACTIVE, false);
        
        if (isActive) {
            stopMonitoring();
        } else {
            startMonitoring();
        }
    }
    
    /**
     * Start background monitoring
     */
    private void startMonitoring() {
        Log.d(TAG, "Starting monitoring...");
        
        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
            DataSyncWorker.class, 15, TimeUnit.MINUTES) // Minimum interval is 15 minutes
            .build();
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            syncWorkRequest
        );
        
        prefs.edit().putBoolean(KEY_MONITORING_ACTIVE, true).apply();
        updateMonitoringStatus();
        
        Toast.makeText(this, R.string.sync_started, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Stop background monitoring
     */
    private void stopMonitoring() {
        Log.d(TAG, "Stopping monitoring...");
        
        WorkManager.getInstance(this).cancelUniqueWork(WORK_NAME);
        prefs.edit().putBoolean(KEY_MONITORING_ACTIVE, false).apply();
        updateMonitoringStatus();
        
        Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Perform manual sync
     */
    private void performManualSync() {
        Log.d(TAG, "Performing manual sync...");
        
        // Create one-time work request for immediate sync
        androidx.work.OneTimeWorkRequest syncWork = new androidx.work.OneTimeWorkRequest.Builder(DataSyncWorker.class)
            .build();
        
        WorkManager.getInstance(this).enqueue(syncWork);
        
        Toast.makeText(this, "Syncing data...", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Update monitoring status UI
     */
    private void updateMonitoringStatus() {
        boolean isActive = prefs.getBoolean(KEY_MONITORING_ACTIVE, false);
        
        if (isActive) {
            tvMonitoringStatus.setText(R.string.monitoring_active);
            tvMonitoringStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
            btnToggleMonitoring.setText(R.string.stop_monitoring);
            btnToggleMonitoring.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            tvMonitoringStatus.setText(R.string.monitoring_inactive);
            tvMonitoringStatus.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            btnToggleMonitoring.setText(R.string.start_monitoring);
            btnToggleMonitoring.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        }
    }
    
    /**
     * Update last sync time display
     */
    private void updateLastSyncTime() {
        long lastSync = prefs.getLong(KEY_LAST_SYNC, 0);
        if (lastSync > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            tvLastSync.setText(sdf.format(new Date(lastSync)));
        } else {
            tvLastSync.setText("Never");
        }
    }
    
    /**
     * Update status information (battery, network, location)
     */
    private void updateStatusInformation() {
        // Update battery level
        int batteryLevel = deviceHelper.getBatteryLevel();
        tvBatteryLevel.setText(batteryLevel + "%");
        
        // Update network status
        String networkStatus = networkHelper.getNetworkStatus();
        tvNetworkStatus.setText(networkStatus);
        
        // Update location status
        locationHelper.getCurrentLocation(location -> {
            if (location != null) {
                tvLocationStatus.setText("Available");
            } else {
                tvLocationStatus.setText("Unavailable");
            }
        });
    }
    
    /**
     * Start periodic UI updates
     */
    private void startUIUpdates() {
        // Update UI every 30 seconds
        new Thread(() -> {
            while (!isFinishing()) {
                runOnUiThread(() -> {
                    updateStatusInformation();
                    updateLastSyncTime();
                });
                
                try {
                    Thread.sleep(30000); // 30 seconds
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateStatusInformation();
        updateLastSyncTime();
    }
}