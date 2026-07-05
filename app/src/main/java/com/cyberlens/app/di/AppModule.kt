package com.cyberlens.app.di

import android.content.Context
import androidx.room.Room
import com.cyberlens.app.BuildConfig
import com.cyberlens.app.data.local.AppDatabase
import com.cyberlens.app.data.remote.*
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    @Provides
    @Singleton
    @Named("ipapi")
    fun provideIpApiRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.IPAPI_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @Named("shodan")
    fun provideShodanRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.SHODAN_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @Named("hackertarget")
    fun provideHackerTargetRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.HACKERTARGET_BASE_URL)
            .client(client)
            .addConverterFactory(retrofit2.converter.scalars.ScalarsConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @Named("virustotal")
    fun provideVirusTotalRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.VIRUSTOTAL_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @Named("cvedb")
    fun provideCveDbRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.CVEDB_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideIpApiService(@Named("ipapi") retrofit: Retrofit): IpApiService =
        retrofit.create(IpApiService::class.java)

    @Provides
    @Singleton
    fun provideIpInfoApiService(@Named("ipapi") retrofit: Retrofit): IpInfoApiService =
        retrofit.create(IpInfoApiService::class.java)

    @Provides
    @Singleton
    fun provideShodanService(@Named("shodan") retrofit: Retrofit): ShodanInternetDbService =
        retrofit.create(ShodanInternetDbService::class.java)

    @Provides
    @Singleton
    fun provideHackerTargetService(@Named("hackertarget") retrofit: Retrofit): HackerTargetApiService =
        retrofit.create(HackerTargetApiService::class.java)

    @Provides
    @Singleton
    fun provideVirusTotalService(@Named("virustotal") retrofit: Retrofit): VirusTotalApiService =
        retrofit.create(VirusTotalApiService::class.java)

    @Provides
    @Singleton
    fun provideCveService(@Named("cvedb") retrofit: Retrofit): CveSearchApiService =
        retrofit.create(CveSearchApiService::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Named("vt_api_key")
    fun provideVtApiKey(): String = "" // Set your VirusTotal API key here

}
