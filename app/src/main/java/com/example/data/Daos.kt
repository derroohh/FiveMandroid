package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY playersCount DESC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<ServerEntity>)

    @Query("UPDATE servers SET isFavorite = :isFav WHERE id = :serverId")
    suspend fun updateServerFavorite(serverId: String, isFav: Boolean)

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun getServerCount(): Int
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM connection_history ORDER BY timestamp DESC LIMIT 30")
    fun getConnectionHistory(): Flow<List<ConnectionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ConnectionHistoryEntity)

    @Query("DELETE FROM connection_history")
    suspend fun clearHistory()
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)
}
