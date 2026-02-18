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
    data object Notifications : Screen("Notifications")
    object ShopList : Screen("shop_list")
    object AddShop : Screen("add_shop/{shopId}") {
        fun createRoute(shopId: Int? = null) = if (shopId != null) {
            "add_shop/$shopId"
        } else {
            "add_shop/0"
        }
    }
    data object Investors : Screen("Investors")
    object AddInvestor : Screen("add_investor/{investorId}") {
        fun createRoute(investorId: Int? = null) = if (investorId != null) {
            "add_investor/$investorId"
        } else {
            "add_investor/0"
        }
    }
    object InvestorDetail : Screen("investor_detail/{investorId}") {
        fun createRoute(investorId: Int) = "investor_detail/$investorId"
    }
    // investorId > 0 means coming from investor detail (investor is pre-selected, pick shop)
    // shopId > 0 means coming from shop detail (shop is pre-selected, pick investor)
    object AddShopInvestment : Screen("add_shop_investment/{investorId}/{shopId}") {
        fun createRoute(investorId: Int = 0, shopId: Int = 0) =
            "add_shop_investment/$investorId/$shopId"
    }
    object Employees : Screen("employees")
    object AddEmployee : Screen("add_employee/{employeeId}") {
        fun createRoute(employeeId: Int? = null) = if (employeeId != null) {
            "add_employee/$employeeId"
        } else {
            "add_employee/0"
        }
    }
}