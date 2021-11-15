package ru.netology.diplom.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import ru.netology.diplom.Api.ApiService
import ru.netology.diplom.Entity.WallPostEntity
import ru.netology.diplom.Entity.WallRemoteKeyEntity
import ru.netology.diplom.Entity.toWallPostEntity
import ru.netology.diplom.dao.WallPostDao
import ru.netology.diplom.dao.WallRemoteKeyDao
import ru.netology.diplom.db.AppDb
import ru.netology.diplom.dto.Post
import ru.netology.diplom.error.ApiError

const val DEFAULT_WALL_PAGE_SIZE = 10

@OptIn(ExperimentalPagingApi::class)
class WallRemoteMediator(
    private val apiService: ApiService,
    private val appDb: AppDb,
    private val wallPostDao: WallPostDao,
    private val wallRemoteKeyDao: WallRemoteKeyDao,
    private val authorId: Long,
) : RemoteMediator<Int, WallPostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, WallPostEntity>
    ): MediatorResult {
        try {
            val response = when (loadType) {
                LoadType.REFRESH -> {
                    apiService.getLatestWallPosts(authorId, state.config.initialLoadSize)
                }
                LoadType.PREPEND -> {
                    val maxKey = wallRemoteKeyDao.getMaxKey() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    apiService.getWallPostsAfter(maxKey, authorId, state.config.pageSize)
                }
                LoadType.APPEND -> {
                    val minKey = wallRemoteKeyDao.getMinKey() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    apiService.getWallPostsBefore(minKey, authorId, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) throw ApiError(response.code())
            val receivedBody = response.body() ?: throw ApiError(response.code())

            if (receivedBody.isEmpty()) return MediatorResult.Success(
                endOfPaginationReached = true
            )

            appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        wallRemoteKeyDao.removeAll()
                        insertMaxKey(receivedBody)
                        insertMinKey(receivedBody)
                        wallPostDao.clearPostTable()
                    }
                    LoadType.PREPEND -> insertMaxKey(receivedBody)
                    LoadType.APPEND -> insertMinKey(receivedBody)
                }
                wallPostDao.insertPosts(receivedBody.map { it.copy(likeCount = it.likeOwnerIds.size) }
                    .toWallPostEntity())
            }
            return MediatorResult.Success(endOfPaginationReached = receivedBody.isEmpty())
        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }

    }


    private suspend fun insertMaxKey(receivedBody: List<Post>) {
        wallRemoteKeyDao.insertKey(
            WallRemoteKeyEntity(
                type = WallRemoteKeyEntity.KeyType.AFTER,
                id = receivedBody.first().id
            )
        )
    }

    private suspend fun insertMinKey(receivedBody: List<Post>) {
        wallRemoteKeyDao.insertKey(
            WallRemoteKeyEntity(
                type = WallRemoteKeyEntity.KeyType.BEFORE,
                id = receivedBody.last().id,
            )
        )
    }
}
