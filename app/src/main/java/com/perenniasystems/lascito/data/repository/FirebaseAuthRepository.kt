package com.perenniasystems.lascito.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of [UserRepository].
 *
 * - login / register delegates to FirebaseAuth.
 * - On successful register, creates a `users/{uid}` document in Firestore
 *   with the user's displayName, email, and creation timestamp.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db:   FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    override suspend fun login(email: String, password: String): Result<String> =
        runCatching {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            result.user?.uid ?: throw Exception("Login failed — no UID returned")
        }

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): Result<String> = runCatching {
        // 1. Create the auth account
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val uid = result.user?.uid ?: throw Exception("Register failed — no UID returned")

        // 2. Set display name on the auth profile
        result.user!!.updateProfile(
            userProfileChangeRequest { displayName = name.trim() }
        ).await()

        // 3. Persist basic info in Firestore `users` collection
        val userData = mapOf(
            "displayName" to name.trim(),
            "email"       to email.trim(),
            "createdAt"   to System.currentTimeMillis()
        )
        db.collection("users").document(uid).set(userData).await()

        uid
    }

    override fun currentUserId(): String? = auth.currentUser?.uid

    override fun logout() { auth.signOut() }

    override suspend fun deleteUserDocument(userId: String): Result<Unit> = runCatching {
        db.collection("users").document(userId).delete().await()
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw Exception("No authenticated user")
        user.delete().await()
    }
}
