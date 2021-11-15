package ru.netology.diplom.ViewModel

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
import ru.netology.diplom.dto.Job
import ru.netology.diplom.dto.Post
import ru.netology.diplom.dto.User
import ru.netology.diplom.error.AppError
import ru.netology.diplom.model.FeedModel
import ru.netology.diplom.repository.PageRepository
import javax.inject.Inject


@HiltViewModel
class PageViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val repository: PageRepository
) : ViewModel() {

    val myId: Long = appAuth.authStateFlow.value.id

    private val _currentUser = MutableLiveData(User())
    val currentUser: LiveData<User>
        get() = _currentUser

    private val _dataState = MutableLiveData(FeedModel())
    val dataState: LiveData<FeedModel>
        get() = _dataState

    fun invalidateDataState() {
        _dataState.value = FeedModel()
    }

    private val _profileUserId = MutableLiveData<Long?>()
    val profileUserId: LiveData<Long?>
        get() = _profileUserId


    @ExperimentalCoroutinesApi
    @ExperimentalPagingApi
    fun getWallPosts(): Flow<PagingData<Post>> = appAuth.authStateFlow
        .flatMapLatest { (myId, _) ->
            repository.getAllPosts(_profileUserId.value!!)
                .map { postList ->
                    postList.map {
                        it.copy(
                            ownedByMe = it.authorId == myId
                        )
                    }
                }
        }
        .cachedIn(viewModelScope)

    fun setAuthorId(authorId: Long) {
        _profileUserId.value = if (authorId == -1L) appAuth.authStateFlow.value.id
        else authorId
    }

    fun getUserById() {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModel(isLoading = true)
                val fetchedUser = repository.getUserById(_profileUserId.value!!)
                _currentUser.value = fetchedUser
                _dataState.value = FeedModel()
            } catch (e: Exception) {
                _dataState.value = FeedModel(
                    hasError = true,
                    errorMessage = AppError.getMessage(e)
                )
            }
        }
    }

    fun loadJobsFromServer() {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModel(isLoading = true)
                repository.loadJobsFromServer(_profileUserId.value!!)
                _dataState.value = FeedModel()
            } catch (e: Exception) {
                _dataState.value = FeedModel(
                    hasError = true,
                    errorMessage = AppError.getMessage(e)
                )
            }
        }
    }



    fun getLatestWallPosts(){
        viewModelScope.launch {
            try {
                _dataState.value = FeedModel(isLoading = true)
                repository.getLatestWallPosts(_profileUserId.value!!)
                _dataState.value = FeedModel()
            } catch (e: Exception) {
                _dataState.value = FeedModel(
                    hasError = true,
                    errorMessage = AppError.getMessage(e)
                )
            }
        }
    }

    fun refreshLatestWallPosts(){
        viewModelScope.launch {
            try {
                _dataState.value = FeedModel(isRefreshing = true)
                repository.getLatestWallPosts(_profileUserId.value!!)
                _dataState.value = FeedModel()
            } catch (e: Exception) {
                _dataState.value = FeedModel(
                    hasError = true,
                    errorMessage = AppError.getMessage(e)
                )
            }
        }
    }


    fun likeWallPostById(post: Post) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModel()
                repository.likePost(post)
            } catch (e: Exception) {
                _dataState.value = (FeedModel(
                    hasError = true,
                    errorMessage = AppError.getMessage(e)
                ))
            }
        }
    }

    fun getAllJobs(): LiveData<List<Job>> = repository.getAllJobs()

    fun createNewJob(job: Job) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModel(isLoading = true)
                repository.createJob(job)
                _dataState.value = FeedModel(isLoading = false)
            } catch (e: Exception) {
                _dataState.value = FeedModel(
                    hasError = true,
                    errorMessage = AppError.getMessage(e)
                )
            }
        }
    }

    fun deleteJobById(id: Long) {
        viewModelScope.launch {
            try {
                _dataState.value = FeedModel(isLoading = true)
                repository.deleteJobById(id)
                _dataState.value = FeedModel(isLoading = false)
            } catch (e: Exception) {
                _dataState.value = FeedModel(
                    hasError = true,
                    errorMessage = AppError.getMessage(e)
                )
            }
        }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            try {
                _dataState.value = (FeedModel(isLoading = true))
                repository.deletePost(postId)
                _dataState.value = (FeedModel(isLoading = false))
            } catch (e: Exception) {
                _dataState.value = (FeedModel(
                    hasError = true,
                    errorMessage = AppError.getMessage(e)
                ))
            }
        }
    }

    fun onSignOut() {
        appAuth.removeAuth()
    }
}