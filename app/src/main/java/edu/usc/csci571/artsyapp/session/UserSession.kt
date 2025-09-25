// app/src/main/java/edu/usc/csci571/artsyapp/session/UserSession.kt
package edu.usc.csci571.artsyapp.session

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.usc.csci571.artsyapp.model.FavoriteArtist
import edu.usc.csci571.artsyapp.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.File
import android.content.Context
import edu.usc.csci571.artsyapp.network.createAppContext
import java.io.FileNotFoundException

/**
 * A simple in‑memory session store.
 * Holds the currently logged‑in user (or null) and a list of favorite artist IDs.
 */
data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val profileImageUrl: String? = null
)

// Used for favorites with timestamps in memory
data class FavoriteEntry(
    val id: String,
    val addedAt: Long
)

object UserSession {
    // who's logged in (null = logged out)
    var currentUser by mutableStateOf<User?>(null)
        internal set

    // the single source of truth for favorite IDs
    private val _favoriteIds = mutableStateListOf<String>()
    val favorites: List<String> get() = _favoriteIds
    
    // Detailed favorites data - for home screen display
    private val _detailedFavorites = mutableStateListOf<FavoriteArtist>()
    val detailedFavorites: List<FavoriteArtist> get() = _detailedFavorites
    
    // Coroutine scope for network operations
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Flag to prevent concurrent loadFavorites calls
    private var isLoadingFavorites = false
    
    // Callback for syncing favorite changes, for UI refresh
    private var onFavoriteChangeCallbacks = mutableListOf<() -> Unit>()
    
    /**
     * Save the current user data to a file for persistence
     */
    private fun saveUserDataToStorage() {
        val appContext = createAppContext()
        if (appContext == null) {
            Log.e("UserSession", "Cannot save user data: appContext is null")
            return
        }
        
        currentUser?.let { user ->
            try {
                val userData = JSONObject().apply {
                    put("id", user.id)
                    put("fullName", user.fullName)
                    put("email", user.email)
                    put("profileImageUrl", user.profileImageUrl ?: "")
                }
                
                Log.d("UserSession", "Saving user data to storage: ${userData}")
                Log.d("UserSession", "Profile image URL being saved: ${user.profileImageUrl}")
                
                appContext.openFileOutput("user_data.json", Context.MODE_PRIVATE).use { stream ->
                    stream.write(userData.toString().toByteArray())
                }
            } catch (e: Exception) {
                Log.e("UserSession", "Error saving user data", e)
            }
        }
    }
    
    /**
     * Load user data from storage
     */
    private fun loadUserDataFromStorage(): User? {
        val appContext = createAppContext()
        if (appContext == null) {
            Log.e("UserSession", "Cannot load user data: appContext is null")
            return null
        }
        
        return try {
            appContext.openFileInput("user_data.json").bufferedReader().use { reader ->
                val content = reader.readText()
                val userData = JSONObject(content)
                
                val profileImageUrl = userData.optString("profileImageUrl", null)
                    .takeIf { !it.isNullOrBlank() && it != "null" }
                
                Log.d("UserSession", "Loaded user data from storage: $content")
                Log.d("UserSession", "Profile image URL loaded: $profileImageUrl")
                
                User(
                    id = userData.getString("id"),
                    fullName = userData.getString("fullName"),
                    email = userData.getString("email"),
                    profileImageUrl = profileImageUrl
                )
            }
        } catch (e: FileNotFoundException) {
            Log.d("UserSession", "No saved user data found")
            null
        } catch (e: Exception) {
            Log.e("UserSession", "Error loading user data", e)
            null
        }
    }
    
