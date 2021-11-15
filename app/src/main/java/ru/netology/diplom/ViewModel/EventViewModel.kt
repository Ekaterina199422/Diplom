package ru.netology.diplom.ViewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.diplom.auth.AppAuth
import ru.netology.diplom.dto.Event
import ru.netology.diplom.dto.MediaUpload
import ru.netology.diplom.error.AppError
import ru.netology.diplom.model.FeedModel
import ru.netology.diplom.model.MediaModel
import ru.netology.diplom.repository.EventRepository
import java.io.File
import javax.inject.Inject


@ExperimentalPagingApi
@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: EventRepository,
    appAuth: AppAuth
) : ViewModel() {

    private val _dataState = MutableLiveData(FeedModel())
    val dataState: LiveData<FeedModel>
        get() = _dataState

    fun invalidateDataState() {
        _dataState.value = FeedModel()
    }

    private val _editedEvent = MutableLiveData<Event?>(null)
    val editedEvent: LiveData<Event?>
        get() = _editedEvent

    private val noPhoto = MediaModel()

    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<MediaModel>
        get() = _photo


    private val _eventDateTime = MutableLiveData<String?>()
    val eventDateTime: LiveData<String?>
        get() = _eventDateTime

    private val cached = repository.getAllEvents().cachedIn(viewModelScope)

    @ExperimentalCoroutinesApi
    val eventList: Flow<PagingData<Event>> = appAuth
        .authStateFlow
        .flatMapLatest { (myId, _) ->
            cached.map { pagingDataList ->
                pagingDataList.map { it.copy(ownedByMe = myId == it.authorId) }
            }
        }


    fun editEvent(editedEvent: Event) {
        _editedEvent.value = editedEvent
    }

    fun invalidateEditedEvent() {
        _editedEvent.value = null
    }

    fun invalidateEventDateTime() {
        _eventDateTime.value = null
    }

    fun setEventDateTime(dateTime: String) {
        _eventDateTime.value = dateTime
    }

    fun saveEvent(event: Event) {
        viewModelScope.launch {
            try {
                _dataState.value = (FeedModel(isLoading = true))
                when (_photo.value) {
                    noPhoto -> repository.createEvent(event)
                    else -> _photo.value?.file?.let { file ->
                        repository.saveWithAttachment(event, MediaUpload(file))
                    }
                }
                _dataState.value = (FeedModel(isLoading = false))
            } catch (e: Exception) {
                _dataState.value = (FeedModel(
                    hasError = true,
                    errorMessage = AppError.getMessage(e)
                ))
            } finally {
                invalidateEditedEvent()
                _photo.value = noPhoto
                invalidateEventDateTime()
            }
        }
    }

    fun likeEvent(event: Event) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModel()
                repository.likeEvent(event)
            } catch (e: Exception) {
                _dataState.value = (FeedModel(
                    hasError = true,
                    errorMessage = AppError.getMessage(e)
                ))
            }
        }
    }

    fun exhibitorInEvent(event: Event) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModel()
                repository.participateInEvent(event)
            } catch (e: Exception) {
                _dataState.value = (FeedModel(
                    hasError = true,
                    errorMessage = AppError.getMessage(e)
                ))
            }
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                _dataState.value = (FeedModel(isLoading = true))
                repository.deleteEvent(eventId)
                _dataState.value = (FeedModel(isLoading = false))
            } catch (e: Exception) {
                _dataState.value = (FeedModel(
                    hasError = true,
                    errorMessage = AppError.getMessage(e)
                ))
            }
        }
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = MediaModel(uri, file)
    }


}