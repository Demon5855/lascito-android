package com.perenniasystems.lascito.data.repository

/**
 * Authentication contract.
 *
 * Phase 1 implementation: [MockUserRepository] (synchronous, in-memory).
 * Phase 2 implementation: [FirebaseAuthRepository] (async, Firebase Auth + Firestore).
 */
interface UserRepository {
    /** Authenticate and return the user UID on success. */
    suspend fun login(email: String, password: String): Result<String>

    /** Register a new user and return the UID on success. */
    suspend fun register(name: String, email: String, password: String): Result<String>

    /** Returns the currently signed-in user's UID, or null if signed out. */
    fun currentUserId(): String?

    /** Sign out the current user. */
    fun logout()
}
