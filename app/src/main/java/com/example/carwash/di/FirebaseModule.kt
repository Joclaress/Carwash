package com.example.carwash.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        val storage = try {
            FirebaseStorage.getInstance()
        } catch (_: Exception) {
            try {
                FirebaseStorage.getInstance("gs://custoworks-carwash.firebasestorage.app")
            } catch (_: Exception) {
                FirebaseStorage.getInstance("gs://custoworks-carwash.appspot.com")
            }
        }
        // Set upload and operation retry timeouts to 30 seconds to allow responsive fallback
        storage.maxUploadRetryTimeMillis = 30_000L
        storage.maxOperationRetryTimeMillis = 30_000L
        return storage
    }
}
