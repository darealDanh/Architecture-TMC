package com.tmc.mobile.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tmc.mobile.ui.attendance.AttendanceRoute
import com.tmc.mobile.ui.components.TmcIcons
import com.tmc.mobile.ui.device.DeviceRoute
import com.tmc.mobile.ui.pass.PassRoute
import com.tmc.mobile.ui.signin.SignInRoute
import com.tmc.mobile.ui.theme.TmcColors

private object Routes {
    const val SIGN_IN = "sign-in"
    const val MAIN = "main"
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Pass("pass", "Pass", TmcIcons.Pass),
    Attendance("attendance", "Attendance", TmcIcons.Attendance),
    Device("device", "Device", TmcIcons.Device),
}

@Composable
fun TmcNavHost(startViewModel: StartViewModel = hiltViewModel()) {
    val start by startViewModel.start.collectAsStateWithLifecycle()
    val destination = start ?: run {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = if (destination == StartDestination.Main) Routes.MAIN else Routes.SIGN_IN,
    ) {
        composable(Routes.SIGN_IN) {
            SignInRoute(onSignedIn = {
                navController.navigate(Routes.MAIN) { popUpTo(Routes.SIGN_IN) { inclusive = true } }
            })
        }
        composable(Routes.MAIN) {
            MainScaffold(onSignedOut = {
                navController.navigate(Routes.SIGN_IN) { popUpTo(Routes.MAIN) { inclusive = true } }
            })
        }
    }
}

@Composable
private fun MainScaffold(onSignedOut: () -> Unit) {
    val tabs = rememberNavController()
    val backStack by tabs.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = TmcColors.NavigationBar) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            tabs.navigate(tab.route) {
                                popUpTo(tabs.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(tabs, startDestination = Tab.Pass.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Pass.route) { PassRoute() }
            composable(Tab.Attendance.route) { AttendanceRoute() }
            composable(Tab.Device.route) { DeviceRoute(onSignedOut = onSignedOut) }
        }
    }
}
