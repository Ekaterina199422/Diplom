package ru.netology.diplom.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import ru.netology.diplom.auth.AppAuth
import ru.netology.diplom.auth.AuthState
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val appAuth: AppAuth) : ViewModel() {
    val authState: LiveData<AuthState> = appAuth
        .authStateFlow
        .asLiveData(Dispatchers.Default)

    val isAuthenticated: Boolean
        get() = appAuth.authStateFlow.value.id != 0L

    private var _checkIfAskedLogin = false
    val checkIfAskedToLogin: Boolean
        get() = _checkIfAskedLogin
    fun setCheckLoginTrue() {
        _checkIfAskedLogin = true
    }

}