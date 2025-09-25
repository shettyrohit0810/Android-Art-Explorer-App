package edu.usc.csci571.artsyapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import edu.usc.csci571.artsyapp.screens.HomeScreen
import edu.usc.csci571.artsyapp.screens.SearchScreen
import edu.usc.csci571.artsyapp.screens.ArtistDetailScreen
import edu.usc.csci571.artsyapp.screens.LoginScreen
import edu.usc.csci571.artsyapp.screens.RegisterScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "home") {
        composable(
            route = "home?showRegistrationSuccess={showRegistrationSuccess}",
            arguments = listOf(
                navArgument("showRegistrationSuccess") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val showRegistrationSuccess = backStackEntry.arguments?.getBoolean("showRegistrationSuccess") ?: false
            HomeScreen(navController = navController, showRegistrationSuccess = showRegistrationSuccess)
        }
        composable("search") {
            SearchScreen(navController = navController)
        }
        composable("artistDetail/{artistId}") { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId")
            artistId?.let {
                ArtistDetailScreen(artistId = it, navController = navController)
            }
        }
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("register") {
            RegisterScreen(navController = navController)
        }
    }
}