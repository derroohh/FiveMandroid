package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val playersCount: Int,
    val maxPlayers: Int,
    val ping: Int,
    val tags: String,
    val category: String, // "roleplay", "freeroam", "drift", "racing"
    val isFavorite: Boolean = false,
    val gameBuild: String = "b3095",
    val uptimeByPercent: Int = 99,
    val bannerGradientIndex: Int = 0 // Used to generate distinct, ultra-polished visual card backgrounds
)

@Entity(tableName = "connection_history")
data class ConnectionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ipAddress: String,
    val serverName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val username: String = "Cfx_Player",
    val avatarName: String = "Franklin", // Franklin, Michael, Trevor, Lamar, Lester, Packie
    val syncOffline: Boolean = true,
    val voiceChannel: String = "pma-voice",
    val nuiHardwareAcceleration: Boolean = true,
    val assetCacheSizeMb: Int = 245
)
