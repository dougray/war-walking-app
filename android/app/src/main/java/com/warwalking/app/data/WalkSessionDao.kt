package com.warwalking.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkSessionDao {
    @Insert
    suspend fun insert(session: WalkSessionEntity): Long

    @Update
    suspend fun update(session: WalkSessionEntity)

    @Query("SELECT * FROM walk_sessions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WalkSessionEntity>>

    @Query("SELECT * FROM walk_sessions ORDER BY createdAt DESC")
    suspend fun getAll(): List<WalkSessionEntity>

    @Query("SELECT * FROM walk_sessions WHERE id = :id")
    suspend fun getById(id: Long): WalkSessionEntity?
}
