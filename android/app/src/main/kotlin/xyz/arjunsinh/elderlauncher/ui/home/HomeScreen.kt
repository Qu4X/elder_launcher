package xyz.arjunsinh.elderlauncher.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import xyz.arjunsinh.elderlauncher.ui.common.AutoSizeText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import xyz.arjunsinh.elderlauncher.R
import xyz.arjunsinh.elderlauncher.data.model.FavoriteContact
import xyz.arjunsinh.elderlauncher.data.model.LauncherApp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    currentTime: String,
    currentDate: String,
    favoriteApps: List<LauncherApp>,
    favoriteContacts: List<FavoriteContact>,
    isDefaultLauncher: Boolean,
    onSetDefaultLauncher: () -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onOpenAllApps: () -> Unit,
    onOpenAllContacts: () -> Unit,
    onCallContact: (FavoriteContact) -> Unit,
    onRemoveApp: (LauncherApp) -> Unit,
    onRemoveContact: (FavoriteContact) -> Unit
) {
    val configuration = LocalConfiguration.current
    val columnCount = remember(configuration.screenWidthDp) {
        if (configuration.screenWidthDp < 600) 2 else 4
    }
    val tabs = listOf(stringResource(R.string.apps), stringResource(R.string.contacts))
    var appToRemove by remember { mutableStateOf<LauncherApp?>(null) }
    var contactToRemove by remember { mutableStateOf<FavoriteContact?>(null) }
    var contactToCall by remember { mutableStateOf<FavoriteContact?>(null) }

    // Swipeable pager state
    val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { 2 })

    // Sync from hoisted selectedTab to pagerState
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    // Sync from pagerState back to hoisted selectedTab
    LaunchedEffect(pagerState.currentPage) {
        if (selectedTab != pagerState.currentPage) {
            onTabSelected(pagerState.currentPage)
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Elegant, Large M3 Clock and Date
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 64.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Default Launcher Banner
            if (!isDefaultLauncher) {
                ElevatedCard(
                    onClick = onSetDefaultLauncher,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.msg_not_default_launcher),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.btn_set_default_launcher).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Material 3 Primary Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                containerColor = MaterialTheme.colorScheme.background
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { onTabSelected(index) },
                        text = {
                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                if (page == 0) {
                    // Apps Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (favoriteApps.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.msg_no_favourites),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columnCount),
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(
                                    items = favoriteApps,
                                    key = { it.packageName }
                                ) { app ->
                                    val context = LocalContext.current
                                    var currentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .animateItemPlacement()
                                            .onGloballyPositioned { currentCoords = it }
                                            .combinedClickable(
                                                onClick = {
                                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                                    if (launchIntent != null) {
                                                        currentCoords?.let { coords ->
                                                            if (coords.isAttached) {
                                                                val position = coords.localToWindow(Offset.Zero)
                                                                val size = coords.size
                                                                launchIntent.sourceBounds = android.graphics.Rect(
                                                                    position.x.toInt(),
                                                                    position.y.toInt(),
                                                                    (position.x + size.width).toInt(),
                                                                    (position.y + size.height).toInt()
                                                                )
                                                            }
                                                        }
                                                        context.startActivity(launchIntent)
                                                    }
                                                },
                                                onLongClick = { appToRemove = app }
                                            ),
                                        colors = CardDefaults.elevatedCardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            AsyncImage(
                                                model = app.icon,
                                                contentDescription = app.label,
                                                modifier = Modifier.size(56.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = app.label,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FilledTonalButton(
                            onClick = onOpenAllApps,
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(text = stringResource(R.string.btn_all_apps).uppercase(), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    // Contacts Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (favoriteContacts.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.msg_no_favourites),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columnCount),
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(
                                    items = favoriteContacts,
                                    key = { it.phoneNumber }
                                ) { contact ->
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .animateItemPlacement()
                                            .combinedClickable(
                                                onClick = { contactToCall = contact },
                                                onLongClick = { contactToRemove = contact }
                                            ),
                                        colors = CardDefaults.elevatedCardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            if (contact.photoUri != null) {
                                                AsyncImage(
                                                    model = contact.photoUri,
                                                    contentDescription = contact.name,
                                                    modifier = Modifier.size(56.dp).clip(CircleShape)
                                                )
                                            } else {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.size(56.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Call,
                                                        contentDescription = null,
                                                        modifier = Modifier.padding(12.dp),
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AutoSizeText(
                                                text = contact.name,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth(),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FilledTonalButton(
                            onClick = onOpenAllContacts,
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(text = stringResource(R.string.btn_all_contacts).uppercase(), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Call Confirmation Dialog
    contactToCall?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToCall = null },
            title = { Text(text = stringResource(R.string.confirm_call_title), style = MaterialTheme.typography.titleLarge) },
            text = { 
                Text(
                    text = stringResource(R.string.confirm_call_msg, "${contact.name} (${contact.phoneNumber})"),
                    style = MaterialTheme.typography.bodyLarge
                ) 
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { contactToCall = null },
                        modifier = Modifier.weight(1f).height(60.dp)
                    ) {
                        Text(text = stringResource(R.string.dlg_cancel).uppercase(), style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = {
                            onCallContact(contact)
                            contactToCall = null
                        },
                        modifier = Modifier.weight(1f).height(60.dp)
                    ) {
                        Text(text = stringResource(R.string.dlg_call).uppercase(), style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
            dismissButton = null
        )
    }

    // Remove App Favorite Dialog
    appToRemove?.let { app ->
        AlertDialog(
            onDismissRequest = { appToRemove = null },
            title = { Text(text = stringResource(R.string.dlg_edit_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(text = "Remove ${app.label} from favorites?", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { appToRemove = null },
                        modifier = Modifier.weight(1f).height(60.dp)
                    ) {
                        Text(text = stringResource(R.string.dlg_cancel).uppercase(), style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = {
                            onRemoveApp(app)
                            appToRemove = null
                        },
                        modifier = Modifier.weight(1f).height(60.dp)
                    ) {
                        Text(text = "REMOVE", style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
            dismissButton = null
        )
    }

    // Remove Contact Favorite Dialog
    contactToRemove?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToRemove = null },
            title = { Text(text = stringResource(R.string.dlg_edit_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(text = "Remove ${contact.name} from favorites?", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { contactToRemove = null },
                        modifier = Modifier.weight(1f).height(60.dp)
                    ) {
                        Text(text = stringResource(R.string.dlg_cancel).uppercase(), style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = {
                            onRemoveContact(contact)
                            contactToRemove = null
                        },
                        modifier = Modifier.weight(1f).height(60.dp)
                    ) {
                        Text(text = "REMOVE", style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
            dismissButton = null
        )
    }
}
