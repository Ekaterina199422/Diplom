package ru.netology.diplom.auth

import android.content.SharedPreferences
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ru.netology.diplom.Api.ApiService
import ru.netology.diplom.dto.PushToken


class AppAuth (
    private val prefs: SharedPreferences,
    private val apiService: ApiService
) {

    private val idKey = "id"
    private val tokenKey = "token"

    private val _authStateFlow = MutableStateFlow(AuthState())
    val authStateFlow: StateFlow<AuthState> = _authStateFlow.asStateFlow()


    init {
        val id = prefs.getLong(idKey, 0L)
        val token = prefs.getString(tokenKey, null)
        if (id == 0L || token == null) {
            _authStateFlow.value = AuthState()
            prefs.edit()
                .clear()
                .apply()
        } else {
            _authStateFlow.value = AuthState(id, token)
        }
        sendPushTokenToServer()
    }

    @Synchronized
    fun setAuth(id: Long, token: String) {
        _authStateFlow.value = AuthState(id, token)
        with(prefs.edit()) {
            putLong(idKey, id)
            putString(tokenKey, token)
            apply()
        }
        sendPushTokenToServer()
    }

    @Synchronized
    fun removeAuth() {
        _authStateFlow.value = AuthState()
        with(prefs.edit()) {
            clear()
            commit()
        }
        sendPushTokenToServer()
    }

    fun sendPushTokenToServer(token: String? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val pushToken = PushToken(token ?: Firebase.messaging.token.await())
                apiService.saveToken(pushToken)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }


}

data class AuthState(val id: Long = 0, val token: String? = null)
