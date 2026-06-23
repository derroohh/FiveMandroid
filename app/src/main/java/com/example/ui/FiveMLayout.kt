package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ServerEntity
import com.example.data.UserProfileEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class FiveMTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    SERVERS("Servers", Icons.Default.List),
    DIRECT("Direct Connect", Icons.Default.ArrowForward),
    CONSOLE("Console", Icons.Default.Info),
    PROFILE("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiveMMainLayout(viewModel: FiveMViewModel) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val serverList by viewModel.serverList.collectAsStateWithLifecycle()
    val connState by viewModel.connectionState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(FiveMTab.HOME) }
    val scope = rememberCoroutineScope()

    // Screen dynamic container
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Polished FiveM branded Helix badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FivemOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "5",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Column {
                            Text(
                                "FiveM Mobile Client",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                "Cfx.re network platform simulation",
                                fontSize = 11.sp,
                                color = FivemTextMuted
                            )
                        }
                    }
                },
                actions = {
                    // Diagnostic online counters
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(FivemBorderGrey)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(FivemSuccess)
                            )
                            Text(
                                "Nodes: 145,210 online",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FivemWhite
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FivemDarkBg,
                    titleContentColor = FivemWhite
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = FivemDeepGrey,
                tonalElevation = 12.dp
            ) {
                FiveMTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FivemOrange,
                            selectedTextColor = FivemOrange,
                            unselectedIconColor = FivemTextMuted,
                            unselectedTextColor = FivemTextMuted,
                            indicatorColor = FivemBorderGrey
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FivemDarkBg)
        ) {
            // Main views transit
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_transit"
            ) { targetTab ->
                when (targetTab) {
                    FiveMTab.HOME -> HomeScreen(viewModel, onNavigateToServers = { activeTab = FiveMTab.SERVERS })
                    FiveMTab.SERVERS -> ServersScreen(viewModel)
                    FiveMTab.DIRECT -> DirectConnectScreen(viewModel)
                    FiveMTab.CONSOLE -> TerminalConsoleScreen(viewModel)
                    FiveMTab.PROFILE -> SettingsScreen(viewModel)
                }
            }

            // Connection Modal / Panel Overlay (Active connecting simulator)
            if (connState is ConnectionState.Connecting) {
                val state = connState as ConnectionState.Connecting
                ConnectingOverlay(state, onCancel = { viewModel.disconnect() })
            } else if (connState is ConnectionState.Connected) {
                val state = connState as ConnectionState.Connected
                ActiveLobbySandbox(state, onDisconnect = { viewModel.disconnect() }, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: FiveMViewModel, onNavigateToServers: () -> Unit) {
    val scrollState = rememberScrollState()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero visual Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, FivemBorderGrey, RoundedCornerShape(12.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_fivem_banner),
                contentDescription = "FiveM Hero Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Linear glow scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, FivemDarkBg.copy(alpha = 0.9f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(FivemOrange)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("ACTIVE UPDATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Welcome back, ${profileState.username}!",
                    style = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = FivemWhite
                    )
                )
                Text(
                    "PC modification framework successfully recreated fully offline on Android.",
                    fontSize = 11.sp,
                    color = FivemTextMuted
                )
            }
        }

        // Fast shortcut connections
        Text("Quick Connect Actions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FivemOrange)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Shortcut NoPixel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FivemDeepGrey)
                    .border(1.dp, FivemBorderGrey, RoundedCornerShape(10.dp))
                    .clickable {
                        viewModel.requestConnect(
                            ServerEntity(
                                id = "nopixel",
                                name = "NoPixel 4.0 - Official Android Client",
                                description = "",
                                playersCount = 985,
                                maxPlayers = 1000,
                                ping = 15,
                                tags = "",
                                category = "roleplay"
                            )
                        )
                    }
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nopixel 4.0", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
                    Text("Serious RP Sandbox", fontSize = 10.sp, color = FivemTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Join", tint = FivemOrange, modifier = Modifier.size(16.dp))
                        Text("FAST LOAD", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = FivemOrange)
                    }
                }
            }

            // Shortcut Drift
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FivemDeepGrey)
                    .border(1.dp, FivemBorderGrey, RoundedCornerShape(10.dp))
                    .clickable {
                        viewModel.requestConnect(
                            ServerEntity(
                                id = "hyperdrift",
                                name = "Mt. Haruna Custom drift Sandbox",
                                description = "",
                                playersCount = 188,
                                maxPlayers = 300,
                                ping = 45,
                                tags = "",
                                category = "drift"
                            )
                        )
                    }
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mt. Haruna", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
                    Text("Drift physics tests", fontSize = 10.sp, color = FivemTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Join", tint = FivemOrange, modifier = Modifier.size(16.dp))
                        Text("FAST LOAD", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = FivemOrange)
                    }
                }
            }
        }

        // Diagnostic information panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FivemDeepGrey),
            border = BorderStroke(1.dp, FivemBorderGrey)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Cfx Hardware Diagnostics", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
                Divider(color = FivemBorderGrey)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("NUI UI Acceleration", fontSize = 11.sp, color = FivemTextMuted)
                    Text(if (profileState.nuiHardwareAcceleration) "ENABLED (VULKAN)" else "DISABLED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FivemSuccess)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Voice Synchronizer Channel", fontSize = 11.sp, color = FivemTextMuted)
                    Text(profileState.voiceChannel.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FivemOrange)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Offline Data Sync Cache", fontSize = 11.sp, color = FivemTextMuted)
                    Text("${profileState.assetCacheSizeMb} MB (OPTIMIZED)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Rendering Tickrate Target", fontSize = 11.sp, color = FivemTextMuted)
                    Text("60 Hz (STABLE)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FivemSuccess)
                }
            }
        }

        // News articles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Latest Cfx Updates", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
            Text(
                "See all servers",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = FivemOrange,
                modifier = Modifier.clickable { onNavigateToServers() }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NewsItem(
                title = "Android APK optimized resource delivery",
                desc = "We implemented full offline simulation data compression to minimize file footprint on mobile environments.",
                date = "June 2026"
            )
            NewsItem(
                title = "Fast dynamic terminal console integration",
                desc = "Open the 'Console' tab to execute standard diagnostic commands manually exactly like PC client (~ key syntax).",
                date = "June 2026"
            )
            NewsItem(
                title = "Room Database favorites & history enabled",
                desc = "Local history records synchronize immediately when connecting to any real or custom simulated server IP node.",
                date = "May 2026"
            )
        }
    }
}

