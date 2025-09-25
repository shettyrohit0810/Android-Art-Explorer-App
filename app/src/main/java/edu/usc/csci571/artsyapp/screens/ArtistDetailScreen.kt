package edu.usc.csci571.artsyapp.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import edu.usc.csci571.artsyapp.model.*
import edu.usc.csci571.artsyapp.network.RetrofitClient
import edu.usc.csci571.artsyapp.session.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import edu.usc.csci571.artsyapp.R
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

@Composable
private fun LoadingIndicatorWithText(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 100.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(
            "Loading...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: String,
    navController: NavController
) {
    val isDark = isSystemInDarkTheme()
    val appBarColor   = if (isDark) Color(0xFF283C6C) else Color(0xFFDDE5FF)
    val contentColor  = if (isDark) Color.White else Color.Black
    val tabIconColor  = if (isDark) Color(0xFF9AA8CD) else Color(0xFF0A1F44)

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val isLoggedIn = UserSession.currentUser != null
    val isFavorite by remember { derivedStateOf { artistId in UserSession.favorites } }

    var details by remember { mutableStateOf<ArtistDetails?>(null) }
    var artworks by remember { mutableStateOf<List<Artwork>>(emptyList()) }
    var similarArtists by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var loading  by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }

    // Register a callback for favorite changes to refresh UI
    DisposableEffect(artistId) {
        val callback = {
            // The favorite status might have changed, UI will update via isFavorite
        }
        
        UserSession.addOnFavoriteChangeListener(callback)
        
        onDispose {
            UserSession.removeOnFavoriteChangeListener(callback)
        }
    }

    // Fetch artist, artworks & similar artists
    LaunchedEffect(artistId) {
        launch(Dispatchers.IO) {
            try {
                details = RetrofitClient.apiService.getArtistDetails(artistId)
                artworks = RetrofitClient.apiService.getArtworksByArtist(artistId)
                
                // If user is logged in, fetch similar artists
                if (isLoggedIn) {
                    try {
                        similarArtists = RetrofitClient.apiService.getSimilarArtists(artistId)
                    } catch (e: Exception) {
                        Log.e("ArtistDetail", "Error loading similar artists: ${e.message}")
                        similarArtists = emptyList()
                    }
                }
            } catch (e:Exception) {
                Log.e("ArtistDetail","load failed",e)
            } finally {
                loading = false
            }
        }
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
            TopAppBar(
                title = { Text(details?.name ?: "Artist", color = contentColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = contentColor)
                    }
                },
                actions = {
                    if (isLoggedIn) {
                        IconButton(onClick = {
                            val added = UserSession.toggleFavorite(artistId)
                            val message = if (added) "Added to favorites" else "Removed from favorites"
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (isFavorite) Color.Black else contentColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarColor,
                    navigationIconContentColor = contentColor,
                    titleContentColor = contentColor,
                    actionIconContentColor = contentColor
                )
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isDark) Color.Black else Color.White)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = if (isDark) Color.Black else Color.White,
                indicator = { positions ->
                    TabRowDefaults.Indicator(
                        Modifier
                            .tabIndicatorOffset(positions[selectedTab])
                            .height(2.dp),
                        color = tabIconColor
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.Info, contentDescription = null, tint = tabIconColor) },
                    text = { Text("Details", color = tabIconColor) },
                    modifier = Modifier.weight(1f)
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.AccountBox, contentDescription = null, tint = tabIconColor) },
                    text = { Text("Artworks", color = tabIconColor) },
                    modifier = Modifier.weight(1f)
                )
                if (isLoggedIn) {
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { 
                            Icon(
                                imageVector = Icons.Outlined.Person, 
                                contentDescription = null, 
                                tint = tabIconColor
                            ) 
                        },
                        text = { Text("Similar", color = tabIconColor) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (loading) {
                LoadingIndicatorWithText(modifier = Modifier.fillMaxSize())
            } else {
                when (selectedTab) {
                    0 -> DetailsTab(details!!)
                    1 -> ArtworksTab(artworks)
                    2 -> if (isLoggedIn) SimilarTab(similarArtists, navController)
                }
            }
        }
    }
}

@Composable
private fun DetailsTab(artist: ArtistDetails) {
    val isDark = isSystemInDarkTheme()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(if (isDark) Color.Black else Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            artist.name,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (isDark) Color.White else Color.Black,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${artist.nationality}, ${artist.years}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (isDark) Color.White else Color.Black,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Text(
            artist.biography,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 26.sp,
            color = if (isDark) Color.White else Color.Black,
            textAlign = TextAlign.Justify
        )
    }
}

