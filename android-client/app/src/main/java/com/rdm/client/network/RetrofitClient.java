package com.rdm.client.network;

import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Retrofit client singleton for API communication
 * Handles HTTP client configuration and API service creation
 */
public class RetrofitClient {
    
    private static final String TAG = "RetrofitClient";
    
    // TODO: Replace with your actual backend URL
    private static final String BASE_URL = "https://your-backend-url.com/";
    
    // For local development, use:
    // private static final String BASE_URL = "http://10.0.2.2:3000/"; // Android emulator
    // private static final String BASE_URL = "http://192.168.1.100:3000/"; // Real device on same network
    
    private static RetrofitClient instance;
    private final ApiService apiService;
    
    /**
     * Private constructor to create Retrofit instance
     */
    private RetrofitClient() {
        // Create HTTP logging interceptor for debugging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> 
            Log.d(TAG, "HTTP: " + message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // Configure OkHttp client
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(chain -> {
                // Add common headers to all requests
                return chain.proceed(
                    chain.request()
                        .newBuilder()
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "application/json")
                        .addHeader("User-Agent", "RDM-Android-Client/1.0")
                        .build()
                );
            })
            .build();
        
        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
        
        // Create API service
        apiService = retrofit.create(ApiService.class);
        
        Log.d(TAG, "RetrofitClient initialized with base URL: " + BASE_URL);
    }
    
    /**
     * Get singleton instance of RetrofitClient
     * @return RetrofitClient instance
     */
    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }
    
    /**
     * Get API service for making requests
     * @return ApiService instance
     */
    public ApiService getApiService() {
        return apiService;
    }
    
    /**
     * Update base URL (useful for switching between environments)
     * @param newBaseUrl New base URL
     */
    public static void updateBaseUrl(String newBaseUrl) {
        Log.d(TAG, "Base URL updated to: " + newBaseUrl);
        // Force recreation of instance with new URL
        instance = null;
    }
}