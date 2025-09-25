package edu.usc.csci571.artsyapp.network

import edu.usc.csci571.artsyapp.models.LoginRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import org.json.JSONObject
import edu.usc.csci571.artsyapp.models.RegisterRequest
import android.util.Log


object AuthService {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    private const val BASE_URL = "https://rohit-hw-3.wl.r.appspot.com/api/auth"

    suspend fun login(request: LoginRequest): Response {
        val json = JSONObject().apply {
            put("email", request.email)
            put("password", request.password)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url("$BASE_URL/login")
            .post(body)
            .build()

        return client.newCall(httpRequest).execute()

    }

    suspend fun register(request: RegisterRequest): Response {
        val json = JSONObject().apply {
            put("fullname", request.fullname)
            put("email", request.email)
            put("password", request.password)
        }

        val bodyString = json.toString()
        Log.d("REGISTER_BODY", bodyString)

        val body = bodyString.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url("$BASE_URL/register")
            .post(body)
            .build()

        Log.d("REGISTER_URL", httpRequest.url.toString())

        return client.newCall(httpRequest).execute()
    }
}