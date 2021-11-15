package ru.netology.diplom.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.diplom.Entity.EventEntity

@Dao
interface EventDao {

    @Query("SELECT * FROM EventEntity ORDER BY id DESC")
    fun getEventPagingSource(): PagingSource<Int, EventEntity>


    @Query("SELECT * FROM EventEntity ORDER BY id DESC")
    fun getAllEvents() : Flow<List<EventEntity>>

    @Query("SELECT * FROM EventEntity WHERE id = :id ")
    suspend fun getEventById(id: Long) : EventEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Query("DELETE FROM EventEntity WHERE id = :id")
    suspend fun deleteEvent(id: Long)

    @Query("DELETE FROM EventEntity")
    suspend fun clearEventTable()
}