@Composable
fun NewsItem(title: String, desc: String, date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(FivemDeepGrey)
            .border(1.dp, FivemBorderGrey, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
                Text(date, fontSize = 9.sp, color = FivemTextMuted)
            }
            Text(desc, fontSize = 10.sp, color = FivemTextMuted, lineHeight = 14.sp)
        }
    }
}

@Composable
fun ServersScreen(viewModel: FiveMViewModel) {
    val servers by viewModel.serverList.collectAsStateWithLifecycle()
    val search by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val activeSort by viewModel.activeSort.collectAsStateWithLifecycle()

    var expandedServerId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search & Sort Box
        OutlinedTextField(
            value = search,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search servers by name, tags, description...", color = FivemTextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = FivemTextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("server_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FivemDeepGrey,
                unfocusedContainerColor = FivemDeepGrey,
                focusedBorderColor = FivemOrange,
                unfocusedBorderColor = FivemBorderGrey,
                focusedTextColor = FivemWhite,
                unfocusedTextColor = FivemWhite
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        // Categories selector scroll row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                "all" to "ALL",
                "roleplay" to "ROLEPLAY",
                "drift" to "DRIFT",
                "racing" to "RACING",
                "favorites" to "FAVORITES",
                "history" to "HISTORY"
            )
            items(categories) { (slug, title) ->
                val isSelected = activeCategory == slug
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) FivemOrange else FivemDeepGrey)
                        .border(1.dp, if (isSelected) FivemOrange else FivemBorderGrey, RoundedCornerShape(8.dp))
                        .clickable { viewModel.selectedCategory.value = slug }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FivemWhite
                    )
                }
            }
        }

        // Sorters selector row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Showing ${servers.size} servers",
                fontSize = 12.sp,
                color = FivemTextMuted,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Sort:", fontSize = 11.sp, color = FivemTextMuted)
                val sorts = listOf("players" to "Players", "ping" to "Ping", "name" to "Alphabetical")
                sorts.forEach { (slug, label) ->
                    val isSelected = activeSort == slug
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (isSelected) FivemOrange else FivemTextMuted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clickable { viewModel.activeSort.value = slug }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Servers list
        if (servers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = "Empty", tint = FivemBorderGrey, modifier = Modifier.size(54.dp))
                    Text("No matching Cfx servers found offline.", color = FivemTextMuted, fontSize = 13.sp)
                    Text("Try adjusting filters or clear your search criteria.", color = FivemTextMuted, fontSize = 11.sp)
                    Button(
                        onClick = {
                            viewModel.searchQuery.value = ""
                            viewModel.selectedCategory.value = "all"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FivemOrange)
                    ) {
                        Text("Reset Filters")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(servers, key = { it.id }) { server ->
                    val isExpanded = expandedServerId == server.id
                    ServerGridCard(
                        server = server,
                        isExpanded = isExpanded,
                        onExpandToggle = {
                            expandedServerId = if (isExpanded) null else server.id
                        },
                        onConnect = { viewModel.requestConnect(server) },
                        onToggleFavorite = { viewModel.toggleFavorite(server.id, !server.isFavorite) }
                    )
                }
            }
        }
    }
}

