package com.zenbase.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zenbase.app.ui.collectiondetail.CollectionDetailScreen
import com.zenbase.app.ui.collections.CollectionsScreen
import com.zenbase.app.ui.settings.SettingsScreen

/**
 * Standardizovaný navigační graf implementující routování aplikací Zenbase v Compose strukturálních hranicích.
 */
@Composable
fun ZenbaseNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "collections"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("collections") {
            CollectionsScreen(
                onNavigateToCollection = { collectionId -> 
                    navController.navigate("collection/$collectionId")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        
        composable(
            route = "collection/{collectionId}",
            arguments = listOf(navArgument("collectionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val collectionId = backStackEntry.arguments?.getLong("collectionId") ?: 1L
            CollectionDetailScreen(
                collectionId = collectionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("settings") {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
