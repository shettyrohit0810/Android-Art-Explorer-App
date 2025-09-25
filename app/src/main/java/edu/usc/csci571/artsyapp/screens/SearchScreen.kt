package edu.usc.csci571.artsyapp.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import edu.usc.csci571.artsyapp.model.Artist
import edu.usc.csci571.artsyapp.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import edu.usc.csci571.artsyapp.session.UserSession
import edu.usc.csci571.artsyapp.R
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    var searchText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var artistList by rememberSaveable { mutableStateOf<List<Artist>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    val isDark = isSystemInDarkTheme()
    val searchBarColor = if (isDark) Color(0xFF283C6C) else Color(0xFFDDE5FF)
    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor   = searchBarColor,
        unfocusedContainerColor = searchBarColor,
        disabledContainerColor  = searchBarColor,
        focusedIndicatorColor   = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor  = Color.Transparent,
        focusedTextColor        = if (isDark) Color.White else Color.Black,
        cursorColor             = if (isDark) Color.White else Color.Black
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) {
                Snackbar(
                    snackbarData = it,
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(4.dp)
                )
            }
        },
        topBar = {
            Surface(color = searchBarColor, shadowElevation = 4.dp) {
                TextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        if (it.text.length >= 3) {
                            isSearching = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    artistList = RetrofitClient.apiService.searchArtists(it.text)
                                } catch (e: Exception) {
                                    Log.e("Search", "Error: ${e.message}")
                                    artistList = emptyList()
                                } finally {
                                    isSearching = false
                                }
                            }
                        } else {
                            artistList = emptyList()
                            if (isSearching) isSearching = false
                        }
                    },
                    placeholder = {
                        Text(
                            "Search artists…",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            searchText = TextFieldValue("")
                            artistList = emptyList()
                            navController.popBackStack("home", inclusive = false)
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .focusRequester(focusRequester),
                    colors = textFieldColors,
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
    ) { innerPadding ->
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(artistList) { artist ->
                    ArtistCard(artist = artist, snackbarHostState = snackbarHostState) {
                        navController.navigate("artistDetail/${artist.id}")
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistCard(
    artist: Artist,
    snackbarHostState: SnackbarHostState,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val overlayColor = if (isDark) Color(0xFF283C6C).copy(alpha = 0.7f) else Color(0xFFE0EBFF).copy(alpha = 0.6f)
    val textColor = if (isDark) Color.White else Color.Black

    val coroutineScope = rememberCoroutineScope()
    val isLoggedIn = UserSession.currentUser != null
    val isFavorite by remember(artist.id) { derivedStateOf { artist.id in UserSession.favorites } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.height(180.dp)) {
            // artist image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artist.image)
                    .crossfade(true)
                    .build(),
                contentDescription = "Artist Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = rememberAsyncImagePainter(
                    model = R.drawable.artsy_logo,
                    contentScale = ContentScale.Fit
                )
            )

            // star toggle
            if (isLoggedIn) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    IconButton(
                        onClick = {
                            Log.d("ArtistCard", "Star clicked for artist ID: ${artist.id}, Name: ${artist.name}")
                            val added = UserSession.toggleFavorite(artist.id)
                            val message = if (added) "Added to favorites" else "Removed from favorites"
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Toggle Favorite",
                            tint = Color.Black
                        )
                    }
                }
            }

            // bottom overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(overlayColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        artist.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View Details",
                        tint = textColor
                    )
                }
            }
        }
    }
}