package ru.netology.diplom.repository

import androidx.paging.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.diplom.Api.ApiService
import ru.netology.diplom.Entity.PostEntity
import ru.netology.diplom.dao.PostDao
import ru.netology.diplom.dao.PostRemoteKeyDao
import ru.netology.diplom.db.AppDb
import ru.netology.diplom.dto.*
import ru.netology.diplom.error.ApiError
import ru.netology.diplom.error.DbError
import ru.netology.diplom.error.NetworkError
import ru.netology.diplom.error.UndefinedError
import java.io.IOException
import java.sql.SQLException
import javax.inject.Inject

class PostRepository @Inject constructor(
    private val postDao: PostDao,
    private val postApi: ApiService,
    private val db: AppDb,
    private val postRemoteKeyDao: PostRemoteKeyDao,
) {

    @ExperimentalPagingApi
    fun getAllPosts(): Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = DEFAULT_POST_PAGE_SIZE, enablePlaceholders =false),
        remoteMediator = PostRemoteMediator(postApi, db, postDao, postRemoteKeyDao),
        pagingSourceFactory = { postDao.getPostPagingSource() }
    ).flow.map { postList ->
        postList.map { it.toDto() }
    }


    suspend fun createPost(post: Post) {
        try {
            val createPostResponse = postApi.createPost(post)
            if (!createPostResponse.isSuccessful) {
                throw ApiError(createPostResponse.code())
            }
            val createPostBody = createPostResponse.body() ?: throw ApiError(
                createPostResponse.code()
            )

            val getPostResponse = postApi.getPostById(createPostBody.id)
            if (!getPostResponse.isSuccessful) {
                throw ApiError(getPostResponse.code())
            }
            val getPostBody = getPostResponse.body() ?: throw ApiError(
                getPostResponse.code()
            )

            postDao.insertPost(PostEntity.fromDto(getPostBody))
        } catch (e: IOException) {
            e.printStackTrace()

            throw NetworkError
        } catch (e: SQLException) {
            e.printStackTrace()

            throw  DbError
        } catch (e: Exception) {
            e.printStackTrace()

            throw UndefinedError
        }
    }

    suspend fun saveWithAttachment(post: Post, mediaUpload: MediaUpload, type : AttachmentType) {
        try {
            val uploadedMedia = uploadMedia(mediaUpload)

            val postWithAttachment = post.copy(
                attachment = MediaAttachment(
                    url = uploadedMedia.url,
                    type = type
                )
            )

            createPost(postWithAttachment)
        } catch (e: IOException) {
            e.printStackTrace()
            throw NetworkError
        } catch (e: SQLException) {
            e.printStackTrace()
            throw  DbError
        } catch (e: Exception) {
            e.printStackTrace()
            throw UndefinedError
        }
    }

    private suspend fun uploadMedia(mediaUpload: MediaUpload): MediaDownload {
        try {
            val mediaMultipart = MultipartBody.Part.createFormData(
                "file", mediaUpload.file.name,
                mediaUpload.file.asRequestBody()
            )
            val uploadMediaResponse = postApi.saveMediaFile(mediaMultipart)
            if (!uploadMediaResponse.isSuccessful) throw ApiError(uploadMediaResponse.code())
            return uploadMediaResponse.body() ?: throw ApiError(uploadMediaResponse.code())
        } catch (e: IOException) {
            e.printStackTrace()
            throw NetworkError
        } catch (e: Exception) {
            e.printStackTrace()
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
            postDao.insertPost(PostEntity.fromDto(likedPost))


            val response = if (post.likedByMe) postApi.dislikePostById(post.id)
            else postApi.likePostById(post.id)

            if (!response.isSuccessful)
                throw ApiError(response.code())
        } catch (e: IOException) {

            postDao.insertPost(PostEntity.fromDto(post))
            throw NetworkError
        } catch (e: SQLException) {

            postDao.insertPost(PostEntity.fromDto(post))
            throw  DbError
        } catch (e: Exception) {
            postDao.insertPost(PostEntity.fromDto(post))
            throw UndefinedError
        }
    }


    suspend fun deletePost(postId: Long) {
        val postToDelete = postDao.getPostById(postId)
        try {
            postDao.deletePost(postId)

            val response = postApi.deletePost(postId)
            if (!response.isSuccessful) {
                postDao.insertPost(postToDelete)
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

