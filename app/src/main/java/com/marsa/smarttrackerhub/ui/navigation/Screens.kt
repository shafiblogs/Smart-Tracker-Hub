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
    object Statement : Screen("Statement")
    object ShopList : Screen("ShopList")
    object Investors : Screen("Investors")
    object Employees : Screen("Employees")
    object CategoryList : Screen("CategoryList")
    object AddCategory : Screen("AddCategory")
    object AddShop : Screen("AddShop")
    object AddInvestor : Screen("AddInvestor")
    object AddEmployee : Screen("AddEmployee")
}