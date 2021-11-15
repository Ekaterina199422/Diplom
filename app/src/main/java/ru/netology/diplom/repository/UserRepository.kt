package ru.netology.diplom.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.netology.diplom.Api.ApiService
import ru.netology.diplom.Entity.toEntity
import ru.netology.diplom.dao.UserDao
import ru.netology.diplom.dto.Event
import ru.netology.diplom.dto.User
import ru.netology.diplom.error.ApiError
import ru.netology.diplom.error.DbError
import ru.netology.diplom.error.NetworkError
import ru.netology.diplom.error.UndefinedError
import java.io.IOException
import java.sql.SQLException
import javax.inject.Inject


class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao,
) {

    fun getUsers() = userDao.getAllUsers().map { userList ->
        userList.map { it.toDto() }
    }

    suspend fun loadUsers() {
        try {
            userDao.removeAllUsers()
            val response = apiService.getAllUsers()

            if (!response.isSuccessful) throw ApiError(response.code())

            val body = response.body() ?: throw ApiError(response.code())

            userDao.insertUsers(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: SQLException) {
            throw  DbError
        } catch (e: Exception) {
            throw UndefinedError
        }
    }

    suspend fun getParticipatedEvent(id: Long) : Event {
        try {
            val response = apiService.getEventById(id)

            if (!response.isSuccessful) throw ApiError(response.code())

            return response.body() ?: throw ApiError(response.code())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: SQLException) {
            throw  DbError
        } catch (e: Exception) {
            throw UndefinedError
        }
    }

    suspend fun getEventParticipants(eventId: Long): Flow<List<User>> {
        val event = getParticipatedEvent(eventId)
        return userDao.getEventParticipants(event.exhibitorsIds).map { participantsList ->
            participantsList.map { it.toDto() }
        }
    }
}