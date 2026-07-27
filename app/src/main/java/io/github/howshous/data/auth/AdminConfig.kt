package io.github.howshous.data.auth

/**
 * Emails allowed to hold the administrator role.
 * Must stay in sync with the allowlist in [firestore.rules] (`isAllowedAdministratorProfileCreate`).
 *
 * To add an app manager: add their email here, deploy Firestore rules with the same email,
 * create their account in Firebase Authentication, then sign in once in the app.
 */
object AdminConfig {
    private val allowedAdminEmails = setOf(
        "aa@hh.com",
    )

    fun isAllowedAdminEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return email.trim().lowercase() in allowedAdminEmails
    }
}
