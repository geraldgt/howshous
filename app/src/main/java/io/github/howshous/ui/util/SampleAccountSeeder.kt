package io.github.howshous.ui.util

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.R
import io.github.howshous.data.firestore.AnalyticsRepository
import io.github.howshous.data.firestore.ListingRepository
import kotlinx.coroutines.tasks.await

object SampleAccountSeeder {

    data class SeedResult(
        val created: Boolean,
        val message: String
    )

    private const val TENANT_FIRST = "Timmy"
    private const val TENANT_LAST = "Tenant"
    private const val TENANT_EMAIL = "tt@hh.com"
    private const val TENANT_PASSWORD = "timmytimmy"

    private const val TENANT2_FIRST = "Terry"
    private const val TENANT2_LAST = "Benevolence"
    private const val TENANT2_EMAIL = "tb@hh.com"
    private const val TENANT2_PASSWORD = "terryterry"

    private const val LANDLORD_FIRST = "Lemm"
    private const val LANDLORD_LAST = "Landlord"
    private const val LANDLORD_EMAIL = "ll@hh.com"
    private const val LANDLORD_PASSWORD = "lemmlemm"

    private const val LANDLORD2_FIRST = "Larry"
    private const val LANDLORD2_LAST = "Bantrot"
    private const val LANDLORD2_EMAIL = "lb@hh.com"
    private const val LANDLORD2_PASSWORD = "larrylarry"

    private const val ADMIN_FIRST = "Annie"
    private const val ADMIN_LAST = "Admin"
    private const val ADMIN_EMAIL = "aa@hh.com"
    private const val ADMIN_PASSWORD = "annieannie"

    suspend fun generateIfMissing(context: Context): SeedResult {
        val auth = FirebaseAuth.getInstance()

        return try {
            val tenantUid = ensureAccount(
                auth = auth,
                email = TENANT_EMAIL,
                password = TENANT_PASSWORD
            ) { createTenantAccount(context, TENANT_FIRST, TENANT_LAST, TENANT_EMAIL, TENANT_PASSWORD) }
            auth.signOut()

            val tenant2Uid = ensureAccount(
                auth = auth,
                email = TENANT2_EMAIL,
                password = TENANT2_PASSWORD
            ) { createTenantAccount(context, TENANT2_FIRST, TENANT2_LAST, TENANT2_EMAIL, TENANT2_PASSWORD) }
            auth.signOut()

            val landlordUid = ensureAccount(
                auth = auth,
                email = LANDLORD_EMAIL,
                password = LANDLORD_PASSWORD
            ) { createLandlordAccount(context, LANDLORD_FIRST, LANDLORD_LAST, LANDLORD_EMAIL, LANDLORD_PASSWORD) }
            auth.signOut()

            val landlord2Uid = ensureAccount(
                auth = auth,
                email = LANDLORD2_EMAIL,
                password = LANDLORD2_PASSWORD
            ) { createLandlordAccount(context, LANDLORD2_FIRST, LANDLORD2_LAST, LANDLORD2_EMAIL, LANDLORD2_PASSWORD) }
            auth.signOut()

            // Listing writes are expected to be authored by the landlord account.
            auth.signInWithEmailAndPassword(LANDLORD_EMAIL, LANDLORD_PASSWORD).await()
            val lemmListingIds = SampleListingsGenerator.generateSampleListings(landlordUid)
            auth.signOut()

            auth.signInWithEmailAndPassword(LANDLORD2_EMAIL, LANDLORD2_PASSWORD).await()
            val larryListingIds = SampleListingsGenerator.generateSampleListings(
                landlord2Uid,
                titlePrefix = "Larry's"
            )
            auth.signOut()

            val adminUid = ensureAccount(
                auth = auth,
                email = ADMIN_EMAIL,
                password = ADMIN_PASSWORD
            ) { createAdminAccount(context) }
            val listingRepo = ListingRepository()
            val analyticsRepo = AnalyticsRepository()
            val lemmListings = listingRepo.getListingsForLandlord(landlordUid)
            if (lemmListings.isNotEmpty()) {
                analyticsRepo.seedTestEventsForLandlord(
                    landlordId = landlordUid,
                    listings = lemmListings.map { it.id to it.price }
                )
            }
            val larryListings = listingRepo.getListingsForLandlord(landlord2Uid)
            if (larryListings.isNotEmpty()) {
                analyticsRepo.seedTestEventsForLandlord(
                    landlordId = landlord2Uid,
                    listings = larryListings.map { it.id to it.price }
                )
            }

            auth.signOut()

            SeedResult(
                true,
                "Sample accounts ready (tenant1=$tenantUid, tenant2=$tenant2Uid, landlord1=$landlordUid, landlord2=$landlord2Uid, admin=$adminUid). Recreated listings: lemm=${lemmListingIds.size}, larry=${larryListingIds.size}."
            )
        } catch (e: Exception) {
            auth.signOut()
            SeedResult(false, "Failed to generate sample data: ${e.message}")
        }
    }

