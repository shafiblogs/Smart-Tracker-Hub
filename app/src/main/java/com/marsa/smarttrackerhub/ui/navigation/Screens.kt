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
    object ShopList : Screen("shop_list")
    object AddShop : Screen("add_shop/{shopId}") {
        fun createRoute(shopId: Int? = null) = if (shopId != null) {
            "add_shop/$shopId"
        } else {
            "add_shop/0"
        }
    }
    data object Investors : Screen("Investors")
    data object Employees : Screen("Employees")
    data object AddInvestor : Screen("AddInvestor")
    data object AddEmployee : Screen("AddEmployee")
}