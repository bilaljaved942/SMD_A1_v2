package com.example.firstapp.data.remote

import com.example.firstapp.data.remote.api.*
import com.google.gson.GsonBuilder
import com.example.firstapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    // Base URL is injected via BuildConfig.BASE_URL (set in build.gradle from local.properties)
    private val BASE_URL: String = BuildConfig.BASE_URL
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = GsonBuilder()
        .setLenient()
        .create()
    
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    val authApi: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
    
    val messageApi: MessageApiService by lazy {
        retrofit.create(MessageApiService::class.java)
    }
    
    val postApi: PostApiService by lazy {
        retrofit.create(PostApiService::class.java)
    }
    
    val storyApi: StoryApiService by lazy {
        retrofit.create(StoryApiService::class.java)
    }
    
    val commentApi: CommentApiService by lazy {
        retrofit.create(CommentApiService::class.java)
    }
    
    val followApi: FollowApiService by lazy {
        retrofit.create(FollowApiService::class.java)
    }
    
    val userApi: UserApiService by lazy {
        retrofit.create(UserApiService::class.java)
    }
}
