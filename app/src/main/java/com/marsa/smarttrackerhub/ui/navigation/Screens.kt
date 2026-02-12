package com.marsa.smarttrackerhub.ui.navigation


/**
 * Created by Muhammed Shafi on 08/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object AccountSetup : Screen("AccountSetup")
    data object Sale : Screen("sale")
    data object Home : Screen("home")
    data object Summary : Screen("Summary")
    data object Statement : Screen("Statement")
    data object ShopList : Screen("ShopList")
    data object Investors : Screen("Investors")
    data object Employees : Screen("Employees")
    data object AddShop : Screen("AddShop")
    data object AddInvestor : Screen("AddInvestor")
    data object AddEmployee : Screen("AddEmployee")
}