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
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideGson(): Gson = Gson()

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                    redactHeader("Authorization")
                })
            }
        }
        .build()

    // ── Retrofit instances ────────────────────────────────────────────

    @Provides @Singleton @Named("ipgeo")
    fun provideIpGeoRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder().baseUrl(BuildConfig.IPGEO_BASE_URL).client(client)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()

    @Provides @Singleton @Named("shodan_inetdb")
    fun provideShodanInternetDbRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder().baseUrl(BuildConfig.SHODAN_INETDB_BASE_URL).client(client)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()

    @Provides @Singleton @Named("shodan_api")
    fun provideShodanApiRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder().baseUrl(BuildConfig.SHODAN_API_BASE_URL).client(client)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()

    @Provides @Singleton @Named("hackertarget")
    fun provideHackerTargetRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder().baseUrl(BuildConfig.HACKERTARGET_BASE_URL).client(client)
            .addConverterFactory(ScalarsConverterFactory.create()).build()

    @Provides @Singleton @Named("virustotal")
    fun provideVirusTotalRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder().baseUrl(BuildConfig.VIRUSTOTAL_BASE_URL).client(client)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()

    @Provides @Singleton @Named("cvedb")
    fun provideCveDbRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder().baseUrl(BuildConfig.CVEDB_BASE_URL).client(client)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()

    @Provides @Singleton @Named("whoisxml")
    fun provideWhoisXmlRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder().baseUrl(BuildConfig.WHOISXML_BASE_URL).client(client)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()

    @Provides @Singleton @Named("saucenao")
    fun provideSauceNaoRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder().baseUrl(BuildConfig.SAUCENAO_BASE_URL).client(client)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()

    // ── API Services ──────────────────────────────────────────────────

    @Provides @Singleton
    fun provideIpGeoService(@Named("ipgeo") r: Retrofit): IpGeoApiService =
        r.create(IpGeoApiService::class.java)

    @Provides @Singleton
    fun provideShodanInternetDbService(@Named("shodan_inetdb") r: Retrofit): ShodanInternetDbService =
        r.create(ShodanInternetDbService::class.java)

    @Provides @Singleton
    fun provideShodanApiService(@Named("shodan_api") r: Retrofit): ShodanApiService =
        r.create(ShodanApiService::class.java)

    @Provides @Singleton
    fun provideHackerTargetService(@Named("hackertarget") r: Retrofit): HackerTargetApiService =
        r.create(HackerTargetApiService::class.java)

    @Provides @Singleton
    fun provideVirusTotalService(@Named("virustotal") r: Retrofit): VirusTotalApiService =
        r.create(VirusTotalApiService::class.java)

    @Provides @Singleton
    fun provideCveService(@Named("cvedb") r: Retrofit): CveSearchApiService =
        r.create(CveSearchApiService::class.java)

    @Provides @Singleton
    fun provideWhoisXmlService(@Named("whoisxml") r: Retrofit): WhoisXmlApiService =
        r.create(WhoisXmlApiService::class.java)

    @Provides @Singleton
    fun provideSauceNaoService(@Named("saucenao") r: Retrofit): SauceNaoApiService =
        r.create(SauceNaoApiService::class.java)

    // ── Database ──────────────────────────────────────────────────────

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    // ── Named API keys ────────────────────────────────────────────────

    @Provides @Named("vt_api_key")
    fun provideVtApiKey(): String = BuildConfig.VT_API_KEY

    @Provides @Named("saucenao_api_key")
    fun provideSauceNaoApiKey(): String = BuildConfig.SAUCENAO_API_KEY

    @Provides @Named("ipgeo_api_key")
    fun provideIpGeoApiKey(): String = BuildConfig.IPGEO_API_KEY

    @Provides @Named("shodan_api_key")
    fun provideShodanApiKey(): String = BuildConfig.SHODAN_API_KEY

    @Provides @Named("whoisxml_api_key")
    fun provideWhoisXmlApiKey(): String = BuildConfig.WHOISXML_API_KEY
}
