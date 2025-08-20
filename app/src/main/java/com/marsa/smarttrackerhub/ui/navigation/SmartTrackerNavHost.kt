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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.marsa.smarttracker.ui.theme.sTypography
import com.marsa.smarttrackerhub.ui.components.CommonTextField
import com.marsa.smarttrackerhub.ui.components.SmallTextField
import com.marsa.smarttrackerhub.ui.screens.SplashScreen
import com.marsa.smarttrackerhub.ui.screens.account.AccountSetupScreen
import com.marsa.smarttrackerhub.ui.screens.category.CategoryListScreen
import com.marsa.smarttrackerhub.ui.screens.employees.AddEmployeeScreen
import com.marsa.smarttrackerhub.ui.screens.employees.EmployeesScreen
import com.marsa.smarttrackerhub.ui.screens.home.HomeScreen
import com.marsa.smarttrackerhub.ui.screens.investers.AddInvestorScreen
import com.marsa.smarttrackerhub.ui.screens.investers.InvestorsScreen
import com.marsa.smarttrackerhub.ui.screens.login.LoginScreen
import com.marsa.smarttrackerhub.ui.screens.shops.AddShopScreen
import com.marsa.smarttrackerhub.ui.screens.shops.ShopsListScreen
import com.marsa.smarttrackerhub.ui.screens.statement.ShopsScreen
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val bottomNavRoutes = mutableListOf(
        Screen.Home.route, Screen.Shops.route, Screen.Summary.route
    )

    val showBottomBar = currentRoute in bottomNavRoutes

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
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
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
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Shops.route) { ShopsScreen() }
            composable(Screen.Summary.route) { SummaryScreen() }
            composable(Screen.AddShop.route) { AddShopScreen(onShopCreated = { navController.popBackStack() }) }
            composable(Screen.AddInvestor.route) { AddInvestorScreen(onSaveSuccess = { navController.popBackStack() }) }
            composable(Screen.AddEmployee.route) { AddEmployeeScreen(onEmployeeCreated = {navController.popBackStack()}) }
            composable(Screen.CategoryList.route) {
                CategoryListScreen(
                    onItemClick = {},
                    onAddClick = {})
            }
            composable(Screen.ShopList.route) {
                ShopsListScreen(
                    onItemClick = {},
                    onAddClick = {
                        navController.navigate(Screen.AddShop.route)
                    })
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
                    onItemClick = {},
                    onAddClick = {
                        navController.navigate(Screen.AddEmployee.route)
                    })
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
                                    value = "Today's Track",
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

                    Screen.Shops.route, Screen.ShopList.route, Screen.AddShop.route,
                    Screen.Investors.route, Screen.AddInvestor.route, Screen.Employees.route, Screen.AddEmployee.route,
                    Screen.AccountSetup.route, Screen.CategoryList.route, Screen.Summary.route,
                    Screen.AddCategory.route -> {
                        val titleText = when (currentRoute) {
                            Screen.AccountSetup.route -> "My Account"
                            Screen.Investors.route -> "Investors"
                            Screen.ShopList.route -> "Shops"
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
                            selected = currentRoute == Screen.Shops.route,
                            onClick = { navigateToRoute(Screen.Shops.route) },
                            icon = {
                                Icon(Icons.Default.DateRange, contentDescription = "Shops")
                            },
                            label = {
                                SmallTextField("Shops")
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
                    }
                }
            },
            floatingActionButton = {
                when (currentRoute) {
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

                        val drawerItems = listOf(
                            Screen.AccountSetup.route to "My Account",
                            Screen.Investors.route to "Investors",
                            Screen.ShopList.route to "Shops",
                            Screen.Employees.route to "Employees",
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
