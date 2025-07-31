package com.rdm.client.network;

import com.rdm.client.models.ApiResponse;
import com.rdm.client.models.DeviceInfo;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Retrofit API service interface
 * Defines all API endpoints for communication with backend
 */
public interface ApiService {
    
    /**
     * Register a new device with the backend
     * @param deviceInfo Device information to register
     * @return API response with registration result
     */
    @POST("api/register")
    Call<ApiResponse> registerDevice(@Body DeviceInfo deviceInfo);
    
    /**
     * Sync device data with backend
     * @param apiKey API key for authentication
     * @param deviceInfo Current device information and status
     * @return API response with sync result
     */
    @POST("api/sync")
    Call<ApiResponse> syncDeviceData(
        @Header("X-API-Key") String apiKey,
        @Body DeviceInfo deviceInfo
    );
    
    /**
     * Send heartbeat to keep device active
     * @param apiKey API key for authentication
     * @param deviceId Device identifier
     * @return API response with heartbeat result
     */
    @POST("api/heartbeat")
    Call<ApiResponse> sendHeartbeat(
        @Header("X-API-Key") String apiKey,
        @Body HeartbeatRequest deviceId
    );
    
    /**
     * Get device configuration from server
     * @param apiKey API key for authentication
     * @param deviceId Device identifier
     * @return API response with device configuration
     */
    @POST("api/config")
    Call<ApiResponse> getDeviceConfig(
        @Header("X-API-Key") String apiKey,
        @Body ConfigRequest deviceId
    );
    
    /**
     * Inner class for heartbeat requests
     */
    class HeartbeatRequest {
        private String device_id;
        
        public HeartbeatRequest(String deviceId) {
            this.device_id = deviceId;
        }
        
        public String getDevice_id() {
            return device_id;
        }
        
        public void setDevice_id(String device_id) {
            this.device_id = device_id;
        }
    }
    
    /**
     * Inner class for configuration requests
     */
    class ConfigRequest {
        private String device_id;
        
        public ConfigRequest(String deviceId) {
            this.device_id = deviceId;
        }
        
        public String getDevice_id() {
            return device_id;
        }
        
        public void setDevice_id(String device_id) {
            this.device_id = device_id;
        }
    }
}