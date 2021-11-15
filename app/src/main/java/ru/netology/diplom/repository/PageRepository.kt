package ru.netology.diplom.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.netology.diplom.Api.ApiService
import ru.netology.diplom.Entity.JobEntity
import ru.netology.diplom.Entity.WallPostEntity
import ru.netology.diplom.Entity.fromDto
import ru.netology.diplom.Entity.toWallPostEntity
import ru.netology.diplom.dao.JobDao
import ru.netology.diplom.dao.PostDao
import ru.netology.diplom.dao.WallPostDao
import ru.netology.diplom.dao.WallRemoteKeyDao
import ru.netology.diplom.db.AppDb
import ru.netology.diplom.dto.Job
import ru.netology.diplom.dto.Post
import ru.netology.diplom.dto.User
import ru.netology.diplom.error.ApiError
import ru.netology.diplom.error.DbError
import ru.netology.diplom.error.NetworkError
import ru.netology.diplom.error.UndefinedError
import java.io.IOException
import java.sql.SQLException
import javax.inject.Inject

class PageRepository @Inject constructor(
    private val apiService: ApiService,
    private val appDb: AppDb,
    private val wallPostDao: WallPostDao,
    private val wallRemoteKeyDao: WallRemoteKeyDao,
    private val jobDao: JobDao,
    private val postDao: PostDao,
) {

    @ExperimentalPagingApi
    fun getAllPosts(authorId: Long): Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = DEFAULT_WALL_PAGE_SIZE, enablePlaceholders = false),
        remoteMediator = WallRemoteMediator(
            apiService,
            appDb,
            wallPostDao,
            wallRemoteKeyDao,
            authorId
        ),
        pagingSourceFactory = { wallPostDao.getWallPagingSource() }
    ).flow.map { postList ->
        postList.map { it.toDto() }
    }

    fun getAllJobs(): LiveData<List<Job>> = jobDao.getAllJobs().map { jobList ->
        jobList.map {
            it.toDto()
        }
    }


    suspend fun getUserById(userId: Long): User {
        try {
            val response = apiService.getUserById(userId)
            if (!response.isSuccessful) throw ApiError(response.code())
            return response.body() ?: throw ApiError(response.code())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UndefinedError
        }
    }

    suspend fun getLatestWallPosts(authorId: Long) {
        try {
            wallPostDao.clearPostTable()
            val response = apiService.getLatestWallPosts(authorId, 10)

            if (!response.isSuccessful) throw ApiError(response.code())

            val body = response.body() ?: throw ApiError(response.code())

            wallPostDao.insertPosts(body.toWallPostEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: SQLException) {
            throw  DbError
        } catch (e: Exception) {
            throw UndefinedError
        }
    }


    suspend fun likePost(post: Post) {
        try {

            val likedPost = post.copy(
                likeCount = if (post.likedByMe) post.likeCount.dec()
                else post.likeCount.inc(),
                likedByMe = !post.likedByMe
            )
            wallPostDao.insertPost(WallPostEntity.fromDto(likedPost))


            val response = if (post.likedByMe) apiService.dislikeWallPostById(post.id)
            else apiService.likeWallPostById(post.id)

            if (!response.isSuccessful)
                throw ApiError(response.code())
        } catch (e: IOException) {

            wallPostDao.insertPost(WallPostEntity.fromDto(post))
            throw NetworkError
        } catch (e: SQLException) {
            wallPostDao.insertPost(WallPostEntity.fromDto(post))
            throw  DbError
        } catch (e: Exception) {
            wallPostDao.insertPost(WallPostEntity.fromDto(post))
            throw UndefinedError
        }
    }

    suspend fun loadJobsFromServer(authorId: Long) {
        try {
            jobDao.removeAllJobs()
            val response = apiService.getAllUserJobs(authorId)

            if (!response.isSuccessful) throw ApiError(response.code())

            val body = response.body() ?: throw ApiError(response.code())

            jobDao.insertJobs(body.fromDto())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: SQLException) {
            throw  DbError
        } catch (e: Exception) {
            throw UndefinedError
        }
    }

    suspend fun createJob(job: Job) {
        try {
            val response = apiService.saveJob(job)

            if (!response.isSuccessful) throw ApiError(response.code())

            val body = response.body() ?: throw ApiError(response.code())

            jobDao.insertJob(JobEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: SQLException) {
            throw  DbError
        } catch (e: Exception) {
            throw UndefinedError
        }
    }

    suspend fun deleteJobById(id: Long) {
        val jobToDelete = jobDao.getJobById(id)
        try {
            jobDao.removeJobById(id)

            val response = apiService.removeJobById(id)
            if (!response.isSuccessful) {
                jobDao.insertJob(jobToDelete)
                throw ApiError(response.code())
            }
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: SQLException) {
            throw  DbError
        } catch (e: Exception) {
            throw UndefinedError
        }
    }

    suspend fun deletePost(postId: Long) {
        val postToDelete = postDao.getPostById(postId)
        try {
            postDao.deletePost(postId)
            wallPostDao.deletePost(postId)
            val response = apiService.deletePost(postId)
            if (!response.isSuccessful) {
                postDao.insertPost(postToDelete)
                wallPostDao.insertPost(WallPostEntity.fromDto(postToDelete.toDto()))
                throw ApiError(response.code())
            }
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: SQLException) {
            throw  DbError
        } catch (e: Exception) {
            throw UndefinedError
        }
    }
}