    private suspend fun hasAccountForEmail(auth: FirebaseAuth, email: String): Boolean {
        return try {
            val methods = auth.fetchSignInMethodsForEmail(email).await().signInMethods
            !methods.isNullOrEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun ensureAccount(
        auth: FirebaseAuth,
        email: String,
        password: String,
        createNew: suspend () -> String
    ): String {
        return if (hasAccountForEmail(auth, email)) {
            resolveExistingUid(auth, email, password)
        } else {
            createNew()
        }
    }

    private suspend fun resolveExistingUid(
        auth: FirebaseAuth,
        email: String,
        password: String
    ): String {
        val byDoc = findUserUidByEmail(email)
        if (!byDoc.isNullOrBlank()) return byDoc

        val signIn = auth.signInWithEmailAndPassword(email, password).await()
        return signIn.user?.uid
            ?: throw IllegalStateException("Unable to resolve UID for existing account: $email")
    }

    private suspend fun findUserUidByEmail(email: String): String? {
        val db = FirebaseFirestore.getInstance()
        val snap = db.collection("users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .await()
        return snap.documents.firstOrNull()?.id
    }

    private suspend fun createTenantAccount(
        context: Context,
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): String {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val res = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = res.user!!.uid
        val displayName = "$firstName $lastName"
        res.user?.updateProfile(userProfileChangeRequest { this.displayName = displayName })?.await()

        val selfieUri = resUri(context, R.drawable.test_pfp)
        val idUri = resUri(context, R.drawable.test_id_card)

        val profileUrl = uploadCompressedImage(context, selfieUri, "users/$uid/profile.jpg")
        val selfieVerificationUrl = uploadCompressedImage(context, selfieUri, "verifications/$uid/selfie.jpg")
        val idVerificationUrl = uploadCompressedImage(context, idUri, "verifications/$uid/id.jpg")

        val userDoc = hashMapOf(
            "uid" to uid,
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "role" to "tenant",
            "verified" to false,
            "isBanned" to false,
            "bannedAt" to null,
            "bannedBy" to "",
            "banReason" to "",
            "profileImageUrl" to profileUrl,
            "businessPermitUrl" to ""
        )

        db.collection("users").document(uid).set(userDoc).await()

        val verificationDoc = hashMapOf(
            "selfieUrl" to selfieVerificationUrl,
            "idUrl" to idVerificationUrl,
            "propertyUrl" to "",
            "status" to "pending",
            "submittedAt" to FieldValue.serverTimestamp()
        )

        db.collection("verifications").document(uid).set(verificationDoc).await()

        return uid
    }

    private suspend fun createLandlordAccount(
        context: Context,
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): String {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val res = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = res.user!!.uid
        val displayName = "$firstName $lastName"
        res.user?.updateProfile(userProfileChangeRequest { this.displayName = displayName })?.await()

        val selfieUri = resUri(context, R.drawable.test_pfp)
        val idUri = resUri(context, R.drawable.test_id_card)
        val permitUri = resUri(context, R.drawable.test_permit)

        val profileUrl = uploadCompressedImage(context, selfieUri, "users/$uid/profile.jpg")
        val selfieVerificationUrl = uploadCompressedImage(context, selfieUri, "verifications/$uid/selfie.jpg")
        val idVerificationUrl = uploadCompressedImage(context, idUri, "verifications/$uid/id.jpg")
        val propertyVerificationUrl = uploadCompressedImage(context, permitUri, "verifications/$uid/property.jpg")

        val userDoc = hashMapOf(
            "uid" to uid,
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "role" to "landlord",
            "verified" to false,
            "isBanned" to false,
            "bannedAt" to null,
            "bannedBy" to "",
            "banReason" to "",
            "profileImageUrl" to profileUrl,
            "businessPermitUrl" to propertyVerificationUrl
        )

        db.collection("users").document(uid).set(userDoc).await()

        val verificationDoc = hashMapOf(
            "selfieUrl" to selfieVerificationUrl,
            "idUrl" to idVerificationUrl,
            "propertyUrl" to propertyVerificationUrl,
            "status" to "pending",
            "submittedAt" to FieldValue.serverTimestamp()
        )

        db.collection("verifications").document(uid).set(verificationDoc).await()

        return uid
    }

    private suspend fun createAdminAccount(context: Context): String {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val res = auth.createUserWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD).await()
        val uid = res.user!!.uid
        val displayName = "$ADMIN_FIRST $ADMIN_LAST"
        res.user?.updateProfile(userProfileChangeRequest { this.displayName = displayName })?.await()

        val selfieUri = resUri(context, R.drawable.test_pfp)
        val profileUrl = uploadCompressedImage(context, selfieUri, "users/$uid/profile.jpg")

        val userDoc = hashMapOf(
            "uid" to uid,
            "firstName" to ADMIN_FIRST,
            "lastName" to ADMIN_LAST,
            "email" to ADMIN_EMAIL,
            "role" to "administrator",
            "verified" to true,
            "isBanned" to false,
            "profileImageUrl" to profileUrl,
            "businessPermitUrl" to ""
        )

        db.collection("users").document(uid).set(userDoc).await()

        return uid
    }

    private fun resUri(context: Context, @DrawableRes resId: Int): Uri {
        return Uri.parse("android.resource://${context.packageName}/$resId")
    }
}
