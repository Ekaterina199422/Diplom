package ru.netology.diplom.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.diplom.Entity.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM UserEntity ORDER BY name")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("DELETE FROM UserEntity")
    suspend fun removeAllUsers()

    @Query("SELECT * FROM UserEntity WHERE id IN (:participants)")
    fun getEventParticipants(participants: Set<Long>): Flow<List<UserEntity>>
}