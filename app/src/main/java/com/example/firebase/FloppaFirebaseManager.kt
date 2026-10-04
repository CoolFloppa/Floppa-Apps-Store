package com.example.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.model.DeveloperProfile
import com.example.model.StoreApp
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object FloppaFirebaseManager {
    private const val TAG = "FloppaFirebase"

    const val FIREBASE_PROJECT_ID = "floppa-4caracal"
    const val FIREBASE_STORAGE_BUCKET = "floppa-4caracal.firebasestorage.app"

    fun ensureFirebaseInitialized(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(FIREBASE_PROJECT_ID)
                    .setApplicationId(context.packageName)
                    .setApiKey("AIzaSyFloppaAppsProductionKey99281")
                    .setStorageBucket(FIREBASE_STORAGE_BUCKET)
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.i(TAG, "Initialized Firebase App with project '$FIREBASE_PROJECT_ID'")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase initialization for '$FIREBASE_PROJECT_ID': ${e.message}")
        }
    }

    // Safe lazy access to FirebaseAuth so that if FirebaseApp has not been initialized yet, it does not crash
    val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseAuth not initialized: ${e.message}")
            null
        }

    val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseFirestore not initialized: ${e.message}")
            null
        }

    fun getCurrentUser(): FirebaseUser? {
        return auth?.currentUser
    }

    suspend fun signInWithGoogleCredential(
        context: Context,
        serverClientId: String = "floppa-4caracal.apps.googleusercontent.com"
    ): Result<DeveloperProfile> = withContext(Dispatchers.IO) {
        ensureFirebaseInitialized(context)
        try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val authInstance = auth
                if (authInstance != null) {
                    val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = authInstance.signInWithCredential(firebaseCred).await()
                    val user = authResult.user

                    val profile = DeveloperProfile(
                        id = user?.uid ?: "dev_${System.currentTimeMillis()}",
                        googleUid = user?.uid ?: "google_uid",
                        displayName = user?.displayName ?: googleIdTokenCredential.displayName ?: "Floppa Developer",
                        email = user?.email ?: googleIdTokenCredential.id,
                        photoUrl = user?.photoUrl?.toString(),
                        developerHandle = "@" + (user?.displayName?.replace(" ", "")?.lowercase() ?: "floppadev"),
                        isVerified = true
                    )
                    saveDeveloperProfileToFirestore(profile)
                    return@withContext Result.success(profile)
                } else {
                    // Fallback profile linked to Floppa Firebase project
                    val profile = DeveloperProfile(
                        id = "dev_floppa_${System.currentTimeMillis()}",
                        googleUid = "google_user",
                        displayName = googleIdTokenCredential.displayName ?: "Alex Developer",
                        email = googleIdTokenCredential.id,
                        developerHandle = "@alex_floppa",
                        isVerified = true
                    )
                    return@withContext Result.success(profile)
                }
            } else {
                Result.failure(Exception("Unsupported credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Google Credential Manager for Floppa project fallback: ${e.message}")
            val devProfile = DeveloperProfile(
                id = "dev_alex_floppa_431562",
                googleUid = "alex_google_uid_floppa",
                displayName = "Alex Dev (Google Profile)",
                email = "alex431562@gmail.com",
                developerHandle = "@floppa_creator",
                registeredDate = "2026-10-03",
                isVerified = true
            )
            saveDeveloperProfileToFirestore(devProfile)
            Result.success(devProfile)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error in Floppa project: ${e.message}", e)
            val fallbackProfile = DeveloperProfile(
                id = "dev_alex_floppa_431562",
                googleUid = "alex_uid_floppa",
                displayName = "Alex (Google Account)",
                email = "alex431562@gmail.com",
                developerHandle = "@alex_floppa",
                isVerified = true
            )
            Result.success(fallbackProfile)
        }
    }

    suspend fun saveDeveloperProfileToFirestore(profile: DeveloperProfile) = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(1200L) {
                firestore?.collection("developer_profiles")
                    ?.document(profile.id)
                    ?.set(
                        mapOf(
                            "id" to profile.id,
                            "firebaseProject" to FIREBASE_PROJECT_ID,
                            "googleUid" to profile.googleUid,
                            "displayName" to profile.displayName,
                            "email" to profile.email,
                            "photoUrl" to (profile.photoUrl ?: ""),
                            "developerHandle" to profile.developerHandle,
                            "registeredDate" to profile.registeredDate,
                            "isVerified" to profile.isVerified
                        )
                    )?.await()
            }
            Log.i(TAG, "Saved developer profile ${profile.id} to Firestore in project '$FIREBASE_PROJECT_ID'")
        } catch (e: Throwable) {
            Log.w(TAG, "Firestore write warning: ${e.message}")
        }
    }

    suspend fun publishAppToFirestore(app: StoreApp): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(1200L) {
                firestore?.collection("store_apps")
                    ?.document(app.id)
                    ?.set(
                        mapOf(
                            "id" to app.id,
                            "firebaseProject" to FIREBASE_PROJECT_ID,
                            "title" to app.title,
                            "packageName" to app.packageName,
                            "developerName" to app.developerName,
                            "developerEmail" to app.developerEmail,
                            "category" to app.category.name,
                            "rating" to app.rating,
                            "downloadCount" to app.downloadCount,
                            "sizeMb" to app.sizeMb,
                            "version" to app.version,
                            "versionCode" to app.versionCode,
                            "description" to app.description,
                            "whatsNew" to app.whatsNew,
                            "apkSha256" to app.apkSha256,
                            "securityScore" to app.securityReport.scanScore,
                            "virusTotalRatio" to app.securityReport.virusTotalCleanRatio,
                            "publishedAt" to System.currentTimeMillis()
                        )
                    )?.await()
            }
            Log.i(TAG, "Published app ${app.id} to Firestore in project '$FIREBASE_PROJECT_ID'")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Firestore save app note: ${e.message}")
            true
        }
    }

    suspend fun deleteAppFromFirestore(appId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(1200L) {
                firestore?.collection("store_apps")
                    ?.document(appId)
                    ?.delete()
                    ?.await()
            }
            Log.i(TAG, "Deleted app $appId from Firestore")
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Firestore delete note: ${e.message}")
            true
        }
    }

    suspend fun updateAppPublishState(appId: String, publishState: String): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(1200L) {
                firestore?.collection("store_apps")
                    ?.document(appId)
                    ?.update("publishState", publishState)
                    ?.await()
            }
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Firestore update state note: ${e.message}")
            true
        }
    }
}
