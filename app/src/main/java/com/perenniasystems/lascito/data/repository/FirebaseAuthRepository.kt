package com.perenniasystems.lascito.data.repository

import com.google.firebase.auth.EmailAuthProvider
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Firebase implementation of [UserRepository].
 *
 * - login / register delegates to FirebaseAuth.
 * - On successful register, creates a `users/{uid}` document in Firestore
 *   with the user's displayName, email, and creation timestamp.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db:   FirebaseFirestore
) : UserRepository {

    override suspend fun login(email: String, password: String): Result<String> =
        runCatching {
            withTimeout(10_000L) {
                val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
                result.user?.uid ?: throw Exception("Login failed — no UID returned")
            }
        }

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): Result<String> = runCatching {
        withTimeout(10_000L) {
            // 1. Create the auth account
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val uid = result.user?.uid ?: throw Exception("Register failed — no UID returned")

            // 2. Set display name on the auth legacy
            result.user!!.updateProfile(
                userProfileChangeRequest { displayName = name.trim() }
            ).await()

            // 3. Send email verification (fire and forget)
            runCatching {
                result.user!!.sendEmailVerification().await()
            }

            // 4. Persist basic info in Firestore `users` collection
            val userData = mapOf(
                "displayName" to name.trim(),
                "email"       to email.trim(),
                "createdAt"   to System.currentTimeMillis()
            )
            db.collection("users").document(uid).set(userData).await()

            uid
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        runCatching {
            val user = auth.currentUser ?: throw IllegalStateException("No authenticated user")
            val email = user.email ?: throw IllegalStateException("User email not found")
            user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
            user.updatePassword(newPassword).await()
        }

    override suspend fun fetchDisplayName(uid: String): Result<String?> = runCatching {
        db.collection("users").document(uid).get().await().getString("displayName")
    }

    override suspend fun deleteUserDocument(userId: String): Result<Unit> = runCatching {
        withTimeout(10_000L) {
            db.collection("users").document(userId).delete().await()
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        withTimeout(10_000L) {
            val user = auth.currentUser ?: throw Exception("No authenticated user")
            user.delete().await()
        }
    }
}