@Composable
private fun ArtworksTab(artworks: List<Artwork>) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var loadingCats by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var page by remember { mutableStateOf(0) }
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF283C6C) else Color(0xFFDDE5FF)
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(artworks) {
        isLoading = true
        // Simulate network delay for smoother transition
        kotlinx.coroutines.delay(500)
        isLoading = false
    }

    if (artworks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color.Black else Color.White),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .padding(top = 32.dp),
                shape = RoundedCornerShape(8.dp),
                color = backgroundColor,
                shadowElevation = 0.dp
            ) {
                Text(
                    "No Artworks",
                    modifier = Modifier
                        .padding(vertical = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDark) Color.White else Color.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color.Black else Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = if (isDark) Color(0xFFA6C4FC) else Color(0xFF34446C)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Loading...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDark) Color.White else Color.Black
                )
            }
        }
    } else {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color.Black else Color.White),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(artworks) { art ->
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = art.image,
                        contentDescription = art.title,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                        Text(
                            "${art.title}, ${art.date}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            loadingCats = true
                            showDialog = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val resp: GenesResponse = RetrofitClient.apiService.getGenes(art.id)
                                    categories = resp.embedded.genes.map { g ->
                                        Category(
                                            id = g.id,
                                            name = g.displayName ?: g.name.orEmpty(),
                                            image = g.links.thumbnail.href,
                                            description = g.description
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("Genes", "Error loading: ${e.message}")
                                    categories = emptyList()
                                } finally {
                                    loadingCats = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFFA6C4FC) else Color(0xFF34446C)
                        )
                    ) {
                        Text(
                            "View categories",
                            color = if (isDark) Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF282424) else Color.White,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(min = 600.dp, max = 600.dp),
            title = { 
                Text(
                    "Categories",
                    color = if (isDark) Color.White else Color.Black,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            },
            text = {
                when {
                    loadingCats -> {
                        LoadingIndicatorWithText(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
                    }

                    categories.isEmpty() -> {
                        Text("No Category", textAlign = TextAlign.Center)
                    }

                    else -> {
                        val cat = categories[page]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            // Image with previews: Using Row for clear division
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous category preview
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(150.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(categories[if (page > 0) page - 1 else categories.lastIndex].image)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Previous Category",
                                        contentScale = ContentScale.Crop,
                                        alignment = Alignment.CenterEnd,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .alpha(0.7f)
                                            .clip(RoundedCornerShape(topStart = 16.dp))
                                    )
                                }

                                // Current category image
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(150.dp)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(cat.image)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = cat.name,
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    )
                                }

                                // Next category preview
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(150.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(categories[if (page < categories.lastIndex) page + 1 else 0].image)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Next Category",
                                        contentScale = ContentScale.Crop,
                                        alignment = Alignment.CenterStart,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .alpha(0.7f)
                                            .clip(RoundedCornerShape(topEnd = 16.dp))
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                cat.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color.Black,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(8.dp))

                            // Description with navigation arrows (circular)
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Previous button (always visible)
                                IconButton(
                                    onClick = { 
                                        page = if (page > 0) page - 1 else categories.lastIndex
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = (-20).dp, y = (-10).dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ChevronLeft,
                                        contentDescription = "Previous",
                                        tint = if (isDark) Color.White else Color.Black,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(350.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Color(0xFF302C34) else Color(0xFFECECEC))
                                ) {
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        cat.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                            color = if (isDark) Color.White else Color.Black,
                                            textAlign = TextAlign.Justify
                                        )
                                    }
                                }

                                // Next button (always visible)
                                IconButton(
                                    onClick = { 
                                        page = if (page < categories.lastIndex) page + 1 else 0
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .offset(x = 20.dp, y = (-10).dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ChevronRight,
                                        contentDescription = "Next",
                                        tint = if (isDark) Color.White else Color.Black,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 36.dp, bottom = 36.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Button(
                        onClick = { showDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFFA6C4FC) else Color(0xFF34446C)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text(
                            "Close",
                            color = if (isDark) Color.Black else Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun SimilarTab(artists: List<Artist>, navController: NavController) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black
    val overlayColor = if (isDark) Color(0xFF283C6C).copy(alpha = 0.7f) else Color(0xFFE0EBFF).copy(alpha = 0.6f)
    val backgroundColor = if (isDark) Color(0xFF283C6C) else Color(0xFFDDE5FF)
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (artists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color.Black else Color.White),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.98f)
                        .padding(top = 32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = backgroundColor,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        "No Similar Artists",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDark) Color.White else Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color.Black else Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(artists) { artist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { navController.navigate("artistDetail/${artist.id}") },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(modifier = Modifier.height(180.dp)) {
                            // Artist image
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

                            // Star toggle
                            val isLoggedIn = UserSession.currentUser != null
                            val isFavorite by remember(artist.id) { derivedStateOf { artist.id in UserSession.favorites } }
                            val coroutineScope = rememberCoroutineScope()

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

                            // Bottom overlay with artist name
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
            }
        }

        // Snackbar host for favorite toggle messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Snackbar(
                snackbarData = it,
                containerColor = Color.Black,
                contentColor = Color.White,
                shape = RoundedCornerShape(4.dp)
            )
        }
    }
}
