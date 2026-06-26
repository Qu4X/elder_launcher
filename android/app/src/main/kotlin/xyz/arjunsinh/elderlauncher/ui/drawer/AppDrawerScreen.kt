package xyz.arjunsinh.elderlauncher.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import xyz.arjunsinh.elderlauncher.R
import xyz.arjunsinh.elderlauncher.data.model.LauncherApp
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    allApps: List<LauncherApp>,
    favoritePackageNames: Set<String>,
    onToggleFavorite: (LauncherApp) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Handle system back gesture
    BackHandler {
        onBack()
    }

    val filteredApps = remember(searchQuery, allApps) {
        allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = paddingValues.calculateBottomPadding())
                .padding(horizontal = 16.dp)
        ) {
            // Compact Header Row to eliminate empty space
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_all_apps).uppercase(),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Material 3 SearchBar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {},
                active = false,
                onActiveChange = {},
                placeholder = { Text(text = stringResource(R.string.search_apps_hint), style = MaterialTheme.typography.bodyLarge) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(24.dp)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {}

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { app ->
                    val isFavorite = favoritePackageNames.contains(app.packageName)

                    var currentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                    Surface(
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
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                            .onGloballyPositioned { currentCoords = it }
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            leadingContent = {
                                AsyncImage(
                                    model = app.icon,
                                    contentDescription = app.label,
                                    modifier = Modifier.size(56.dp)
                                )
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = { onToggleFavorite(app) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Toggle Favorite",
                                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
