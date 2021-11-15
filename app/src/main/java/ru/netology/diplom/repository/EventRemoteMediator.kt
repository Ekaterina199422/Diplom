package ru.netology.diplom.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import ru.netology.diplom.Api.ApiService
import ru.netology.diplom.Entity.EventEntity
import ru.netology.diplom.Entity.EventRemoteKeyEntity
import ru.netology.diplom.Entity.toEntity
import ru.netology.diplom.dao.EventDao
import ru.netology.diplom.dao.EventRemoteKeyDao
import ru.netology.diplom.db.AppDb
import ru.netology.diplom.dto.Event
import ru.netology.diplom.error.ApiError

const val DEFAULT_EVENT_PAGE_SIZE: Int = 10

@ExperimentalPagingApi
class EventRemoteMediator(
    private val appDb: AppDb,
    private val remoteKeyDao: EventRemoteKeyDao,
    private val apiService: ApiService,
    private val eventDao: EventDao
) : RemoteMediator<Int, EventEntity>() {


    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, EventEntity>
    ): MediatorResult {
        try {
            val response = when (loadType) {
                LoadType.REFRESH -> {
                    apiService.getLatestEvents(state.config.initialLoadSize)
                }
                LoadType.PREPEND -> {
                    val maxKey = remoteKeyDao.getMaxKey() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    apiService.getEventsAfter(maxKey, state.config.pageSize)
                }
                LoadType.APPEND -> {
                    val minKey = remoteKeyDao.getMinKey() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    apiService.getEventsBefore(minKey, state.config.pageSize)
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
                        remoteKeyDao.removeAll()
                        insertMinKey(receivedBody)
                        insertMaxKey(receivedBody)
                        eventDao.clearEventTable()
                    }
                    LoadType.PREPEND -> insertMaxKey(receivedBody)
                    LoadType.APPEND -> insertMinKey(receivedBody)
                }
                eventDao.insertEvents(receivedBody.map {
                    it.copy(
                        likeCount = it.likeOwnerIds.size,
                        exhibitorsCount = it.exhibitorsIds.size
                    )
                }
                    .toEntity())
            }
            return MediatorResult.Success(
                endOfPaginationReached =
                receivedBody.isEmpty()
            )
        } catch (e: Exception) {
            e.printStackTrace()

            return MediatorResult.Error(e)
        }
    }


    private suspend fun insertMaxKey(receivedBody: List<Event>) {
        remoteKeyDao.insertKey(
            EventRemoteKeyEntity(
                type = EventRemoteKeyEntity.KeyType.AFTER,
                id = receivedBody.first().id
            )
        )
    }

    private suspend fun insertMinKey(receivedBody: List<Event>) {
        remoteKeyDao.insertKey(
            EventRemoteKeyEntity(
                type = EventRemoteKeyEntity.KeyType.BEFORE,
                id = receivedBody.last().id,
            )
        )
    }
}