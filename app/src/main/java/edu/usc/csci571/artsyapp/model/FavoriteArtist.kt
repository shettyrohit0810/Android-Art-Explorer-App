package edu.usc.csci571.artsyapp.model

import com.google.gson.annotations.SerializedName

/**
 * Represents an enriched favorite artist returned from the API
 */
data class FavoriteArtist(
    @SerializedName("artistId")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("nationality")
    val nationality: String,
    
    @SerializedName("years")
    val years: String,
    
    @SerializedName("addedAt")
    val addedAt: String,
    
    @SerializedName("thumbnail")
    val image: String
) 