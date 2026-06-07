package com.example.guet_map.di

import com.example.guet_map.BuildConfig
import com.example.guet_map.local.AppDatabase
import com.example.guet_map.local.dao.AccountDao
import com.example.guet_map.local.dao.ContributeDraftDao
import com.example.guet_map.local.dao.FavoriteDao
import com.example.guet_map.local.dao.GuideStepDao
import com.example.guet_map.local.dao.LocationDao
import com.example.guet_map.local.dao.NotificationDao
import com.example.guet_map.network.ApiService
import com.example.guet_map.network.AuthInterceptor
import com.example.guet_map.network.MockInterceptor
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        if (BuildConfig.USE_MOCK_API) {
            builder.addInterceptor(MockInterceptor())
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(application: android.app.Application): AppDatabase {
        return AppDatabase.getInstance(application)
    }

    @Provides
    fun provideGuideStepDao(database: AppDatabase): GuideStepDao = database.guideStepDao()

    @Provides
    fun provideLocationDao(database: AppDatabase): LocationDao = database.locationDao()

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideNotificationDao(database: AppDatabase): NotificationDao = database.notificationDao()

    @Provides
    fun provideContributeDraftDao(database: AppDatabase): ContributeDraftDao =
        database.contributeDraftDao()

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()
}
