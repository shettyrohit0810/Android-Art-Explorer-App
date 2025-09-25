package edu.usc.csci571.artsyapp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.JavaNetCookieJar
import java.net.CookieManager
import java.net.CookiePolicy
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import android.content.Context

object RetrofitClient {
    private const val BASE_URL = "https://rohit-hw-3.wl.r.appspot.com/"
    private const val TAG = "RetrofitClient"

    private lateinit var persistentCookieJar: PersistentCookieJar
    private lateinit var apiServiceInstance: ApiService

    // Cookie monitoring interceptor
    private val cookieMonitoringInterceptor = Interceptor { chain ->
        val request = chain.request()
        Log.d(TAG, "🍪 Request: ${request.url} with cookies: ${request.headers("Cookie")}")
        
        val response = chain.proceed(request)
        
        // Log received cookies
        val cookies = response.headers("Set-Cookie")
        if (cookies.isNotEmpty()) {
            Log.d(TAG, "🍪 Response from ${request.url} with Set-Cookie: $cookies")
        }
        
        response
    }

    private fun createClient(context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // Initialize persistent cookie jar
        persistentCookieJar = PersistentCookieJar(context)
        
        return OkHttpClient.Builder()
            // Use persistent cookie jar
            .cookieJar(persistentCookieJar)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(cookieMonitoringInterceptor)
            // Add reasonable timeouts
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun init(context: Context) {
        val client = createClient(context)
        apiServiceInstance = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }

    val apiService: ApiService
        get() {
            if (!::apiServiceInstance.isInitialized) {
                throw IllegalStateException("RetrofitClient must be initialized with context first. Call RetrofitClient.init(context)")
            }
            return apiServiceInstance
        }
    
    /**
     * Clear all saved cookies (used during logout)
     */
    fun clearCookies() {
        if (::persistentCookieJar.isInitialized) {
            persistentCookieJar.clearAll()
        }
    }
}