    /**
     * Validates if the user has a valid session from persistent cookies
     * Called when the app starts
     */
    suspend fun validateSession() {
        Log.d("UserSession", "Validating session from cookies")
        
        try {
            // Try to load user from local storage first (for immediate UI update)
            val savedUser = loadUserDataFromStorage()
            if (savedUser != null) {
                Log.d("UserSession", "Restored user from local storage: ${savedUser.fullName}, ${savedUser.email}")
                Log.d("UserSession", "Profile image URL from storage: ${savedUser.profileImageUrl}")
                
                // Set the current user from saved data using the setter method
                updateUser(savedUser)
            }
            
            // Now check if session is actually valid by trying to load favorites
            val favoritesResponse = RetrofitClient.apiService.getFavorites()
            
            if (favoritesResponse.favorites.isNotEmpty() || favoritesResponse.message == "success") {
                Log.d("UserSession", "Found valid session! User is logged in from persistent cookies")
                
                // Update favorites from response
                _detailedFavorites.clear()
                _detailedFavorites.addAll(favoritesResponse.favorites)
                
                _favoriteIds.clear()
                _favoriteIds.addAll(favoritesResponse.favorites.map { it.id })
                
                Log.d("UserSession", "Restored session with ${_favoriteIds.size} favorites")
                notifyFavoriteChange()
            } else {
                Log.d("UserSession", "No valid session found from cookies")
                // Clear any locally saved user data if session is invalid
                updateUser(null)
            }
        } catch (e: Exception) {
            Log.e("UserSession", "Error validating session", e)
            
            // Keep the user data from storage if we couldn't validate with the server
            // This gives a better UX as the user appears logged in while offline
            if (currentUser == null) {
                // Try to get user data from storage as fallback
                val storedUser = loadUserDataFromStorage()
                if (storedUser != null) {
                    updateUser(storedUser)
                    Log.d("UserSession", "Using stored user data as fallback due to network error")
                    Log.d("UserSession", "Profile URL from fallback: ${currentUser?.profileImageUrl}")
                }
            }
        }
    }
    
    /**
     * Clear saved user data file
     */
    private fun clearSavedUserData() {
        val appContext = createAppContext()
        if (appContext == null) {
            Log.e("UserSession", "Cannot clear user data: appContext is null")
            return
        }
        
        try {
            val file = File(appContext.filesDir, "user_data.json")
            if (file.exists()) {
                file.delete()
                Log.d("UserSession", "Cleared saved user data")
            }
        } catch (e: Exception) {
            Log.e("UserSession", "Error clearing user data", e)
        }
    }
    
    /**
     * Adds a callback that will be invoked whenever favorites are changed
     */
    fun addOnFavoriteChangeListener(callback: () -> Unit) {
        onFavoriteChangeCallbacks.add(callback)
    }
    
    /**
     * Removes a previously registered callback
     */
    fun removeOnFavoriteChangeListener(callback: () -> Unit) {
        onFavoriteChangeCallbacks.remove(callback)
    }
    
    /**
     * Notify all listeners that favorites have changed
     */
    private fun notifyFavoriteChange() {
        onFavoriteChangeCallbacks.forEach { it() }
    }

    /**
     * Loads favorites from the API and updates the local state
     * @param retryCount Number of retries if network request fails
     */
    fun loadFavorites(retryCount: Int = 2) {
        if (currentUser == null) {
            Log.d("UserSession", "loadFavorites: No user logged in, clearing local favorites.")
            _favoriteIds.clear()
            _detailedFavorites.clear()
            notifyFavoriteChange()
            return
        }
        
        // Prevent concurrent calls to loadFavorites
        if (isLoadingFavorites) {
            Log.d("UserSession", "loadFavorites: Already loading favorites, skipping.")
            return
        }
        
        isLoadingFavorites = true
        Log.d("UserSession", "loadFavorites: Called for user ${currentUser?.email}")
        
        scope.launch {
            try {
                // apiService.getFavorites() returns FavoritesResponse directly
                val favoritesResponse = RetrofitClient.apiService.getFavorites()
                
                // Print detailed info about the response
                val artistIds = favoritesResponse.favorites.map { it.id }
                Log.d("UserSession", "GetFavorites API Response: Got ${favoritesResponse.favorites.size} favorites.")
                Log.d("UserSession", "Favorite IDs received: $artistIds")
                
                _detailedFavorites.clear()
                _detailedFavorites.addAll(favoritesResponse.favorites)
                
                _favoriteIds.clear()
                _favoriteIds.addAll(favoritesResponse.favorites.map { it.id })
                
                Log.d("UserSession", "Updated local state - IDs: ${_favoriteIds.size}, Details: ${_detailedFavorites.size}")
                notifyFavoriteChange()
            } catch (e: Exception) {
                Log.e("UserSession", "Exception while loading favorites", e)
                
                // Retry logic for transient network issues
                if (retryCount > 0) {
                    Log.d("UserSession", "Retrying loadFavorites in 1 second (${retryCount} retries left)")
                    delay(1000) // Wait 1 second before retry
                    isLoadingFavorites = false
                    loadFavorites(retryCount - 1)
                    return@launch
                }
                
                // Clear local state on error to avoid showing stale data
                _favoriteIds.clear()
                _detailedFavorites.clear()
                notifyFavoriteChange()
            } finally {
                isLoadingFavorites = false
            }
        }
    }

