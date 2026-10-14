package com.perenniasystems.lascito.data.di

import com.perenniasystems.lascito.data.repository.FirebaseAuthRepository
import com.perenniasystems.lascito.data.repository.FirebaseGuardianRepository
import com.perenniasystems.lascito.data.repository.FirebaseMemoryRepository
import com.perenniasystems.lascito.data.repository.FirebaseProfileRepository
import com.perenniasystems.lascito.data.repository.FirebaseStorageRepository
import com.perenniasystems.lascito.data.repository.FirebaseStorageRepositoryImpl
import com.perenniasystems.lascito.data.repository.GuardianRepository
import com.perenniasystems.lascito.data.repository.MemoryRepository
import com.perenniasystems.lascito.data.repository.ProfileRepository
import com.perenniasystems.lascito.data.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Enlaza cada contrato de repositorio con su implementación de Firebase. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindUserRepository(impl: FirebaseAuthRepository): UserRepository

    @Binds
    abstract fun bindGuardianRepository(impl: FirebaseGuardianRepository): GuardianRepository

    @Binds
    abstract fun bindProfileRepository(impl: FirebaseProfileRepository): ProfileRepository

    @Binds
    abstract fun bindMemoryRepository(impl: FirebaseMemoryRepository): MemoryRepository

    @Binds
    abstract fun bindStorageRepository(impl: FirebaseStorageRepositoryImpl): FirebaseStorageRepository
}
