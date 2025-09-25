package edu.usc.csci571.artsyapp.model

import com.google.gson.annotations.SerializedName

/**
 * Represents the response from the favorites API endpoint
 */
data class FavoritesResponse(
    @SerializedName("favorites")
    val favorites: List<FavoriteArtist>,
    
    @SerializedName("message")
    val message: String = ""
) 