package edu.usc.csci571.artsyapp

import android.app.Application
import android.util.Log
import edu.usc.csci571.artsyapp.network.RetrofitClient
import edu.usc.csci571.artsyapp.network.setAppContext
import edu.usc.csci571.artsyapp.session.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArtsyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("ArtsyApplication", "Initializing RetrofitClient with persistent cookies")
        
        // Set app context for use in UserSession
        setAppContext(applicationContext)
        
        // Initialize RetrofitClient with application context
        RetrofitClient.init(applicationContext)
        
        // Check if we have a valid session (in background)
        CoroutineScope(Dispatchers.IO).launch {
            UserSession.validateSession()
        }
    }
} 