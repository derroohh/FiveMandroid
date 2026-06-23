package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface ConnectionState {
    object Idle : ConnectionState
    data class Connecting(
        val serverName: String,
        val progress: Int,
        val statusMessage: String,
        val logs: List<String>
    ) : ConnectionState
    data class Connected(
        val serverId: String,
        val serverName: String,
        val currentPlayers: Int,
        val maxPlayers: Int,
        val gameBuild: String,
        val isDevCluster: Boolean = false
    ) : ConnectionState
}

data class TerminalLine(
    val text: String,
    val type: String // "input", "info", "success", "error"
)

class FiveMViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val repository = FiveMRepository(db)

    // UI States
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("all") // "all", "roleplay", "drift", "freeroam", "racing", "favorites", "history"
    val activeSort = MutableStateFlow("players") // "players", "ping", "name"

    // Profile & Settings
    val profileState = repository.profileFlow
        .map { it ?: UserProfileEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserProfileEntity())

    // Active connection simulation states
    val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    private var connectionJob: Job? = null

    // Server lobby interactive states
    val lobbyChat = MutableStateFlow<List<String>>(listOf(
        "System: Welcome to Los Santos Roleplay cluster!",
        "Lspd_Officer_423: Active pursuit 10-80 near Elgin Ave, all units code 3!",
        "Dealer_X: Anyone got lockpicks for sale near Pillbox Hill?",
        "Franklin_Clinton (Local): Simulated server environment is running flawlessly.",
        "Server_Host: Network synchronizer operating at 60Hz tickrate."
    ))
    val localPlayerHealth = MutableStateFlow(100)
    val localPlayerArmor = MutableStateFlow(50)
    val localPlayerCash = MutableStateFlow(15420)
    val activeRadioChannel = MutableStateFlow("None")
    val developerDiagnosticsEnabled = MutableStateFlow(false)

    // Terminal State
    val terminalLogs = MutableStateFlow<List<TerminalLine>>(listOf(
        TerminalLine("Cfx.re Android Platform Console [Version 1.0.24-b3095]", "info"),
        TerminalLine("Type 'help' to list available command diagnostic protocols.", "info"),
        TerminalLine("", "info")
    ))

    // Server List computed by combining all flow states
    val serverList: StateFlow<List<ServerEntity>> = combine(
        repository.allServersFlow,
        searchQuery,
        selectedCategory,
        activeSort,
        repository.historyFlow
    ) { allServers, search, category, sort, historyList ->
        var list = allServers

        // Filter by category
        list = when (category) {
            "all" -> list
            "favorites" -> list.filter { it.isFavorite }
            "history" -> {
                // Return servers that match history names
                val historyNames = historyList.map { it.serverName }.toSet()
                list.filter { it.name in historyNames }
            }
            else -> list.filter { it.category.equals(category, ignoreCase = true) }
        }

        // Filter by search query
        if (search.isNotBlank()) {
            list = list.filter {
                it.name.contains(search, ignoreCase = true) ||
                        it.tags.contains(search, ignoreCase = true) ||
                        it.description.contains(search, ignoreCase = true)
            }
        }

        // Sort application
        list = when (sort) {
            "players" -> list.sortedByDescending { it.playersCount }
            "ping" -> list.sortedBy { it.ping }
            "name" -> list.sortedBy { it.name }
            else -> list
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.prepopulateDatabaseIfNeeded()
        }
    }

    // Toggle Favorite
    fun toggleFavorite(serverId: String, isNowFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(serverId, isNowFav)
        }
    }

    // Connect trigger
    fun requestConnect(server: ServerEntity) {
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            repository.addConnectionHistory("local.server.id:${server.id}", server.name)
            runConnectSimulation(server.name, server.id, server.playersCount, server.maxPlayers, server.gameBuild)
        }
    }

    fun requestDirectConnect(ip: String) {
        val trimmed = ip.trim()
        if (trimmed.isEmpty()) return
        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            repository.addConnectionHistory(trimmed, "Direct Connect Cluster [$trimmed]")
            runConnectSimulation("Direct: $trimmed", "direct_connect", 1, 32, "b3095", isDevCluster = true)
        }
    }

    private suspend fun runConnectSimulation(
        name: String,
        id: String,
        players: Int,
        maxPlayers: Int,
        build: String,
        isDevCluster: Boolean = false
    ) {
        val logSequence = listOf(
            "Connecting to FXServer: handshake handclasp with Rockstar Authentication protocol...",
            "Established socket bridge: dynamic remote host verification passed.",
            "Retrieving remote server information...",
            "Downloading script package: [pma-voice] (1.2 MB / 1.2 MB)... Complete.",
            "Downloading script package: [es_extended] (8.4 MB / 14.5 MB)...",
            "Downloading script package: [es_extended] (14.5 MB / 14.5 MB)... Complete.",
            "Downloading script package: [qb-core] (5.2 MB / 5.2 MB)... Complete.",
            "Downloading script package: [ox_lib] (3.8 MB / 3.8 MB)... Complete.",
            "Checking local client assets database cache...",
            "Validating assets integrity signatures (0 bad chunks, 0 corrupt blocks).",
            "Initializing NUI rendering thread...",
            "Creating hardware UI sandbox canvas...",
            "Authenticating local nickname profile and user parameters...",
            "Executing remote resource boot-sequence: loading client-side assembly script cache...",
            "Synchronizing dynamic world entity state arrays...",
            "Instantiating local ped entity: player model successfully spawned at X: 112.5, Y: -845.2, Z: 54.8"
        )
        val logs = mutableListOf<String>()
        logs.add("Core: Requesting network socket tunnel...")

        for (progress in 0..100 step Random.nextInt(5, 15)) {
            val progressVal = progress.coerceAtMost(100)
            val logIdx = (progressVal * logSequence.size / 101).coerceIn(0, logSequence.size - 1)
            val statusMsg = logSequence[logIdx]
            if (logs.lastOrNull() != statusMsg) {
                logs.add(statusMsg)
            }
            connectionState.value = ConnectionState.Connecting(
                serverName = name,
                progress = progressVal,
                statusMessage = statusMsg,
                logs = logs.toList()
            )
            delay(Random.nextLong(120, 240))
        }

        // Final transition
        connectionState.value = ConnectionState.Connecting(
            serverName = name,
            progress = 100,
            statusMessage = "World stream initialized successfully.",
            logs = logs + "Boot sequence accomplished. Entering safe interactive state."
        )
        delay(300)
        connectionState.value = ConnectionState.Connected(
            serverId = id,
            serverName = name,
            currentPlayers = players,
            maxPlayers = maxPlayers,
            gameBuild = build,
            isDevCluster = isDevCluster
        )
    }

    fun disconnect() {
        connectionJob?.cancel()
        connectionState.value = ConnectionState.Idle
    }

    // Lobby simulation actions
    fun sendLobbyChatMessage(msg: String) {
        val trimmed = msg.trim()
        if (trimmed.isEmpty()) return
        val currentProfile = profileState.value
        viewModelScope.launch {
            lobbyChat.value = lobbyChat.value + "${currentProfile.username} (Local): $trimmed"
            if (trimmed.startsWith("/")) {
                delay(300)
                handleCommandSimulation(trimmed)
            } else {
                // Simulate friendly automatic AI response
                delay(1200)
                val automaticResponses = listOf(
                    "GTA_Fanatic: Hell yeah, welcome aboard ${currentProfile.username}!",
                    "Admin_Tony: Keep it serious RP guys, obey the rules. Use chat prefix /help for custom commands.",
                    "Hacker_LS: Nice car choice. Spawn a custom tuner with /car adder",
                    "Cop_Jack: We are hosting a high-speed vehicle race tracking near the airport soon!"
                )
                lobbyChat.value = lobbyChat.value + automaticResponses.random()
            }
        }
    }

    private fun handleCommandSimulation(cmd: String) {
        val parts = cmd.lowercase().split(" ")
        val trigger = parts[0]
        when (trigger) {
            "/car" -> {
                val carName = if (parts.size > 1) parts[1] else "t20"
                lobbyChat.value = lobbyChat.value + "System: Spawned vehicle [$carName] at player location successfully!"
                localPlayerCash.value = (localPlayerCash.value - 200).coerceAtLeast(0)
            }
            "/kit" -> {
                lobbyChat.value = lobbyChat.value + "System: Weapons kit [Assault rifle/Pistol/Radio] equipped!"
                localPlayerArmor.value = 100
            }
            "/tp" -> {
                val dest = if (parts.size > 1) parts[1] else "pillbox"
                lobbyChat.value = lobbyChat.value + "System: Teleported to dest [$dest] successfully coordinates: X: 345.1, Y: -202.9"
            }
            "/heal" -> {
                lobbyChat.value = lobbyChat.value + "System: Medical syringe consumed. Fully restored!"
                localPlayerHealth.value = 100
            }
            "/rules" -> {
                lobbyChat.value = lobbyChat.value + "System Rules: 1. FailRP is bannable. 2. Must use active microphone. 3. VDM/RDM is strictly prohibited."
            }
            else -> {
                lobbyChat.value = lobbyChat.value + "System: Unknown simulator command. Try /car, /heal, /kit, /tp, or /rules."
            }
        }
    }

    // Terminal console executor
    fun runTerminalCommand(command: String) {
        val cmd = command.trim()
        if (cmd.isEmpty()) return

        val newLogs = terminalLogs.value.toMutableList()
        newLogs.add(TerminalLine("> $cmd", "input"))

        val parts = cmd.lowercase().split(" ")
        val action = parts[0]

        when (action) {
            "help" -> {
                newLogs.add(TerminalLine("Core Cfx client command protocols representable:", "success"))
                newLogs.add(TerminalLine("  connect <ip_addr> : Fast bypass tunnel connect to target IP node.", "info"))
                newLogs.add(TerminalLine("  ping              : Execute active network ICMP latency test loop.", "info"))
                newLogs.add(TerminalLine("  clear             : Wipe the system diagnostic logs state clean.", "info"))
                newLogs.add(TerminalLine("  profile           : Render current active user credentials profile status.", "info"))
                newLogs.add(TerminalLine("  cache_clear       : Purge cache script directories to boost APK performance.", "info"))
                newLogs.add(TerminalLine("  fps <num>         : Toggle simulated display target FPS cap.", "info"))
                newLogs.add(TerminalLine("  status            : Print system memory, client tickrate, database health index.", "info"))
                newLogs.add(TerminalLine("  disconnect        : Kill existing server connecting socket state.", "info"))
            }
            "connect" -> {
                if (parts.size > 1) {
                    val ip = parts[1]
                    newLogs.add(TerminalLine("Routing fast connect socket to: $ip", "success"))
                    terminalLogs.value = newLogs
                    requestDirectConnect(ip)
                    return
                } else {
                    newLogs.add(TerminalLine("Error: Missing target node. Usage: connect <ip:port>", "error"))
                }
            }
            "ping" -> {
                newLogs.add(TerminalLine("Pinging Cfx.re root validation server [104.22.42.115] with 32 bytes data:", "info"))
                newLogs.add(TerminalLine("  Reply from 104.22.42.115: bytes=32 time=24ms TTL=54", "success"))
                newLogs.add(TerminalLine("  Reply from 104.22.42.115: bytes=32 time=19ms TTL=54", "success"))
                newLogs.add(TerminalLine("Ping metrics statistics: Pass=2, Fail=0 (0% loss), Avg Latency=21ms", "success"))
            }
            "clear" -> {
                terminalLogs.value = listOf(TerminalLine("Diagnostic buffer cleared.", "info"))
                return
            }
            "profile" -> {
                val prof = profileState.value
                newLogs.add(TerminalLine("Active Cfx User: ${prof.username}", "success"))
                newLogs.add(TerminalLine("Avatar Model: ${prof.avatarName} character asset file", "info"))
                newLogs.add(TerminalLine("Sync Offline Data: ${prof.syncOffline}", "info"))
                newLogs.add(TerminalLine("Acceleration State: ${prof.nuiHardwareAcceleration}", "info"))
            }
            "cache_clear" -> {
                newLogs.add(TerminalLine("Purging assets index table...", "info"))
                newLogs.add(TerminalLine("Rebuilding resource assemblies directory... Freed 142.4 MB.", "success"))
                viewModelScope.launch {
                    val current = profileState.value
                    repository.saveProfile(current.copy(assetCacheSizeMb = 0))
                }
            }
            "fps" -> {
                val fpsVal = if (parts.size > 1) parts[1] else "60"
                newLogs.add(TerminalLine("Simulated hardware vertical refresh synchronization cap restricted to: ${fpsVal}fps", "success"))
            }
            "status" -> {
                val prof = profileState.value
                newLogs.add(TerminalLine("Cfx Core state: OK [60Hz loop cycle active]", "success"))
                newLogs.add(TerminalLine("SQLite Persistence: connected to [fivem_mobile_database]", "success"))
                newLogs.add(TerminalLine("Resource allocation: cache size = ${prof.assetCacheSizeMb} MB", "info"))
                newLogs.add(TerminalLine("Hardware audio device state: [${prof.voiceChannel}] driver synchronized active", "info"))
            }
            "disconnect" -> {
                disconnect()
                newLogs.add(TerminalLine("Connection line terminated.", "success"))
            }
            else -> {
                newLogs.add(TerminalLine("Error: Unrecognized command '$action'. Type 'help' for support.", "error"))
            }
        }

        terminalLogs.value = newLogs
    }

    // Settings adjustments
    fun updateUsername(userName: String) {
        viewModelScope.launch {
            val prof = profileState.value
            repository.saveProfile(prof.copy(username = userName))
        }
    }

    fun updateAvatar(avatar: String) {
        viewModelScope.launch {
            val prof = profileState.value
            repository.saveProfile(prof.copy(avatarName = avatar))
        }
    }

    fun updateVoiceChannel(chan: String) {
        viewModelScope.launch {
            val prof = profileState.value
            repository.saveProfile(prof.copy(voiceChannel = chan))
        }
    }

    fun toggleNuiAcceleration(enabled: Boolean) {
        viewModelScope.launch {
            val prof = profileState.value
            repository.saveProfile(prof.copy(nuiHardwareAcceleration = enabled))
        }
    }

    fun toggleOfflineSync(enabled: Boolean) {
        viewModelScope.launch {
            val prof = profileState.value
            repository.saveProfile(prof.copy(syncOffline = enabled))
        }
    }

    fun clearCachedFiles() {
        viewModelScope.launch {
            val prof = profileState.value
            repository.saveProfile(prof.copy(assetCacheSizeMb = 0))
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