    /** 
     * Toggles an artist ID in the favorites set and syncs with API.
     * Returns true if the artist was added, false if removed.
     */
    fun toggleFavorite(artistId: String): Boolean {
        Log.d("UserSession", "toggleFavorite CALLED with artistId: $artistId. CurrentUser: ${currentUser?.email}")
        if (currentUser == null) {
            Log.w("UserSession", "toggleFavorite: No user logged in, returning false.")
            return false
        }

        val isCurrentlyFavorite = _favoriteIds.contains(artistId)
        val adding = !isCurrentlyFavorite

        Log.d("UserSession", "toggleFavorite: ArtistID: $artistId, IsCurrentlyFavorite: $isCurrentlyFavorite, Action: ${if (adding) "Add" else "Remove"}")

        // Optimistic UI update
        if (adding) {
            _favoriteIds.add(artistId)
            Log.d("UserSession", "Optimistic add: _favoriteIds now contains $artistId. Size: ${_favoriteIds.size}")
        } else {
            _favoriteIds.remove(artistId)
            _detailedFavorites.removeAll { it.id == artistId }
            Log.d("UserSession", "Optimistic remove: _favoriteIds no longer contains $artistId. Size: ${_favoriteIds.size}")
        }
        notifyFavoriteChange() 
        Log.d("UserSession", "notifyFavoriteChange() called after optimistic update.")

        scope.launch {
            try {
                val apiResponse = if (adding) {
                    RetrofitClient.apiService.addFavorite(artistId)
                } else {
                    RetrofitClient.apiService.removeFavorite(artistId)
                }

                val bodyStr = apiResponse.body()?.string() ?: ""
                val errorStr = apiResponse.errorBody()?.string() ?: ""
                
                Log.d("UserSession", "Toggle API for $artistId (${if (adding) "Add" else "Remove"}): " +
                       "Code=${apiResponse.code()}, isSuccess=${apiResponse.isSuccessful}, " +
                       "Body=${bodyStr}, Error=${errorStr}")

                if (apiResponse.isSuccessful) {
                    Log.d("UserSession", "Toggle successful on server for $artistId.")
                    // Always reload favorites after a successful toggle to ensure
                    // we have the most up-to-date state from the server
                    loadFavorites()
                } else {
                    Log.e("UserSession", "Toggle API FAILED for $artistId: Code=${apiResponse.code()}, ErrorBody=${errorStr}")
                    // Revert optimistic update
                    if (adding) {
                        _favoriteIds.remove(artistId)
                    } else {
                        // If remove failed, add ID back
                        _favoriteIds.add(artistId) 
                    }
                    notifyFavoriteChange() // Notify after reverting
                    // Try to reload favorites to ensure consistency
                    loadFavorites()
                }
            } catch (e: Exception) {
                Log.e("UserSession", "Exception during toggleFavorite API call for $artistId", e)
                // Revert optimistic update on generic network error
                if (adding) {
                    _favoriteIds.remove(artistId)
                } else {
                    _favoriteIds.add(artistId)
                }
                notifyFavoriteChange()
                // Always reload from server on unexpected error to resync state
                loadFavorites()
            }
        }
        return adding
    }

    /** Clears everything in the session (logout). */
    fun clear() {
        currentUser = null
        clearSavedUserData()
        _favoriteIds.clear()
        _detailedFavorites.clear()
        notifyFavoriteChange()
    }

    /**
     * Update user info - call this when user logs in or needs to be updated
     */
    fun updateUser(user: User?) {
        when (user) {
            null -> {
                currentUser = null
                clearSavedUserData()
            }
            else -> {
                Log.d("UserSession", "Setting current user: ${user.fullName}, ${user.email}")
                Log.d("UserSession", "Profile image URL: ${user.profileImageUrl}")
                currentUser = user
                saveUserDataToStorage()
            }
        }
    }
}
