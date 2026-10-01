package com.example.carwash.di

import com.example.carwash.repository.FirebaseSaleRepository
import com.example.carwash.repository.InMemorySettingsRepository
import com.example.carwash.repository.SaleRepository
import com.example.carwash.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSaleRepository(
        firebaseSaleRepository: FirebaseSaleRepository
    ): SaleRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        inMemorySettingsRepository: InMemorySettingsRepository
    ): SettingsRepository
}
