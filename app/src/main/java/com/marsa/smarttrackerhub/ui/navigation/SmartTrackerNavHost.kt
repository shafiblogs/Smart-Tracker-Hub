package com.marsa.smarttrackerhub.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.marsa.smarttracker.ui.theme.sTypography
import com.marsa.smarttrackerhub.domain.AccessCode
import com.marsa.smarttrackerhub.ui.components.CommonTextField
import com.marsa.smarttrackerhub.ui.components.SmallTextField
import com.marsa.smarttrackerhub.ui.screens.SplashScreen
import com.marsa.smarttrackerhub.ui.screens.account.AccountSetupScreen
import com.marsa.smarttrackerhub.ui.screens.employees.AddEmployeeScreen
import com.marsa.smarttrackerhub.ui.screens.employees.EmployeesScreen
import com.marsa.smarttrackerhub.ui.screens.home.HomeScreen
import com.marsa.smarttrackerhub.ui.screens.investers.AddInvestorScreen
import com.marsa.smarttrackerhub.ui.screens.investers.InvestorsScreen
import com.marsa.smarttrackerhub.ui.screens.login.LoginScreen
import com.marsa.smarttrackerhub.ui.screens.notifications.NotificationsScreen
import com.marsa.smarttrackerhub.ui.screens.sale.SaleScreen
import com.marsa.smarttrackerhub.ui.screens.shops.AddShopScreen
import com.marsa.smarttrackerhub.ui.screens.shops.ShopsListScreen
import com.marsa.smarttrackerhub.ui.screens.statement.StatementScreen
import com.marsa.smarttrackerhub.ui.screens.summary.SummaryScreen
import kotlinx.coroutines.launch


