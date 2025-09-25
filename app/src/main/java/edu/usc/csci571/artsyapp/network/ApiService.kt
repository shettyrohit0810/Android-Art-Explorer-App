package edu.usc.csci571.artsyapp.network

import edu.usc.csci571.artsyapp.model.Artist
import edu.usc.csci571.artsyapp.model.ArtistDetails
import edu.usc.csci571.artsyapp.model.Artwork
import edu.usc.csci571.artsyapp.model.GenesResponse
import edu.usc.csci571.artsyapp.model.Category
import edu.usc.csci571.artsyapp.model.FavoriteArtist
import edu.usc.csci571.artsyapp.model.FavoritesResponse
import edu.usc.csci571.artsyapp.model.UserProfileResponse
import edu.usc.csci571.artsyapp.session.FavoriteEntry
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import edu.usc.csci571.artsyapp.models.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import okhttp3.ResponseBody
import retrofit2.http.POST
import edu.usc.csci571.artsyapp.models.LoginRequest
import retrofit2.http.DELETE


interface ApiService {
    @GET("api/artists/search")
    suspend fun searchArtists(
        @Query("q") query: String
    ): List<Artist>

    @GET("api/artists/{id}")
    suspend fun getArtistDetails(
        @Path("id") artistId: String
    ): ArtistDetails

    @GET("api/artists/{id}/artworks")
    suspend fun getArtworksByArtist(
        @Path("id") artistId: String
    ): List<Artwork>

    /**
     * Get similar artists for a given artist ID
     */
    @GET("api/artists/{id}/similar")
    suspend fun getSimilarArtists(
        @Path("id") artistId: String
    ): List<Artist>

    /**
     * Return genes for an artwork in the `GenesResponse` wrapper
     */
    @GET("api/genes")
    suspend fun getGenes(
        @Query("artwork_id") artworkId: String
    ): GenesResponse

    /**
     * Legacy: returns flattened category list (optional)
     */
    @GET("api/artworks/{id}/genes")
    suspend fun getCategoriesByArtwork(
        @Path("id") artworkId: String
    ): List<Category>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ResponseBody>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ResponseBody>

    /**
     * Get the current user's profile information
     * Requires auth cookie to be present
     */
    @GET("api/auth/profile")
    suspend fun getUserProfile(): Response<UserProfileResponse>

    /**
     * Delete the current user's account
     * Requires auth cookie to be present
     */
    @DELETE("api/auth/delete")
    suspend fun deleteAccount(): Response<ResponseBody>

    /**
     * Get favorite artists with full details for the logged-in user
     * Ensures proper cookie handling for session management
     */
    @GET("api/favorites")
    suspend fun getFavorites(): FavoritesResponse

    /**
     * Add an artist to favorites
     * Uses query parameter for artistId as per backend implementation
     */
    @GET("api/favorites/add")
    suspend fun addFavorite(@Query("artistId") artistId: String): Response<ResponseBody>

    /**
     * Remove an artist from favorites
     * Uses query parameter for artistId as per backend implementation
     */
    @GET("api/favorites/remove")
    suspend fun removeFavorite(@Query("artistId") artistId: String): Response<ResponseBody>
}