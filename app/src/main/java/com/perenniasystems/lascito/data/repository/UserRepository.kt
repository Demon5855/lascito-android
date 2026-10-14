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

    /** Sends the password reset email to [email]. */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /** Re-authenticates the current user with [currentPassword] and sets [newPassword]. */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>

    /** Reads the display name stored in `users/{uid}`. */
    suspend fun fetchDisplayName(uid: String): Result<String?>

    /** Verifies and deletes the user document from Firestore */
    suspend fun deleteUserDocument(userId: String): Result<Unit>

    /** Deletes the Auth account completely */
    suspend fun deleteAccount(): Result<Unit>
}
