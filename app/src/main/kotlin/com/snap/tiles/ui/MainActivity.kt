package com.snap.tiles.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.snap.tiles.data.Action
import com.snap.tiles.data.TileConfigRepo
import com.snap.tiles.ui.screens.AddActionScreen
import com.snap.tiles.ui.screens.EditTileScreen
import com.snap.tiles.ui.screens.HomeScreen
import com.snap.tiles.ui.theme.QuickTilesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TileConfigRepo.initAllSlots(this)

        setContent {
            QuickTilesTheme {
                QuickTilesNavHost()
            }
        }
    }
}

@Composable
private fun QuickTilesNavHost() {
    val navController = rememberNavController()

    // Shared state: EditTileScreen writes current selections here before navigating to AddAction
    var pendingActions by remember { mutableStateOf<Set<Action>>(emptySet()) }
    // When AddActionScreen saves, it writes the result here so EditTileScreen picks it up
    var returnedActions by remember { mutableStateOf<Set<Action>?>(null) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onEditSlot = { slot ->
                    // Reset returned actions when entering edit
                    returnedActions = null
                    navController.navigate("edit/$slot")
                }
            )
        }

        composable(
            "edit/{slot}",
            arguments = listOf(navArgument("slot") { type = NavType.IntType })
        ) { backStackEntry ->
            val slot = backStackEntry.arguments?.getInt("slot") ?: 1
            EditTileScreen(
                slotIndex = slot,
                returnedActions = returnedActions,
                onReturnedActionsConsumed = { returnedActions = null },
                onBack = { navController.popBackStack() },
                onAddAction = { currentSelected ->
                    pendingActions = currentSelected
                    navController.navigate("addAction/$slot")
                },
                onSaved = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            "addAction/{slot}",
            arguments = listOf(navArgument("slot") { type = NavType.IntType })
        ) {
            AddActionScreen(
                alreadySelected = pendingActions,
                onBack = { navController.popBackStack() },
                onActionsSelected = { selected ->
                    returnedActions = selected
                    navController.popBackStack()
                }
            )
        }
    }
}
