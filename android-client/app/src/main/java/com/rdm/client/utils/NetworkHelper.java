package com.rdm.client.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * Helper class for network-related operations
 * Provides methods to get network status and IP addresses
 */
public class NetworkHelper {
    
    private static final String TAG = "NetworkHelper";
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final WifiManager wifiManager;
    
    public NetworkHelper(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }
    
    /**
     * Get current network status
     * @return Network status string
     */
    public String getNetworkStatus() {
        try {
            if (connectivityManager == null) {
                return "Unknown";
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork == null) {
                    return "No Connection";
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (capabilities == null) {
                    return "No Connection";
                }
                
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return "WiFi Connected";
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return "Mobile Data";
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return "Ethernet";
                } else {
                    return "Connected";
                }
            } else {
                // Fallback for older Android versions
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
                    return "No Connection";
                }
                
                int type = activeNetworkInfo.getType();
                if (type == ConnectivityManager.TYPE_WIFI) {
                    return "WiFi Connected";
                } else if (type == ConnectivityManager.TYPE_MOBILE) {
                    return "Mobile Data";
                } else {
                    return "Connected";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting network status", e);
            return "Error";
        }
    }
    
    /**
     * Check if device is connected to internet
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        try {
            if (connectivityManager == null) {
                return false;
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork == null) {
                    return false;
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking connection status", e);
            return false;
        }
    }
    
    /**
     * Check if connected to WiFi
     * @return true if connected to WiFi, false otherwise
     */
    public boolean isWifiConnected() {
        try {
            if (connectivityManager == null) {
                return false;
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork == null) {
                    return false;
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            } else {
                NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                return wifiInfo != null && wifiInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking WiFi status", e);
            return false;
        }
    }
    
    /**
     * Get local IP address
     * @return Local IP address string
     */
    public String getLocalIpAddress() {
        try {
            // Try to get WiFi IP first
            if (isWifiConnected() && wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    int ipAddress = wifiInfo.getIpAddress();
                    if (ipAddress != 0) {
                        return String.format("%d.%d.%d.%d",
                            (ipAddress & 0xff),
                            (ipAddress >> 8 & 0xff),
                            (ipAddress >> 16 & 0xff),
                            (ipAddress >> 24 & 0xff));
                    }
                }
            }
            
            // Fallback: iterate through network interfaces
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                        String hostAddress = address.getHostAddress();
                        if (hostAddress != null && hostAddress.contains(".")) {
                            // IPv4 address
                            return hostAddress;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting local IP address", e);
        }
        
        return "unknown";
    }
    
    /**
     * Get public IP address (requires internet connection)
     * @return Public IP address string
     * @throws Exception if unable to retrieve public IP
     */
    public String getPublicIpAddress() throws Exception {
        String[] ipServices = {
            "https://api.ipify.org",
            "https://checkip.amazonaws.com",
            "https://icanhazip.com",
            "https://ipinfo.io/ip"
        };
        
        for (String service : ipServices) {
            try {
                URL url = new URL(service);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "RDM-Android-Client/1.0");
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String publicIp = reader.readLine();
                    reader.close();
                    connection.disconnect();
                    
                    if (publicIp != null && !publicIp.trim().isEmpty()) {
                        Log.d(TAG, "Public IP obtained from " + service + ": " + publicIp.trim());
                        return publicIp.trim();
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                Log.w(TAG, "Failed to get IP from " + service, e);
                // Continue to next service
            }
        }
        
        throw new Exception("Unable to retrieve public IP address");
    }
    
    /**
     * Get WiFi network name (SSID)
     * @return WiFi network name or "Unknown"
     */
    public String getWifiNetworkName() {
        try {
            if (isWifiConnected() && wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    String ssid = wifiInfo.getSSID();
                    if (ssid != null && !ssid.equals("<unknown ssid>")) {
                        // Remove quotes from SSID
                        return ssid.replace("\"", "");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting WiFi network name", e);
        }
        
        return "Unknown";
    }
    
    /**
     * Get WiFi signal strength
     * @return Signal strength description
     */
    public String getWifiSignalStrength() {
        try {
            if (isWifiConnected() && wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    int rssi = wifiInfo.getRssi();
                    int level = WifiManager.calculateSignalLevel(rssi, 5);
                    
                    String[] levels = {"Very Weak", "Weak", "Fair", "Good", "Excellent"};
                    return levels[level] + " (" + rssi + " dBm)";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting WiFi signal strength", e);
        }
        
        return "Unknown";
    }
    
    /**
     * Get network connection type details
     * @return Detailed network type string
     */
    public String getDetailedNetworkType() {
        try {
            if (connectivityManager == null) {
                return "Unknown";
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork == null) {
                    return "No Connection";
                }
                
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (capabilities == null) {
                    return "No Connection";
                }
                
                StringBuilder details = new StringBuilder();
                
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    details.append("WiFi");
                    String networkName = getWifiNetworkName();
                    if (!networkName.equals("Unknown")) {
                        details.append(" (").append(networkName).append(")");
                    }
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    details.append("Mobile Data");
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    details.append("Ethernet");
                } else {
                    details.append("Other");
                }
                
                // Add capabilities
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    details.append(" - Internet");
                }
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    details.append(" - Validated");
                }
                
                return details.toString();
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
                    return "No Connection";
                }
                
                return activeNetworkInfo.getTypeName() + " - " + activeNetworkInfo.getSubtypeName();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting detailed network type", e);
            return "Error";
        }
    }
}