package io.github.howshous.data.auth

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.ui.data.clearSession
import kotlinx.coroutines.tasks.await
import io.github.howshous.ui.data.saveRole
import io.github.howshous.ui.data.saveUid

data class SimpleUser(
    val uid: String,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String?,
    val role: String
)

class AuthRepository(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun signUpWithEmail(user: SimpleUser, password: String): Result<String> {
        return try {
            if ((user.role == "administrator" || user.role == "admin") && !AdminConfig.isAllowedAdminEmail(user.email)) {
                return Result.failure(IllegalArgumentException("Administrator accounts cannot be created in-app."))
            }
            val res = auth.createUserWithEmailAndPassword(user.email!!, password).await()
            val uid = res.user!!.uid
            val displayName = "${user.firstName} ${user.lastName}".trim()
            if (displayName.isNotEmpty()) {
                val request = userProfileChangeRequest { this.displayName = displayName }
                res.user?.updateProfile(request)?.await()
            }
            // Save user doc in Firestore
            val userMap = mapOf(
                "uid" to uid,
                "firstName" to user.firstName,
                "lastName" to user.lastName,
                "email" to user.email,
                "phone" to (user.phone ?: ""),
                "role" to user.role,
                "isBanned" to false,
                "bannedAt" to null,
                "bannedBy" to "",
                "banReason" to "",
                "profileImageUrl" to "",
                "verified" to false,
                "createdAt" to Timestamp.now()
            )
            db.collection("users").document(uid).set(userMap).await()

            // persist local session
            saveRole(context, user.role)
            saveUid(context, uid)
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<String> {
        return try {
            val res = auth.signInWithEmailAndPassword(email, password).await()
            val uid = res.user!!.uid
            val authEmail = res.user?.email?.trim().orEmpty()
            val doc = db.collection("users").document(uid).get().await()
            if (!doc.exists() && AdminConfig.isAllowedAdminEmail(authEmail)) {
                val displayName = res.user?.displayName?.trim().orEmpty()
                val nameParts = displayName.split(" ").filter { it.isNotBlank() }
                val firstName = nameParts.firstOrNull() ?: "Admin"
                val lastName = nameParts.drop(1).joinToString(" ").ifBlank { "User" }
                val userMap = mapOf(
                    "uid" to uid,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to authEmail,
                    "phone" to "",
                    "role" to "administrator",
                    "isBanned" to false,
                    "bannedAt" to null,
                    "bannedBy" to "",
                    "banReason" to "",
                    "profileImageUrl" to "",
                    "verified" to true,
                    "createdAt" to Timestamp.now()
                )
                db.collection("users").document(uid).set(userMap).await()
            }
            val profileDoc = if (doc.exists()) doc else db.collection("users").document(uid).get().await()
            val role = profileDoc.getString("role") ?: ""
            val isBanned = profileDoc.getBoolean("isBanned") ?: false
            if (isBanned) {
                saveRole(context, "banned")
                saveUid(context, uid)
                return Result.success(uid)
            }
            saveRole(context, role)
            saveUid(context, uid)
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        auth.signOut()
        clearSession(context)
    }
}
