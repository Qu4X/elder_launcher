package xyz.arjunsinh.elderlauncher

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.arjunsinh.elderlauncher.data.model.FavoriteContact
import xyz.arjunsinh.elderlauncher.ui.drawer.AppDrawerScreen
import xyz.arjunsinh.elderlauncher.ui.drawer.ContactDrawerScreen
import xyz.arjunsinh.elderlauncher.ui.home.HomeScreen
import xyz.arjunsinh.elderlauncher.ui.home.HomeViewModel
import xyz.arjunsinh.elderlauncher.ui.theme.ElderLauncherTheme

enum class Screen {
    Home,
    AllApps,
    AllContacts
}

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private var pendingContactToCall: FavoriteContact? = null
    private var isDefaultLauncherState by mutableStateOf(false)

    // Hoisted navigation state properties to handle onNewIntent & prevent screen state loss
    private var currentScreen by mutableStateOf(Screen.Home)
    private var selectedTab by mutableIntStateOf(0)

    // BroadcastReceiver for real-time app installs/uninstalls
    private val packageReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            homeViewModel.refreshApps()
        }
    }

    // Activity Result Launcher for Role Manager Q+
    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkLauncherStatus()
    }

    // Permission Launcher for Contacts
    private val requestContactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            homeViewModel.refreshContacts()
        }
    }

    // Permission Launcher for Call Phone
    private val requestCallPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val contact = pendingContactToCall
        if (contact != null) {
            if (isGranted) {
                makeDirectCall(contact.phoneNumber)
            } else {
                makeFallbackCall(contact.phoneNumber)
            }
            pendingContactToCall = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Splash Screen API
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Draw under status and nav bars (Edge-to-Edge)
        enableEdgeToEdge()

        checkLauncherStatus()

        // Register BroadcastReceiver for real-time app installs/uninstalls
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)

        setContent {
            val currentTime by homeViewModel.currentTime.collectAsStateWithLifecycle("")
            val currentDate by homeViewModel.currentDate.collectAsStateWithLifecycle("")
            val favoriteApps by homeViewModel.favoriteApps.collectAsStateWithLifecycle(emptyList())
            val favoriteContacts by homeViewModel.favoriteContacts.collectAsStateWithLifecycle(emptyList())
            val allApps by homeViewModel.allApps.collectAsStateWithLifecycle(emptyList())
            val allContacts by homeViewModel.allContacts.collectAsStateWithLifecycle(emptyList())
            val favoriteAppPackageNames by remember(favoriteApps) {
                derivedStateOf { favoriteApps.map { it.packageName }.toSet() }
            }
            val favoriteContactNumbers by remember(favoriteContacts) {
                derivedStateOf { favoriteContacts.map { it.phoneNumber }.toSet() }
            }

            // Check contact permission in UI scope
            val hasContactsPermission = remember {
                mutableStateOf(hasPermission(Manifest.permission.READ_CONTACTS))
            }

            ElderLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Prevent launcher from closing/recreating when back gesture is used on the main home screen
                    BackHandler(enabled = currentScreen == Screen.Home) {
                        // Do nothing: home screen is the root of the launcher
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // 1. Home Screen is always in the background (static)
                        HomeScreen(
                            currentTime = currentTime,
                            currentDate = currentDate,
                            favoriteApps = favoriteApps,
                            favoriteContacts = if (hasContactsPermission.value) favoriteContacts else emptyList(),
                            isDefaultLauncher = isDefaultLauncherState,
                            onSetDefaultLauncher = { setDefaultLauncher() },
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            onOpenAllApps = { currentScreen = Screen.AllApps },
                            onOpenAllContacts = {
                                if (hasPermission(Manifest.permission.READ_CONTACTS)) {
                                    hasContactsPermission.value = true
                                    homeViewModel.refreshContacts()
                                    selectedTab = 1
                                    currentScreen = Screen.AllContacts
                                } else {
                                    requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            },
                            onCallContact = { contact -> handleCallAction(contact) },
                            onRemoveApp = { app -> homeViewModel.removeFavoriteApp(app.packageName) },
                            onRemoveContact = { contact -> homeViewModel.removeFavoriteContact(contact.phoneNumber) }
                        )

                        // 2. Overlay App Drawer
                        AnimatedVisibility(
                            visible = currentScreen == Screen.AllApps,
                            enter = slideInVertically(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300)),
                            exit = slideOutVertically(animationSpec = tween(300)) { it } + fadeOut(animationSpec = tween(300))
                        ) {
                            AppDrawerScreen(
                                allApps = allApps,
                                favoritePackageNames = favoriteAppPackageNames,
                                onToggleFavorite = { app ->
                                    if (favoriteAppPackageNames.contains(app.packageName)) {
                                        homeViewModel.removeFavoriteApp(app.packageName)
                                    } else {
                                        homeViewModel.addFavoriteApp(app.packageName)
                                    }
                                },
                                onBack = { currentScreen = Screen.Home }
                            )
                        }

                        // 3. Overlay Contact Drawer
                        AnimatedVisibility(
                            visible = currentScreen == Screen.AllContacts,
                            enter = slideInVertically(animationSpec = tween(300)) { it } + fadeIn(animationSpec = tween(300)),
                            exit = slideOutVertically(animationSpec = tween(300)) { it } + fadeOut(animationSpec = tween(300))
                        ) {
                            if (hasPermission(Manifest.permission.READ_CONTACTS)) {
                                ContactDrawerScreen(
                                    allContacts = allContacts,
                                    favoritePhoneNumbers = favoriteContactNumbers,
                                    onToggleFavorite = { contact ->
                                        if (favoriteContactNumbers.contains(contact.phoneNumber)) {
                                            homeViewModel.removeFavoriteContact(contact.phoneNumber)
                                        } else {
                                            homeViewModel.addFavoriteContact(contact.phoneNumber)
                                        }
                                    },
                                    onBack = { currentScreen = Screen.Home }
                                )
                            } else {
                                // Request permission interface
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = stringResource(R.string.msg_no_contacts_permission),
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = {
                                                requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                            },
                                            modifier = Modifier.height(60.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.btn_grant_permission).uppercase(),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkLauncherStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // If the user clicks the Home button/gesture, reset the navigation back to the main home screen smoothly
        if (intent.hasCategory(Intent.CATEGORY_HOME) || intent.action == Intent.ACTION_MAIN) {
            currentScreen = Screen.Home
        }
    }

    private fun checkLauncherStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            val isDefault = isDefaultLauncher()
            withContext(Dispatchers.Main) {
                isDefaultLauncherState = isDefault
            }
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun setDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                roleRequestLauncher.launch(intent)
            }
        } else {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    private fun handleCallAction(contact: FavoriteContact) {
        if (hasPermission(Manifest.permission.CALL_PHONE)) {
            makeDirectCall(contact.phoneNumber)
        } else {
            pendingContactToCall = contact
            requestCallPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun makeDirectCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:${phoneNumber.replace(" ", "")}")
        }
        try {
            startActivity(intent)
        } catch (e: SecurityException) {
            // Fallback to dialer if security check fails
            makeFallbackCall(phoneNumber)
        } catch (e: ActivityNotFoundException) {
            makeFallbackCall(phoneNumber)
        }
    }

    private fun makeFallbackCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${phoneNumber.replace(" ", "")}")
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
