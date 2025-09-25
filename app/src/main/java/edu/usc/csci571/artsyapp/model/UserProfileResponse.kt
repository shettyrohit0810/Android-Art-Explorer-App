package edu.usc.csci571.artsyapp.model

/**
 * Response model for the user profile endpoint
 */
data class UserProfileResponse(
    val id: String,
    val fullName: String,
    val email: String,
    val profileImageUrl: String?
) 