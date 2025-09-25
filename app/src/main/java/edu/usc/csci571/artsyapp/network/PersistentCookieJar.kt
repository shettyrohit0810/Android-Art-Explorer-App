package edu.usc.csci571.artsyapp.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.io.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A persistent cookie jar that stores cookies in SharedPreferences to maintain
 * login sessions across app restarts.
 */
class PersistentCookieJar(private val context: Context) : CookieJar {
    private val TAG = "PersistentCookieJar"
    private val cookiePrefs: SharedPreferences = context.getSharedPreferences(COOKIE_PREFS, Context.MODE_PRIVATE)
    
    // In-memory cache of cookies
    private val cookieMap: MutableMap<String, List<Cookie>> = ConcurrentHashMap()
    
    init {
        // Load cookies from SharedPreferences into memory
        loadFromSharedPreferences()
    }
    
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val cookiesForHost = ArrayList(cookies)
        
        Log.d(TAG, "Saving ${cookies.size} cookies for host: $host")
        cookies.forEach { cookie ->
            Log.d(TAG, "Cookie: ${cookie.name}=${cookie.value}, expires: ${cookie.expiresAt}, persistent: ${cookie.persistent}")
        }
        
        // Save to memory
        cookieMap[host] = cookiesForHost
        
        // Save to SharedPreferences
        saveToDisk(host, cookiesForHost)
    }
    
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = cookieMap[host] ?: emptyList()
        
        Log.d(TAG, "Loading ${cookies.size} cookies for request to: $url")
        
        // Filter out expired cookies
        val validCookies = cookies.filter { cookie ->
            val isValid = !cookie.hasExpired()
            if (!isValid) {
                Log.d(TAG, "Cookie ${cookie.name} has expired, removing")
            }
            isValid
        }
        
        // If some cookies were expired, update the cache
        if (validCookies.size < cookies.size) {
            cookieMap[host] = validCookies
            saveToDisk(host, validCookies)
        }
        
        return validCookies
    }
    
    /**
     * Load cookies from SharedPreferences into memory
     */
    private fun loadFromSharedPreferences() {
        Log.d(TAG, "Loading cookies from SharedPreferences")
        
        cookieMap.clear()
        
        val allCookies = cookiePrefs.all
        for ((host, serializedCookieList) in allCookies) {
            val cookies = deserializeCookies(serializedCookieList as String)
            cookieMap[host] = cookies
            Log.d(TAG, "Loaded ${cookies.size} cookies for host: $host")
        }
    }
    
    /**
     * Save cookies for a host to SharedPreferences
     */
    private fun saveToDisk(host: String, cookies: List<Cookie>) {
        val editor = cookiePrefs.edit()
        if (cookies.isEmpty()) {
            // If empty, remove the entry
            editor.remove(host)
            Log.d(TAG, "Removed cookies for host: $host")
        } else {
            // Serialize and save
            val serializedCookies = serializeCookies(cookies)
            editor.putString(host, serializedCookies)
            Log.d(TAG, "Saved ${cookies.size} cookies for host: $host")
        }
        editor.apply()
    }
    
    /**
     * Clear all cookies from both memory and disk
     */
    fun clearAll() {
        Log.d(TAG, "Clearing all cookies")
        cookieMap.clear()
        cookiePrefs.edit().clear().apply()
    }
    
    /**
     * Serialize a list of cookies to a string
     */
    private fun serializeCookies(cookies: List<Cookie>): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        
        // Convert cookies to serializable format
        val serializableCookies = cookies.map { SerializableCookie(it) }
        
        objectOutputStream.writeObject(serializableCookies)
        return byteArrayOutputStream.toByteArray().encodeToBase64()
    }
    
    /**
     * Deserialize a string to a list of cookies
     */
    private fun deserializeCookies(serializedCookies: String): List<Cookie> {
        try {
            val bytes = serializedCookies.decodeBase64()
            val byteArrayInputStream = ByteArrayInputStream(bytes)
            val objectInputStream = ObjectInputStream(byteArrayInputStream)
            
            @Suppress("UNCHECKED_CAST")
            val serializableCookies = objectInputStream.readObject() as List<SerializableCookie>
            
            return serializableCookies.mapNotNull { it.cookie }
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing cookies", e)
            return emptyList()
        }
    }
    
    /**
     * Extension to check if a cookie has expired
     */
    private fun Cookie.hasExpired(): Boolean {
        return expiresAt < System.currentTimeMillis()
    }
    
    /**
     * Encode ByteArray to Base64 string
     */
    private fun ByteArray.encodeToBase64(): String {
        return android.util.Base64.encodeToString(this, android.util.Base64.DEFAULT)
    }
    
    /**
     * Decode Base64 string to ByteArray
     */
    private fun String.decodeBase64(): ByteArray {
        return android.util.Base64.decode(this, android.util.Base64.DEFAULT)
    }
    
    /**
     * A serializable wrapper for Cookie
     */
    private class SerializableCookie(originalCookie: Cookie) : Serializable {
        private val TAG = "SerializableCookie"
        
        // Store cookie properties as serializable fields
        private val name: String = originalCookie.name
        private val value: String = originalCookie.value
        private val expiresAt: Long = originalCookie.expiresAt
        private val domain: String = originalCookie.domain
        private val path: String = originalCookie.path
        private val secure: Boolean = originalCookie.secure
        private val httpOnly: Boolean = originalCookie.httpOnly
        private val hostOnly: Boolean = originalCookie.hostOnly
        
        // Transient (non-serialized) field for the actual cookie
        @Transient
        var cookie: Cookie? = null
        
        init {
            // Create the cookie immediately
            cookie = createCookie()
        }
        
        companion object {
            private const val serialVersionUID = 1L
        }
        
        private fun createCookie(): Cookie {
            val builder = Cookie.Builder()
                .name(name)
                .value(value)
                .expiresAt(expiresAt)
                .path(path)
            
            if (hostOnly) {
                builder.hostOnlyDomain(domain)
            } else {
                builder.domain(domain)
            }
            
            if (secure) builder.secure()
            if (httpOnly) builder.httpOnly()
            
            return builder.build()
        }
        
        // Called during deserialization
        private fun readObject(input: ObjectInputStream) {
            input.defaultReadObject()
            cookie = createCookie()
        }
    }
    
    companion object {
        private const val COOKIE_PREFS = "artsyapp_cookies"
    }
} 