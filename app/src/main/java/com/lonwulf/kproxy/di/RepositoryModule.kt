package com.lonwulf.kproxy.di

import android.content.Context
import com.lonwulf.kproxy.NetworkRotator
import com.lonwulf.kproxy.repository.ProxyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {


    @Provides
    @Singleton
    fun provideNetworkRotator(@ApplicationContext context: Context): NetworkRotator {
        return NetworkRotator(context)
    }

    @Provides
    @Singleton
    fun provideProxyRepository(
        @ApplicationContext context: Context
    ): ProxyRepository {
        return ProxyRepository(context)
    }
}