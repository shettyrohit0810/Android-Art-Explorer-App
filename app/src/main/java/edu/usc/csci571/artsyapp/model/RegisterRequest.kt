package edu.usc.csci571.artsyapp.models

data class RegisterRequest(
    val fullname: String,
    val email: String,
    val password: String
)
