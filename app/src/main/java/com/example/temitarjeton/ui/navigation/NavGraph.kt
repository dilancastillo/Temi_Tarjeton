package com.example.temitarjeton.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

object Routes {
    const val Attract = "attract"
    const val Ballot = "ballot"
    const val Video = "video"
}

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    attract: @Composable () -> Unit,
    ballot: @Composable () -> Unit,
    video: @Composable () -> Unit,
) {
    NavHost(navController = navController, startDestination = Routes.Attract, modifier = modifier) {
        composable(Routes.Attract) { attract() }
        composable(Routes.Ballot) { ballot() }
        composable(Routes.Video) { video() }
    }
}