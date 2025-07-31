package com.rdm.client.models;

import com.google.gson.annotations.SerializedName;

/**
 * Device information model for API communication
 */
public class DeviceInfo {
    
    @SerializedName("device_id")
    private String deviceId;
    
    @SerializedName("model")
    private String model;
    
    @SerializedName("manufacturer")
    private String manufacturer;
    
    @SerializedName("os_version")
    private String osVersion;
    
    @SerializedName("app_version")
    private String appVersion;
    
    @SerializedName("battery_level")
    private int batteryLevel;
    
    @SerializedName("network_status")
    private String networkStatus;
    
    @SerializedName("latitude")
    private double latitude;
    
    @SerializedName("longitude")
    private double longitude;
    
    @SerializedName("local_ip")
    private String localIp;
    
    @SerializedName("public_ip")
    private String publicIp;
    
    @SerializedName("timestamp")
    private long timestamp;
    
    // Constructors
    public DeviceInfo() {
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getManufacturer() {
        return manufacturer;
    }
    
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
    
    public String getOsVersion() {
        return osVersion;
    }
    
    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }
    
    public String getAppVersion() {
        return appVersion;
    }
    
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
    
    public int getBatteryLevel() {
        return batteryLevel;
    }
    
    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }
    
    public String getNetworkStatus() {
        return networkStatus;
    }
    
    public void setNetworkStatus(String networkStatus) {
        this.networkStatus = networkStatus;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public String getLocalIp() {
        return localIp;
    }
    
    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }
    
    public String getPublicIp() {
        return publicIp;
    }
    
    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "DeviceInfo{" +
                "deviceId='" + deviceId + '\'' +
                ", model='" + model + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", osVersion='" + osVersion + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", batteryLevel=" + batteryLevel +
                ", networkStatus='" + networkStatus + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", localIp='" + localIp + '\'' +
                ", publicIp='" + publicIp + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}