package com.rdm.client.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

/**
 * Helper class for location-related operations
 * Handles GPS location retrieval using FusedLocationProviderClient
 */
public class LocationHelper {
    
    private static final String TAG = "LocationHelper";
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    
    /**
     * Interface for location callbacks
     */
    public interface LocationCallback {
        void onLocationReceived(Location location);
    }
    
    public LocationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }
    
    /**
     * Get current location asynchronously
     * @param callback Callback to receive location result
     */
    public void getCurrentLocation(LocationCallback callback) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted");
            callback.onLocationReceived(null);
            return;
        }
        
        if (!isLocationEnabled()) {
            Log.w(TAG, "Location services disabled");
            callback.onLocationReceived(null);
            return;
        }
        
        try {
            // Create cancellation token for timeout
            CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
            
            // Request current location with high accuracy
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.getToken()
            ).addOnSuccessListener(location -> {
                if (location != null) {
                    Log.d(TAG, "Location obtained: " + location.getLatitude() + ", " + location.getLongitude());
                    callback.onLocationReceived(location);
                } else {
                    Log.w(TAG, "Location is null, trying last known location");
                    getLastKnownLocation(callback);
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get current location", e);
                getLastKnownLocation(callback);
            });
            
            // Set timeout for location request (10 seconds)
            new android.os.Handler().postDelayed(() -> {
                cancellationTokenSource.cancel();
            }, 10000);
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting location", e);
            callback.onLocationReceived(null);
        } catch (Exception e) {
            Log.e(TAG, "Error getting current location", e);
            callback.onLocationReceived(null);
        }
    }
    
    /**
     * Get last known location as fallback
     * @param callback Callback to receive location result
     */
    private void getLastKnownLocation(LocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onLocationReceived(null);
            return;
        }
        
        try {
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        Log.d(TAG, "Last known location: " + location.getLatitude() + ", " + location.getLongitude());
                        callback.onLocationReceived(location);
                    } else {
                        Log.w(TAG, "No last known location available");
                        callback.onLocationReceived(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get last known location", e);
                    callback.onLocationReceived(null);
                });
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting last known location", e);
            callback.onLocationReceived(null);
        }
    }
    
    /**
     * Check if location permissions are granted
     * @return true if permissions granted, false otherwise
     */
    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Check if location services are enabled
     * @return true if enabled, false otherwise
     */
    public boolean isLocationEnabled() {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                       locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking location enabled status", e);
        }
        return false;
    }
    
    /**
     * Get location provider status
     * @return Status string
     */
    public String getLocationStatus() {
        if (!hasLocationPermission()) {
            return "Permission Denied";
        }
        
        if (!isLocationEnabled()) {
            return "Location Disabled";
        }
        
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                
                if (gpsEnabled && networkEnabled) {
                    return "GPS + Network";
                } else if (gpsEnabled) {
                    return "GPS Only";
                } else if (networkEnabled) {
                    return "Network Only";
                } else {
                    return "No Providers";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting location status", e);
        }
        
        return "Unknown";
    }
    
    /**
     * Calculate distance between two locations
     * @param lat1 Latitude of first location
     * @param lon1 Longitude of first location
     * @param lat2 Latitude of second location
     * @param lon2 Longitude of second location
     * @return Distance in meters
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }
    
    /**
     * Format location coordinates for display
     * @param location Location object
     * @return Formatted coordinate string
     */
    public static String formatCoordinates(Location location) {
        if (location == null) {
            return "Unknown";
        }
        
        return String.format("%.6f, %.6f", location.getLatitude(), location.getLongitude());
    }
    
    /**
     * Get location accuracy description
     * @param location Location object
     * @return Accuracy description
     */
    public static String getAccuracyDescription(Location location) {
        if (location == null || !location.hasAccuracy()) {
            return "Unknown";
        }
        
        float accuracy = location.getAccuracy();
        if (accuracy < 5) {
            return "Excellent (" + accuracy + "m)";
        } else if (accuracy < 10) {
            return "Good (" + accuracy + "m)";
        } else if (accuracy < 50) {
            return "Fair (" + accuracy + "m)";
        } else {
            return "Poor (" + accuracy + "m)";
        }
    }
}