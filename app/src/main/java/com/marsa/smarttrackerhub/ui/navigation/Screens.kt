package com.marsa.smarttrackerhub.ui.navigation


/**
 * Created by Muhammed Shafi on 08/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object AccountSetup : Screen("AccountSetup")
    object Home : Screen("home")
    object Summary : Screen("Summary")
    object Shops : Screen("Shops")
    object CategoryList : Screen("CategoryList")
    object AddCategory : Screen("AddCategory")
}