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

    // investorId > 0 means coming from investor detail (investor pre-selected, pick shop)
    // shopId > 0 means coming from shop detail (shop pre-selected, pick investor)
    object AddShopInvestment : Screen("add_shop_investment/{investorId}/{shopId}") {
        fun createRoute(investorId: Int = 0, shopId: Int = 0) =
            "add_shop_investment/$investorId/$shopId"
    }

    /**
     * ShopInvestmentDashboard — overview of capital, investors and transactions for one shop.
     * Can be reached from: AddShopScreen investor section, InvestorDetailScreen shop row.
     */
    object ShopInvestmentDashboard : Screen("shop_investment_dashboard/{shopId}") {
        fun createRoute(shopId: Int) = "shop_investment_dashboard/$shopId"
    }

    /**
     * AddTransaction — record a phase-based payment for an investor in a shop.
     * shopInvestorId can be 0 to let the user pick; > 0 to pre-select.
     */
    object AddTransaction : Screen("add_transaction/{shopId}/{shopInvestorId}") {
        fun createRoute(shopId: Int, shopInvestorId: Int = 0) =
            "add_transaction/$shopId/$shopInvestorId"
    }

    /**
     * SettlementCalculator — compute and confirm year-end settlement for a shop.
     */
    object SettlementCalculator : Screen("settlement_calculator/{shopId}") {
        fun createRoute(shopId: Int) = "settlement_calculator/$shopId"
    }

    /**
     * SettlementHistory — list of all past year-end settlements for a shop.
     */
    object SettlementHistory : Screen("settlement_history/{shopId}") {
        fun createRoute(shopId: Int) = "settlement_history/$shopId"
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
