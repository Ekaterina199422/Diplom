package ru.netology.diplom.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.diplom.auth.AppAuth
import ru.netology.diplom.error.AppError
import ru.netology.diplom.model.FeedModel
import ru.netology.diplom.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class EventExhibitorViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: AppAuth
) :
    ViewModel() {

    private val _dataState = MutableLiveData(FeedModel())
    val dataState: LiveData<FeedModel>
        get() = _dataState

    suspend fun getExhibitor(eventId: Long) = auth.authStateFlow.flatMapLatest { (myId, _) ->
        userRepository.getEventParticipants(eventId).map { userList ->
            userList.map { it.copy(isItMe = it.id == myId) }
        }
    }


    init {
        loadAllUsers()
    }


    private fun loadAllUsers() {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModel(isLoading = true)
                userRepository.loadUsers()
                _dataState.value = FeedModel()
            } catch (e: Exception) {
                _dataState.value = FeedModel(
                    hasError = true,
                    errorMessage = AppError.getMessage(e)
                )
            }
        }
    }

    fun invalidateDataState() {
        _dataState.value = FeedModel()
    }


}