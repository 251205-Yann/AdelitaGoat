package mx.edu.upchiapas.adelitagoat.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import mx.edu.upchiapas.adelitagoat.domain.UserRole
import mx.edu.upchiapas.adelitagoat.ui.screens.*
import mx.edu.upchiapas.adelitagoat.ui.theme.AdelitaCoral
import mx.edu.upchiapas.adelitagoat.ui.theme.AdelitaNavy
import mx.edu.upchiapas.adelitagoat.ui.theme.AdelitaTextMuted
import mx.edu.upchiapas.adelitagoat.ui.viewmodel.AuthViewModel
import mx.edu.upchiapas.adelitagoat.ui.viewmodel.CustomerViewModel
import mx.edu.upchiapas.adelitagoat.ui.viewmodel.VendorViewModel

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AdelitaNavHost(
    authViewModel: AuthViewModel,
    customerViewModel: CustomerViewModel,
    vendorViewModel: VendorViewModel
) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()
    val feedbackMessage by customerViewModel.feedbackMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            customerViewModel.clearFeedback()
        }
    }

    val startDestination = remember(currentUser) {
        when (currentUser?.role) {
            UserRole.CLIENTE -> "customer_home"
            UserRole.VENDEDOR -> "vendor_orders"
            null -> if (authViewModel.isOnboardingSeen()) "auth?register=false" else "welcome"
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    val customerTabs = remember {
        listOf(
            BottomNavItem("customer_home", "Inicio", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem("customer_orders", "Pedidos", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
            BottomNavItem("customer_favorites", "Favoritos", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
            BottomNavItem("profile", "Perfil", Icons.Filled.Person, Icons.Outlined.Person)
        )
    }

    val vendorTabs = remember {
        listOf(
            BottomNavItem("vendor_orders", "Pedidos", Icons.Filled.Inbox, Icons.Outlined.Inbox),
            BottomNavItem("vendor_menu", "Menú", Icons.Filled.RestaurantMenu, Icons.Outlined.RestaurantMenu),
            BottomNavItem("profile", "Perfil", Icons.Filled.Person, Icons.Outlined.Person)
        )
    }

    val showBottomBar = currentUser != null && !currentRoute.startsWith("welcome") && !currentRoute.startsWith("auth")

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                val tabs = if (currentUser?.role == UserRole.VENDEDOR) vendorTabs else customerTabs
                NavigationBar(
                    containerColor = Color.White
                ) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AdelitaCoral,
                                selectedTextColor = AdelitaCoral,
                                unselectedIconColor = AdelitaTextMuted,
                                unselectedTextColor = AdelitaTextMuted,
                                indicatorColor = AdelitaCoral.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("welcome") {
                WelcomeScreen(
                    onStartClick = {
                        authViewModel.markOnboardingSeen()
                        navController.navigate("auth?register=true")
                    },
                    onLoginClick = {
                        authViewModel.markOnboardingSeen()
                        navController.navigate("auth?register=false")
                    }
                )
            }

            composable(
                route = "auth?register={register}",
                arguments = listOf(navArgument("register") { type = NavType.BoolType; defaultValue = false })
            ) { backStackEntry ->
                val isRegister = backStackEntry.arguments?.getBoolean("register") ?: false
                AuthScreen(
                    authViewModel = authViewModel,
                    initialRegisterMode = isRegister,
                    onAuthenticated = { user ->
                        val target = if (user.role == UserRole.VENDEDOR) "vendor_orders" else "customer_home"
                        navController.navigate(target) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // =================================================================
            // RECORRIDOS DE CLIENTE
            // =================================================================

            composable("customer_home") {
                CustomerHomeScreen(
                    currentUser = currentUser,
                    customerViewModel = customerViewModel,
                    onOpenStall = { stallId ->
                        navController.navigate("stall_detail/$stallId")
                    },
                    onOpenCart = {
                        navController.navigate("cart")
                    }
                )
            }

            composable(
                route = "stall_detail/{stallId}",
                arguments = listOf(navArgument("stallId") { type = NavType.LongType })
            ) { backStackEntry ->
                val stallId = backStackEntry.arguments?.getLong("stallId") ?: 1L
                StallDetailScreen(
                    stallId = stallId,
                    customerViewModel = customerViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenProduct = { productId ->
                        navController.navigate("product_detail/$productId")
                    },
                    onOpenCart = {
                        navController.navigate("cart")
                    }
                )
            }

            composable(
                route = "product_detail/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: 1L
                ProductDetailScreen(
                    productId = productId,
                    customerViewModel = customerViewModel,
                    onBack = { navController.popBackStack() },
                    onAddedNavigateToCart = {
                        navController.navigate("cart")
                    }
                )
            }

            composable("cart") {
                CartAndCheckoutScreen(
                    customerViewModel = customerViewModel,
                    onBack = { navController.popBackStack() },
                    onExploreStalls = {
                        navController.navigate("customer_home") {
                            popUpTo("customer_home") { inclusive = true }
                        }
                    },
                    onOrderConfirmed = { orderId ->
                        navController.navigate("order_ticket/$orderId") {
                            popUpTo("customer_home") { inclusive = false }
                        }
                    }
                )
            }

            composable(
                route = "order_ticket/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.LongType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getLong("orderId") ?: 1L
                OrderTicketScreen(
                    orderId = orderId,
                    customerViewModel = customerViewModel,
                    onViewOrderStatus = { id ->
                        navController.navigate("order_detail/$id")
                    },
                    onBackHome = {
                        navController.navigate("customer_home") {
                            popUpTo("customer_home") { inclusive = true }
                        }
                    }
                )
            }

            composable("customer_orders") {
                OrderHistoryScreen(
                    customerViewModel = customerViewModel,
                    onOpenOrderDetail = { orderId ->
                        navController.navigate("order_detail/$orderId")
                    },
                    onRateOrder = { orderId ->
                        navController.navigate("rate_order/$orderId")
                    }
                )
            }

            composable(
                route = "order_detail/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.LongType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getLong("orderId") ?: 1L
                OrderDetailScreen(
                    orderId = orderId,
                    customerViewModel = customerViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenTicket = { id ->
                        navController.navigate("order_ticket/$id")
                    },
                    onRateOrder = { id ->
                        navController.navigate("rate_order/$id")
                    }
                )
            }

            composable(
                route = "rate_order/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.LongType })
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getLong("orderId") ?: 1L
                RateOrderScreen(
                    orderId = orderId,
                    customerViewModel = customerViewModel,
                    onBack = { navController.popBackStack() },
                    onRatingSubmitted = {
                        navController.popBackStack()
                    }
                )
            }

            composable("customer_favorites") {
                FavoritesScreen(
                    customerViewModel = customerViewModel,
                    onOpenStall = { stallId ->
                        navController.navigate("stall_detail/$stallId")
                    }
                )
            }

            // =================================================================
            // RECORRIDOS DE VENDEDOR
            // =================================================================

            composable("vendor_orders") {
                VendorOrdersScreen(vendorViewModel = vendorViewModel)
            }

            composable("vendor_menu") {
                VendorMenuScreen(vendorViewModel = vendorViewModel)
            }

            // =================================================================
            // PERFIL Y CIERRE DE SESIÓN
            // =================================================================

            composable("profile") {
                ProfileScreen(
                    user = currentUser,
                    authViewModel = authViewModel,
                    onLoggedOut = {
                        navController.navigate("auth?register=false") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