@Composable
fun ServerGridCard(
    server: ServerEntity,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onConnect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    // Elegant distinct card gradient indices
    val gradientColors = when (server.bannerGradientIndex) {
        1 -> listOf(Color(0xFF2C3E50), Color(0xFF3498DB)) // Eclipse Blue
        2 -> listOf(Color(0xFF8E44AD), Color(0xFF9B59B6)) // Drift Purple
        3 -> listOf(Color(0xFF27AE60), Color(0xFF2ECC71)) // Survival Green
        4 -> listOf(Color(0xFFE67E22), Color(0xFFF39C12)) // Sand Orange
        5 -> listOf(Color(0xFFC0392B), Color(0xFFE74C3C)) // Racing Red
        else -> listOf(Color(0xFF232526), Color(0xFF414345)) // Dark NoPixel standard slate
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandToggle() }
            .testTag("server_card_${server.id}"),
        colors = CardDefaults.cardColors(containerColor = FivemDeepGrey),
        border = BorderStroke(1.dp, if (isExpanded) FivemOrange.copy(alpha = 0.8f) else FivemBorderGrey)
    ) {
        Column {
            // Header visual indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Brush.horizontalGradient(gradientColors))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(FivemDarkBg.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = server.category.uppercase(),
                            color = FivemOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }

                    // Favorite Star shortcut button
                    IconButton(
                        onClick = { onToggleFavorite() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (server.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Fav",
                            tint = if (server.isFavorite) FivemOrange else FivemWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Central details row
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = server.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FivemWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Build: ${server.gameBuild} | Uptime: ${server.uptimeByPercent}%",
                            fontSize = 10.sp,
                            color = FivemTextMuted
                        )
                    }

                    // Ping Latency box
                    val pingColor = when {
                        server.ping < 20 -> FivemSuccess
                        server.ping < 45 -> FivemBlue
                        else -> FivemOrange
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(pingColor)
                        )
                        Text(
                            text = "${server.ping}ms",
                            color = pingColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tag lines
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    server.tags.split(",").take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(FivemBorderGrey)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(tag.trim(), fontSize = 9.sp, color = FivemTextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Players indicator meter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Players: ${server.playersCount} / ${server.maxPlayers}",
                        fontSize = 11.sp,
                        color = FivemWhite,
                        fontWeight = FontWeight.Bold
                    )
                    LinearProgressIndicator(
                        progress = { server.playersCount.toFloat() / server.maxPlayers.toFloat() },
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = FivemOrange,
                        trackColor = FivemBorderGrey
                    )
                }

                // Expanded Section description / connect action
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Divider(color = FivemBorderGrey)
                        Text(
                            text = server.description,
                            fontSize = 11.sp,
                            color = FivemTextMuted,
                            lineHeight = 15.sp
                        )

                        // Included resources lists simulation
                        Text(
                            text = "Connected Cfx Resource Assemblies:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FivemWhite
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val dummyScripts = listOf("es_extended", "pma-voice", "ox_lib", "qb-inventory")
                            dummyScripts.forEach { script ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(FivemDarkBg)
                                        .border(1.dp, FivemBorderGrey)
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(script, fontSize = 8.sp, color = FivemSuccess, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Big connect button
                        Button(
                            onClick = { onConnect() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_connect_${server.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = FivemOrange),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Connect")
                                Text("CONNECT TO SERVER", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DirectConnectScreen(viewModel: FiveMViewModel) {
    var ipInput by remember { mutableStateOf("") }
    val history by viewModel.repository.historyFlow.collectAsStateWithLifecycle(emptyList())

    val isIpValid = remember(ipInput) {
        val trimmed = ipInput.trim()
        trimmed.isNotEmpty() && (trimmed.contains(".") || trimmed.contains(":"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top direct card info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(FivemDeepGrey)
                .border(1.dp, FivemBorderGrey, RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Direct Connect Terminal",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = FivemWhite
                )
                Text(
                    "If you have local developer GTA servers running, enter the remote IP address and socket port below to boot fast client synclink.",
                    fontSize = 11.sp,
                    color = FivemTextMuted,
                    lineHeight = 15.sp
                )
            }
        }

        // Input forms
        OutlinedTextField(
            value = ipInput,
            onValueChange = { ipInput = it },
            placeholder = { Text("192.168.1.105:30120", color = FivemTextMuted) },
            label = { Text("Target IP Address:Port", color = FivemTextMuted) },
            leadingIcon = { Icon(Icons.Default.ArrowForward, contentDescription = "IP", tint = FivemOrange) },
            trailingIcon = {
                if (ipInput.isNotEmpty()) {
                    IconButton(onClick = { ipInput = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = FivemTextMuted)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("direct_ip_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FivemDeepGrey,
                unfocusedContainerColor = FivemDeepGrey,
                focusedBorderColor = FivemOrange,
                unfocusedBorderColor = FivemBorderGrey,
                focusedTextColor = FivemWhite,
                unfocusedTextColor = FivemWhite
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (isIpValid) viewModel.requestDirectConnect(ipInput)
            })
        )

        // Invalid validation helper
        if (ipInput.isNotEmpty() && !isIpValid) {
            Text(
                "Warning: IP must represent node standard format (e.g. localhost:30120)",
                color = FivemError,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Action button
        Button(
            onClick = { viewModel.requestDirectConnect(ipInput) },
            enabled = isIpValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_direct_connect"),
            colors = ButtonDefaults.buttonColors(
                containerColor = FivemOrange,
                disabledContainerColor = FivemDeepGrey
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "ESTABLISH FXTUNNEL SYNC",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isIpValid) FivemWhite else FivemTextMuted
            )
        }

        // History logs
        Text(
            "Recent Direct Fastlinks",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = FivemWhite
        )

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No historic connection tunnels recorded.",
                    fontSize = 11.sp,
                    color = FivemTextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { record ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(FivemDeepGrey)
                            .border(1.dp, FivemBorderGrey, RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.requestDirectConnect(record.ipAddress)
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(record.ipAddress, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FivemOrange, fontFamily = FontFamily.Monospace)
                                Text(record.serverName, fontSize = 10.sp, color = FivemTextMuted)
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Reconnect", tint = FivemSuccess, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.clearHistory() },
                        colors = ButtonDefaults.textButtonColors(contentColor = FivemError),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CLEAR HISTORY LOGS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalConsoleScreen(viewModel: FiveMViewModel) {
    var consoleInput by remember { mutableStateOf("") }
    val logs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val kbController = LocalSoftwareKeyboardController.current

    // Auto-scroll to bottom on new logs
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            lazyListState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Interactive screen instructions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Diagnostic Command Log", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(FivemSuccess.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("FXCONSOLE STABLE", color = FivemSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Terminal text monitor frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .border(1.dp, FivemBorderGrey, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(logs) { log ->
                    val color = when (log.type) {
                        "input" -> FivemWhite
                        "success" -> FivemSuccess
                        "error" -> FivemError
                        else -> FivemOrange
                    }
                    Text(
                        text = log.text,
                        color = color,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }

        // CommandLine input
        OutlinedTextField(
            value = consoleInput,
            onValueChange = { consoleInput = it },
            placeholder = { Text("type command (e.g. 'help', 'ping', 'status')...", color = FivemTextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("console_command_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FivemDeepGrey,
                unfocusedContainerColor = FivemDeepGrey,
                focusedBorderColor = FivemOrange,
                unfocusedBorderColor = FivemBorderGrey,
                focusedTextColor = FivemWhite,
                unfocusedTextColor = FivemWhite
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (consoleInput.trim().isNotEmpty()) {
                    viewModel.runTerminalCommand(consoleInput)
                    consoleInput = ""
                    kbController?.hide()
                }
            }),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (consoleInput.trim().isNotEmpty()) {
                            viewModel.runTerminalCommand(consoleInput)
                            consoleInput = ""
                            kbController?.hide()
                        }
                    }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = FivemSuccess)
                }
            }
        )
    }
}

@Composable
fun SettingsScreen(viewModel: FiveMViewModel) {
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var editNameInput by remember(profile.username) { mutableStateOf(profile.username) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Client Customization", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FivemOrange)

        // Username customization card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FivemDeepGrey),
            border = BorderStroke(1.dp, FivemBorderGrey)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Cfx Nickname", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("username_settings_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FivemDarkBg,
                            unfocusedContainerColor = FivemDarkBg,
                            focusedBorderColor = FivemOrange,
                            unfocusedBorderColor = FivemBorderGrey,
                            focusedTextColor = FivemWhite,
                            unfocusedTextColor = FivemWhite
                        )
                    )

                    Button(
                        onClick = { viewModel.updateUsername(editNameInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = FivemOrange)
                    ) {
                        Text("SAVE")
                    }
                }
            }
        }

        // Avatar selector
        Text("Active GTA Avatar Model", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
        val avatars = listOf("Franklin", "Michael", "Trevor", "Lamar", "Lester", "Packie")

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(avatars) { avatar ->
                val isSelected = profile.avatarName == avatar
                Box(
                    modifier = Modifier
                        .size(height = 60.dp, width = 85.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) FivemOrange else FivemDeepGrey)
                        .border(1.dp, if (isSelected) FivemWhite else FivemBorderGrey, RoundedCornerShape(8.dp))
                        .clickable { viewModel.updateAvatar(avatar) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Person, contentDescription = avatar, tint = if (isSelected) FivemWhite else FivemTextMuted)
                        Text(avatar, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
                    }
                }
            }
        }

        // Optimization sliders / controls
        Text("Hardware Optimizations", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FivemWhite)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = FivemDeepGrey),
            border = BorderStroke(1.dp, FivemBorderGrey)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Acceleration toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NUI Vulkan Core Acceleration", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
                        Text("Enables faster UI transitions and limits lagging on low-end devices.", fontSize = 9.sp, color = FivemTextMuted)
                    }
                    Switch(
                        checked = profile.nuiHardwareAcceleration,
                        onCheckedChange = { viewModel.toggleNuiAcceleration(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = FivemSuccess, checkedTrackColor = FivemSuccess.copy(alpha = 0.4f))
                    )
                }

                Divider(color = FivemBorderGrey)

                // Cache size manager
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Local Compiled Assets Cache", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
                        Text("Script assemblies and skin buffers: ${profile.assetCacheSizeMb} MB.", fontSize = 9.sp, color = FivemTextMuted)
                    }
                    Button(
                        onClick = { viewModel.clearCachedFiles() },
                        colors = ButtonDefaults.buttonColors(containerColor = FivemError),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("PURGE CACHE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Divider(color = FivemBorderGrey)

                // Voice configuration channel
                Text("Default VoIP Client channel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
                val channels = listOf("pma-voice" to "PMA-Voice Sync", "mumble" to "Legacy Mumble", "saltychat" to "SaltyChat TS3")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    channels.forEach { (slug, name) ->
                        val isSelected = profile.voiceChannel == slug
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) FivemOrange.copy(alpha = 0.2f) else FivemDarkBg)
                                .border(1.dp, if (isSelected) FivemOrange else FivemBorderGrey, RoundedCornerShape(6.dp))
                                .clickable { viewModel.updateVoiceChannel(slug) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name, fontSize = 10.sp, color = if (isSelected) FivemOrange else FivemTextMuted, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectingOverlay(state: ConnectionState.Connecting, onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) {} // block click-throughs
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Spinning stylized helix loader
            CircularProgressIndicator(
                color = FivemOrange,
                strokeWidth = 4.dp,
                modifier = Modifier.size(54.dp)
            )

            Text(
                text = "Connecting to ${state.serverName}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FivemWhite,
                textAlign = TextAlign.Center
            )

            // Linear progressive tracker
            LinearProgressIndicator(
                progress = { state.progress.toFloat() / 100f },
                color = FivemOrange,
                trackColor = FivemBorderGrey,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Asset Download Handclasp", fontSize = 11.sp, color = FivemTextMuted)
                Text("${state.progress}% Complete", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FivemOrange)
            }

            // Real-time terminal downloading output
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(FivemDeepGrey)
                    .border(1.dp, FivemBorderGrey, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.logs) { log ->
                        Text(
                            text = log,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = FivemSuccess,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ABORT BUTTON
            Button(
                onClick = { onCancel() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.width(180.dp)
            ) {
                Text("ABORT CONNECTING", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = FivemWhite)
            }
        }
    }
}

@Composable
fun ActiveLobbySandbox(
    state: ConnectionState.Connected,
    onDisconnect: () -> Unit,
    viewModel: FiveMViewModel
) {
    val chatMessages by viewModel.lobbyChat.collectAsStateWithLifecycle()
    val health by viewModel.localPlayerHealth.collectAsStateWithLifecycle()
    val armor by viewModel.localPlayerArmor.collectAsStateWithLifecycle()
    val cash by viewModel.localPlayerCash.collectAsStateWithLifecycle()
    val activeDiag by viewModel.developerDiagnosticsEnabled.collectAsStateWithLifecycle()

    var chatInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val chatListState = rememberLazyListState()

    // Auto-scroll chat
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FivemDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Connected status banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FivemSuccess))
                        Text("ESTABLISHED SYNC", fontSize = 11.sp, color = FivemSuccess, fontWeight = FontWeight.Bold)
                    }
                    Text(state.serverName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FivemWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = { onDisconnect() },
                    colors = ButtonDefaults.buttonColors(containerColor = FivemError),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("DISCONNECT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Real-time simulated developer telemetry graphs
            AnimatedVisibility(visible = activeDiag) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, FivemOrange.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Active Cfx Diagnostic Overlay (~)", fontSize = 11.sp, color = FivemOrange, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Divider(color = FivemBorderGrey)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Simulated Telemetry FPS: 60.2 fps", fontSize = 9.sp, color = FivemSuccess, fontFamily = FontFamily.Monospace)
                            Text("Tick Rate: 60Hz stable", fontSize = 9.sp, color = FivemSuccess, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Active scripts pool: 142 heap sizes", fontSize = 9.sp, color = FivemWhite, fontFamily = FontFamily.Monospace)
                            Text("Render overhead lag: 1.42 ms", fontSize = 9.sp, color = FivemBlue, fontFamily = FontFamily.Monospace)
                        }
                        // Draw simulated telemetry line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(25.dp)
                                .background(FivemDeepGrey)
                                .drawBehind {
                                    val points = listOf(
                                        15f, 18f, 11f, 15f,
                                        22f, 12f, 15f, 16f,
                                        15f, 18f, 11f, 15f,
                                        22f, 12f, 15f, 16f
                                    )
                                    val step = size.width / points.size
                                    for (i in 0 until points.size - 1) {
                                        drawLine(
                                            color = Color(0xFF00E676),
                                            start = Offset(i * step, size.height - points[i]),
                                            end = Offset((i + 1) * step, size.height - points[i + 1]),
                                            strokeWidth = 2.dp.toPx()
                                        )
                                    }
                                }
                        )
                    }
                }
            }

            // Interactive character metrics HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Lifeline Health bar
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CHARACTER HEALTH", fontSize = 9.sp, color = FivemTextMuted)
                        Text("$health%", fontSize = 9.sp, color = FivemSuccess, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { health.toFloat() / 100f },
                        color = FivemSuccess,
                        trackColor = FivemBorderGrey,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                }

                // Lifeline Armor bar
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ARMOR VEST", fontSize = 9.sp, color = FivemTextMuted)
                        Text("$armor%", fontSize = 9.sp, color = FivemBlue, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { armor.toFloat() / 100f },
                        color = FivemBlue,
                        trackColor = FivemBorderGrey,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            // HUD details (Cash values and trigger shortcut actions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LS Cash Balance: $$cash",
                    fontSize = 13.sp,
                    color = FivemSuccess,
                    fontWeight = FontWeight.Black
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Diagnostic toggle
                    Button(
                        onClick = { viewModel.developerDiagnosticsEnabled.value = !activeDiag },
                        colors = ButtonDefaults.buttonColors(containerColor = FivemDeepGrey),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("DIAG OVERLAY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FivemWhite)
                    }
                }
            }

            // Server Local Chat Simulator box
            Text("Simulated Server Local Chat Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FivemOrange)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, FivemBorderGrey, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(state = chatListState, modifier = Modifier.fillMaxSize()) {
                    items(chatMessages) { chat ->
                        val isLocal = chat.contains("(Local):")
                        val isSystem = chat.startsWith("System:")
                        val color = when {
                            isSystem -> FivemSuccess
                            isLocal -> FivemOrange
                            else -> FivemWhite
                        }
                        Text(
                            text = chat,
                            fontSize = 11.sp,
                            color = color,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }

            // Chat input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = { Text("type chat msg or try '/car adder' '/heal'...", color = FivemTextMuted, fontSize = 11.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lobby_chat_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FivemDeepGrey,
                        unfocusedContainerColor = FivemDeepGrey,
                        focusedBorderColor = FivemOrange,
                        unfocusedBorderColor = FivemBorderGrey,
                        focusedTextColor = FivemWhite,
                        unfocusedTextColor = FivemWhite
                    ),
                    shape = RoundedCornerShape(6.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (chatInput.trim().isNotEmpty()) {
                            viewModel.sendLobbyChatMessage(chatInput)
                            chatInput = ""
                        }
                    })
                )

                Button(
                    onClick = {
                        if (chatInput.trim().isNotEmpty()) {
                            viewModel.sendLobbyChatMessage(chatInput)
                            chatInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FivemOrange),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("SEND", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
