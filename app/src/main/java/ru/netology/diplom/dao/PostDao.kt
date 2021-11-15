package ru.netology.diplom.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.diplom.Entity.PostEntity

@Dao
interface PostDao {


    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getPostPagingSource(): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM PostEntity WHERE authorId = :authorId ORDER BY id DESC")
    fun getWallPagingSource(authorId: Long): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM PostEntity WHERE id = :id ")
    suspend fun getPostById(id: Long) : PostEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Query("DELETE FROM PostEntity WHERE id = :id")
    suspend fun deletePost(id: Long)

    @Query("DELETE FROM PostEntity")
    suspend fun clearPostTable()
}