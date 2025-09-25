package edu.usc.csci571.artsyapp.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import edu.usc.csci571.artsyapp.model.ArtistDetails
import edu.usc.csci571.artsyapp.model.FavoriteArtist
import edu.usc.csci571.artsyapp.network.RetrofitClient
import edu.usc.csci571.artsyapp.session.UserSession
import edu.usc.csci571.artsyapp.session.User
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.ParseException
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.CoroutineScope
import edu.usc.csci571.artsyapp.R
import androidx.compose.foundation.isSystemInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    showRegistrationSuccess: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val appBarColor = if (isDark) Color(0xFF283C6C) else Color(0xFFDDE5FF)
    val backgroundColor = if (isDark) Color.Black else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subtleTextColor = if (isDark) Color.LightGray else Color.DarkGray
    val headerColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFE0E0E0)
    val noFavoritesColor = if (isDark) Color(0xFF283C6C) else Color(0xFFE0EBFF)

    val user = UserSession.currentUser
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show registration success snackbar
    LaunchedEffect(showRegistrationSuccess) {
        if (showRegistrationSuccess) {
            snackbarHostState.showSnackbar(
                message = "Registered successfully",
                duration = SnackbarDuration.Short
            )
        }
    }

    // Get detailed favorites directly from UserSession
    val detailedFavorites by remember { derivedStateOf { UserSession.detailedFavorites } }
    
    // Track if profile menu is expanded
    var showProfileMenu by remember { mutableStateOf(false) }
    
    // Load favorites whenever the user changes
    LaunchedEffect(user) {
        if (user != null) {
            UserSession.loadFavorites()
        }
    }

    // Register a callback for favorite changes to get notified when they change
    DisposableEffect(Unit) {
        val callback = {
            // The callback will be triggered when favorites change
            // We don't need to do anything here as we're using derivedStateOf
        }
        
        UserSession.addOnFavoriteChangeListener(callback)
        
        onDispose {
            UserSession.removeOnFavoriteChangeListener(callback)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) {
                Snackbar(
                    snackbarData = it,
                    containerColor = if (isDark) Color(0xFF283C6C) else Color.Black,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(4.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Artist Search", letterSpacing = 1.sp, color = textColor) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarColor,
                    navigationIconContentColor = textColor,
                    actionIconContentColor = textColor
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("search") }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    
                    Box {
                        IconButton(onClick = { 
                            if (user == null) {
                                // When logged out, clicking profile icon navigates to login page
                                navController.navigate("login")
                            } else {
                                // When logged in, clicking profile icon shows dropdown menu
                                showProfileMenu = true
                            }
                        }) {
                            if (user == null) {
                                // Logged out: Show Person Outline Icon
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Person",
                                    modifier = Modifier.size(28.dp)
                                )
                            } else if (user.profileImageUrl != null) {
                                // Logged in and has profile image: Show user's profile image with Artsy logo placeholder
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = user.profileImageUrl,
                                        error = rememberAsyncImagePainter(R.drawable.artsy_logo),
                                        placeholder = rememberAsyncImagePainter(R.drawable.artsy_logo)
                                    ),
                                    contentDescription = "User Avatar",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                // Logged in but no profile image: Show Artsy logo
                                Image(
                                    painter = rememberAsyncImagePainter(R.drawable.artsy_logo),
                                    contentDescription = "User Profile",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                        
                        // Profile dropdown menu - only shown when user is logged in
                        DropdownMenu(
                            expanded = showProfileMenu && user != null,
                            onDismissRequest = { showProfileMenu = false }
                        ) {
                            // Divider()
                            
                            // Log out option
                            DropdownMenuItem(
                                text = { Text("Log out") },
                                onClick = {
                                    logOut(coroutineScope, navController, snackbarHostState)
                                    showProfileMenu = false
                                }
                            )
                            
                            // Delete account option (in red as shown in screenshot)
                            DropdownMenuItem(
                                text = { Text("Delete account", color = Color.Red) },
                                onClick = {
                                    showProfileMenu = false
                                    deleteAccount(coroutineScope, navController, snackbarHostState)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(innerPadding)
        ) {
            // Current date
            Text(
                text = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Favorites header
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    "Favorites",
                    Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = textColor
                )
            }

            Spacer(Modifier.height(24.dp))

            if (user == null) {
                // Prompt to log in
                Button(
                    onClick = { navController.navigate("login") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFA6C4FC) else Color(0xFF34446C)
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        "Log in to see favorites",
                        color = if (isDark) Color.Black else Color.White
                    )
                }
            } else {
                when {
                    detailedFavorites.isEmpty() -> {
                        // If logged in but no favorites, display "No favorites"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(0.98f) // Make this wider to match the image
                                    .align(Alignment.Center),
                                shape = RoundedCornerShape(8.dp),
                                color = noFavoritesColor,
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    "No favorites",
                                    modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .fillMaxWidth(), // Ensure text also fills width for centering
                                    style = MaterialTheme.typography.titleMedium,
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        // Sort favorites by timestamp, newest first
                        val sortedFavorites = remember(detailedFavorites) {
                            detailedFavorites.sortedByDescending { 
                                try {
                                    val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                                    apiFormat.timeZone = TimeZone.getTimeZone("UTC")
                                    apiFormat.parse(it.addedAt)?.time ?: 0L
                                } catch (e: ParseException) {
                                    0L
                                }
                            }
                        }
                        
                        // Render favorite rows
                        LazyColumn(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            items(sortedFavorites) { favoriteArtist ->
                                FavoriteRow(favoriteArtist, navController)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Powered by Artsy link
            Text(
                "Powered by Artsy",
                fontStyle = FontStyle.Italic,
                color = subtleTextColor,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.artsy.net/"))
                        )
                    }
            )
        }
    }
}

// Function to handle logout
private fun logOut(
    coroutineScope: CoroutineScope,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    UserSession.clear() // This clears the session and cookies
    coroutineScope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(
            message = "Log out successfully",
            duration = SnackbarDuration.Short
        )
        // Removed navigation to login page to stay on home screen
    }
}

// Function to handle account deletion
private fun deleteAccount(
    coroutineScope: CoroutineScope,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    coroutineScope.launch(Dispatchers.IO) {
        try {
            // Call the backend API to delete the account
            val response = RetrofitClient.apiService.deleteAccount()
            
            Log.d("HomeScreen", "Delete account response: ${response.code()}")
            
            if (response.isSuccessful) {
                // Account deleted successfully, clear local session data
                UserSession.clear()
                
                // Show success message to user on main thread using Snackbar
                launch(Dispatchers.Main) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = "Account deleted successfully",
                        duration = SnackbarDuration.Short
                    )
                }
                // Stay on home page (already logged out due to UserSession.clear())
                // No navigation needed as UI will recompose to logged-out state
            } else {
                // Handle error response
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("HomeScreen", "Error deleting account: $errorBody")
                
                // Show error message to user on main thread
                launch(Dispatchers.Main) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = "Failed to delete account: $errorBody",
                        duration = SnackbarDuration.Long
                    )
                }
                
                // Even if the API call fails, still clear the local session since
                // the token might be invalid/expired
                RetrofitClient.clearCookies()
                UserSession.clear()
                // No navigation needed
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Exception deleting account", e)
            
            // Show error message to user on main thread
            launch(Dispatchers.Main) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = "Error: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            }
            
            // Clear local session on error
            RetrofitClient.clearCookies()
            UserSession.clear()
            // No navigation needed
        }
    }
}

@Composable
private fun FavoriteRow(
    favoriteArtist: FavoriteArtist,
    navController: NavController
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black
    val subtleTextColor = if (isDark) Color.LightGray else Color.Gray

    // Parse the timestamp from the API or use current time if parsing fails
    val timestamp = try {
        val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        apiFormat.timeZone = TimeZone.getTimeZone("UTC")
        apiFormat.parse(favoriteArtist.addedAt)?.time ?: 0L
    } catch (e: ParseException) {
        System.currentTimeMillis()
    }
    
    // State for the relative time that will be updated
    var relativeTime by remember { mutableStateOf("") }
    
    // Function to calculate relative time
    fun calculateRelativeTime(): String {
        val now = System.currentTimeMillis()
        val diffInSeconds = (now - timestamp) / 1000
        
        return when {
            diffInSeconds < 60 -> "$diffInSeconds seconds ago"
            diffInSeconds < 3600 -> "${diffInSeconds / 60} minutes ago"
            diffInSeconds < 86400 -> "${diffInSeconds / 3600} hours ago"
            else -> "${diffInSeconds / 86400} days ago"
        }
    }
    
    // Update the time every second using LaunchedEffect
    LaunchedEffect(timestamp) {
        while (true) {
            relativeTime = calculateRelativeTime()
            kotlinx.coroutines.delay(1000) // Update every second
        }
    }
    
    val isFavorite by remember(favoriteArtist.id) { derivedStateOf { favoriteArtist.id in UserSession.favorites } }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("artistDetail/${favoriteArtist.id}") }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Artist info
        Column(modifier = Modifier.weight(1f)) {
            Text(favoriteArtist.name, fontWeight = FontWeight.Bold, color = textColor)
            Text(
                "${favoriteArtist.nationality}, ${favoriteArtist.years}",
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
        
        // Live relative time
        Text(
            text = relativeTime,
            style = MaterialTheme.typography.bodySmall,
            color = subtleTextColor,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        // Arrow to navigate
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "View Details",
            tint = subtleTextColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreen_Preview() {
    // Seed a fake user
    UserSession.currentUser = User(
        id = "preview",
        fullName = "Preview User",
        email = "preview@example.com",
        profileImageUrl = null
    )
    
    // Mock UI without network calls for preview
    Scaffold {
        Column(Modifier.padding(it)) {
            Text(
                text = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0E0E0))
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    "Favorites",
                    Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            
            Text(
                "No favorites",
                modifier = Modifier
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
                    .wrapContentWidth()
                    .background(Color(0xFFE0EBFF))
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
