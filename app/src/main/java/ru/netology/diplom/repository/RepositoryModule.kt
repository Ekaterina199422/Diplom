package ru.netology.diplom.repository

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.diplom.Api.ApiService
import ru.netology.diplom.dao.*
import ru.netology.diplom.db.AppDb
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RepositoryModule {

    @Provides
    @Singleton
    fun providePostRepository(
        apiService: ApiService,
        postDao: PostDao,
        appDb: AppDb,
        postRemoteKeyDao: PostRemoteKeyDao
    ): PostRepository =
        PostRepository(postDao, apiService, appDb, postRemoteKeyDao)

    @Provides
    @Singleton
    fun providesSignInUpRepository(apiService: ApiService): SignInUpRepository =
        SignInUpRepository(apiService)

    @Provides
    @Singleton
    fun provideEventRepository(
        apiService: ApiService,
        eventDao: EventDao,
        appDb: AppDb,
        eventRemoteKeyDao: EventRemoteKeyDao
    ): EventRepository = EventRepository(appDb, apiService, eventDao, eventRemoteKeyDao)

    @Provides
    @Singleton
    fun provideProfileRepository(
        apiService: ApiService,
        appDb: AppDb,
        wallRemoteKeyDao: WallRemoteKeyDao,
        wallPostDao: WallPostDao,
        jobDao: JobDao,
        postDao: PostDao,
    ): PageRepository =
        PageRepository(apiService, appDb, wallPostDao, wallRemoteKeyDao, jobDao, postDao)


    @Provides
    @Singleton
    fun provideUserRepository(
        apiService: ApiService,
        userDao: UserDao,
    ): UserRepository =
        UserRepository(apiService, userDao)
}