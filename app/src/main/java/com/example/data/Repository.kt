package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FiveMRepository(private val db: AppDatabase) {
    private val serverDao = db.serverDao()
    private val historyDao = db.historyDao()
    private val profileDao = db.profileDao()

    val allServersFlow: Flow<List<ServerEntity>> = serverDao.getAllServers()
    val historyFlow: Flow<List<ConnectionHistoryEntity>> = historyDao.getConnectionHistory()
    val profileFlow: Flow<UserProfileEntity?> = profileDao.getProfileFlow()

    suspend fun getProfileDirect(): UserProfileEntity {
        return withContext(Dispatchers.IO) {
            profileDao.getProfile() ?: UserProfileEntity().also {
                profileDao.insertOrUpdateProfile(it)
            }
        }
    }

    suspend fun saveProfile(profile: UserProfileEntity) {
        withContext(Dispatchers.IO) {
            profileDao.insertOrUpdateProfile(profile)
        }
    }

    suspend fun toggleFavorite(serverId: String, isFav: Boolean) {
        withContext(Dispatchers.IO) {
            serverDao.updateServerFavorite(serverId, isFav)
        }
    }

    suspend fun addConnectionHistory(ipAddress: String, serverName: String) {
        withContext(Dispatchers.IO) {
            historyDao.insertHistory(
                ConnectionHistoryEntity(
                    ipAddress = ipAddress,
                    serverName = serverName
                )
            )
        }
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            historyDao.clearHistory()
        }
    }

    suspend fun prepopulateDatabaseIfNeeded() {
        withContext(Dispatchers.IO) {
            if (serverDao.getServerCount() == 0) {
                val seedServers = listOf(
                    ServerEntity(
                        id = "nopixel",
                        name = "NoPixel 4.0 - Official Android Client",
                        description = "The premier GTA V Roleplay experience simulated on Android! High capacity local audio streams, optimized visual state rendering, customized inventory, and job mechanics.",
                        playersCount = 985,
                        maxPlayers = 1000,
                        ping = 15,
                        tags = "Roleplay, WL, Serious, Economy, Factions",
                        category = "roleplay",
                        gameBuild = "b3095",
                        uptimeByPercent = 100,
                        bannerGradientIndex = 0
                    ),
                    ServerEntity(
                        id = "eclipse",
                        name = "Eclipse RP Mobile - Public Access",
                        description = "A massive voice-driven roleplay environment. Active police forces, hospital systems, gang turf wars, customized housing, and dealership markets.",
                        playersCount = 422,
                        maxPlayers = 800,
                        ping = 32,
                        tags = "Roleplay, Non-WL, Voice, Jobs, Gangs, Cars",
                        category = "roleplay",
                        gameBuild = "b2699",
                        uptimeByPercent = 99,
                        bannerGradientIndex = 1
                    ),
                    ServerEntity(
                        id = "hyperdrift",
                        name = "Mt. Haruna Custom drift Sandbox",
                        description = "Features custom suspension handlers, drift scoring multipliers, JDM imports, and active leaderboards. Spawn any racer and blast through Japanese highways.",
                        playersCount = 188,
                        maxPlayers = 300,
                        ping = 45,
                        tags = "Drifting, JDM, Racing, Sandbox, Custom Physics",
                        category = "drift",
                        gameBuild = "b3152",
                        uptimeByPercent = 98,
                        bannerGradientIndex = 2
                    ),
                    ServerEntity(
                        id = "sentinels_zombie",
                        name = "Rotten State: Apocalypse Outbreak",
                        description = "Survival horror sandbox. Loot, craft tools, build defensive structures, fight roaming undead waves, and trade with survivor outposts.",
                        playersCount = 95,
                        maxPlayers = 150,
                        ping = 18,
                        tags = "Survival, Zombie, Looting, Crafting, Hardcore",
                        category = "freeroam",
                        gameBuild = "b2545",
                        uptimeByPercent = 99,
                        bannerGradientIndex = 3
                    ),
                    ServerEntity(
                        id = "grand_riviera",
                        name = "Los Santos Riviera - Modding Playground",
                        description = "Uncapped drift multipliers, customized administrative helper panel command, spawn helicopter, weather sandbox, toggle traffic, direct connect testing.",
                        playersCount = 312,
                        maxPlayers = 600,
                        ping = 9,
                        tags = "Freeroam, Sandbox, Spawners, GodMode, Fun, PvP",
                        category = "freeroam",
                        gameBuild = "b3095",
                        uptimeByPercent = 99,
                        bannerGradientIndex = 4
                    ),
                    ServerEntity(
                        id = "apex_racing",
                        name = "Apex GT3 Championship Circuit",
                        description = "High-octane road-course racing simulator. Accurate telemetry graphs, dynamic tire wear physics, compound selection, pit crew configurations.",
                        playersCount = 54,
                        maxPlayers = 100,
                        ping = 28,
                        tags = "Racing, GT3, Laps, Telemetry, Physics, Active",
                        category = "racing",
                        gameBuild = "b2802",
                        uptimeByPercent = 100,
                        bannerGradientIndex = 5
                    )
                )
                serverDao.insertServers(seedServers)
            }

            // Also seed profile if missing
            if (profileDao.getProfile() == null) {
                profileDao.insertOrUpdateProfile(UserProfileEntity())
            }
        }
    }
}