/**
 * Created by Muhammed Shafi on 08/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTrackerNavHost(navController: NavHostController) {
    val viewModel: MainAppViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(Unit) {
        viewModel.loadUserAccount()
    }

    val bottomNavRoutes = mutableListOf(
        Screen.Home.route, Screen.Sale.route, Screen.Summary.route, Screen.Notifications.route
    )

    val showBottomBar = currentRoute in bottomNavRoutes
    val isAccountActive by viewModel.isAccountActive.collectAsState()
    val userAccessCode by viewModel.userAccessCode.collectAsState()

    fun navigateToRoute(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(Screen.Home.route) { inclusive = false; saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val content: @Composable (PaddingValues) -> Unit = { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(onTimeout = {
                    if (isAccountActive) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.AccountSetup.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                })
            }
            composable(Screen.Login.route) {
                LoginScreen(onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.AccountSetup.route) {
                AccountSetupScreen(onAccountCreated = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.AccountSetup.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Sale.route) {
                SaleScreen(userAccessCode = userAccessCode)
            }
            composable(Screen.Home.route) {
                HomeScreen(userAccessCode = userAccessCode)
            }
            composable(Screen.Statement.route) { StatementScreen(userAccessCode = userAccessCode) }
            composable(Screen.Summary.route) { SummaryScreen(userAccessCode = userAccessCode) }
            composable(
                route = Screen.AddShop.route,
                arguments = listOf(
                    navArgument("shopId") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { backStackEntry ->
                val shopId = backStackEntry.arguments?.getInt("shopId")
                AddShopScreen(
                    shopId = if (shopId == 0) null else shopId,
                    onShopCreated = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.AddInvestor.route) { AddInvestorScreen(onSaveSuccess = { navController.popBackStack() }) }
            composable(
                route = Screen.AddEmployee.route,
                arguments = listOf(
                    navArgument("employeeId") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { backStackEntry ->
                val employeeId = backStackEntry.arguments?.getInt("employeeId")
                AddEmployeeScreen(
                    employeeId = if (employeeId == 0) null else employeeId,
                    onEmployeeCreated = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.ShopList.route) {
                ShopsListScreen(
                    onEditClick = { shopId ->
                        navController.navigate(Screen.AddShop.createRoute(shopId))
                    },
                    onAddClick = {
                        navController.navigate(Screen.AddShop.createRoute())
                    }
                )
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    userAccessCode = userAccessCode,
                    onShopClick = { shopId ->
                        navController.navigate(Screen.AddShop.createRoute(shopId))
                    },
                    onEmployeeClick = { employeeId ->
                        navController.navigate(Screen.AddEmployee.createRoute(employeeId))
                    }
                )
            }
            composable(Screen.Investors.route) {
                InvestorsScreen(
                    onItemClick = {},
                    onAddClick = {
                        navController.navigate(Screen.AddInvestor.route)
                    })
            }
            composable(Screen.Employees.route) {
                EmployeesScreen(
                    onEditClick = { employeeId ->
                        navController.navigate(Screen.AddEmployee.createRoute(employeeId))
                    },
                    onAddClick = {
                        navController.navigate(Screen.AddEmployee.createRoute())
                    }
                )
            }
        }
    }

    val scaffoldContent = @Composable {
        Scaffold(
            topBar = {
                when (currentRoute) {
                    Screen.Home.route -> {
                        TopAppBar(
                            title = {
                                CommonTextField(
                                    value = "Monthly Track",
                                    style = sTypography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    )
                                )
                            },

                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }

                    Screen.Login.route -> {
                        TopAppBar(
                            title = {
                                CommonTextField(
                                    value = "Login Track",
                                    style = sTypography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    )
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }

                    Screen.Statement.route, Screen.ShopList.route, Screen.AddShop.route, Screen.Sale.route, Screen.Notifications.route,
                    Screen.Investors.route, Screen.AddInvestor.route, Screen.Employees.route, Screen.AddEmployee.route,
                    Screen.AccountSetup.route, Screen.Summary.route -> {
                        val titleText = when (currentRoute) {
                            Screen.AccountSetup.route -> "My Account"
                            Screen.Notifications.route -> "Notifications"
                            Screen.AddShop.route -> "Shop"
                            Screen.AddEmployee.route -> "Employee"
                            Screen.Investors.route -> "Investors"
                            Screen.Sale.route -> "Sales"
                            Screen.Statement.route -> "Statements"
                            Screen.ShopList.route -> "Shops"
                            Screen.Summary.route -> "Summary"
                            Screen.Employees.route -> "Employees"
                            else -> "$currentRoute Records"
                        }
                        TopAppBar(
                            title = {
                                CommonTextField(
                                    value = titleText,
                                    style = sTypography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    )
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }

                    else -> {}
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == Screen.Home.route,
                            onClick = { navigateToRoute(Screen.Home.route) },
                            icon = {
                                Icon(Icons.Default.Home, contentDescription = "Home")
                            },
                            label = {
                                Text(
                                    text = "Home",
                                    style = sTypography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                            })

                        NavigationBarItem(
                            selected = currentRoute == Screen.Sale.route,
                            onClick = { navigateToRoute(Screen.Sale.route) },
                            icon = {
                                Icon(Icons.Default.DateRange, contentDescription = "Sales")
                            },
                            label = {
                                SmallTextField("Sales")
                            })

                        NavigationBarItem(
                            selected = currentRoute == Screen.Summary.route,
                            onClick = { navigateToRoute(Screen.Summary.route) },
                            icon = {
                                Icon(Icons.Default.Menu, contentDescription = "Summary")
                            },
                            label = {
                                SmallTextField("Summary")
                            })

                        NavigationBarItem(
                            selected = currentRoute == Screen.Notifications.route,
                            onClick = { navigateToRoute(Screen.Notifications.route) },
                            icon = {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notifications"
                                )
                            },
                            label = {
                                SmallTextField("Notifications")
                            })
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            content = content
        )
    }

    if (currentRoute == Screen.Home.route) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                Surface(
                    modifier = Modifier
                        .width(screenWidth * 0.7f)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Spacer(modifier = Modifier.size(24.dp))
                        Text(
                            "Menu Track",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )

                        val drawerItems = if (userAccessCode == AccessCode.ADMIN) listOf(
                            Screen.Statement.route to "Statement",
                            Screen.ShopList.route to "Shops",
                            Screen.Employees.route to "Employees",
                            Screen.Investors.route to "Investors",
                            Screen.AccountSetup.route to "My Account"
                        ) else listOf(

                            Screen.Statement.route to "Statement",
                            Screen.AccountSetup.route to "My Account"
                        )

                        drawerItems.forEach { (route, label) ->
                            NavigationDrawerItem(
                                label = { Text(label) },
                                selected = currentRoute == route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navigateToRoute(route)
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
            }
        ) {
            scaffoldContent()
        }
    } else {
        scaffoldContent()
    }
}
