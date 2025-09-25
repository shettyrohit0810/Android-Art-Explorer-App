package edu.usc.csci571.artsyapp.network

import android.content.Context
import java.lang.ref.WeakReference

private var appContextRef: WeakReference<Context>? = null

/**
 * Set the application context for use in non-Activity classes
 */
fun setAppContext(context: Context) {
    appContextRef = WeakReference(context.applicationContext)
}

/**
 * Get the application context (may be null)
 */
fun createAppContext(): Context? {
    return appContextRef?.get()
} 