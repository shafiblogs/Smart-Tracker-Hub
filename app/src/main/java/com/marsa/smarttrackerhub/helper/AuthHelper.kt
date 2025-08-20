package com.marsa.smarttrackerhub.helper

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.marsa.smarttrackerhub.data.entity.UserAccount
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * Created by Muhammed Shafi on 06/08/2025.
 * Moro Hub
 * muhammed.poyil@morohub.com
 */

object AuthHelper {
    private suspend fun signInAnonymously(): Boolean {
        val auth = FirebaseAuth.getInstance()

        // Already signed in
        if (auth.currentUser != null) return true

        return suspendCoroutine { cont ->
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d("##PDF", "signInAnonymously - addOnSuccessListener")
                    cont.resume(true)
                }
                .addOnFailureListener {
                    Log.d("##PDF", "signInAnonymously - addOnFailureListener ${it.message}")
                    cont.resume(false)
                }
        }
    }

    suspend fun updateFireStore(
        context: Context,
        account: UserAccount,
        onSuccess: () -> Unit = {},
        onFail: (String) -> Unit = {}
    ) {
        // Step 1: Sign in anonymously (if not already signed in)
        val signedIn = signInAnonymously()

        if (!signedIn) {
            Log.e("##PDF", "Anonymous sign-in failed.")
            return onFail("Anonymous sign-in failed")
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                saveTokenToPreferences(context, token)
                Log.d("##PDF", "New FCM Token - Account Setup: $token")
                val shopInfo = mapOf(
                    "name" to account.userName,
                    "role" to account.userRole,
                    "token" to token
                )

                FirebaseFirestore.getInstance()
                    .collection("accounts")
                    .document(account.userName + "_" + token.take(8))
                    .set(shopInfo, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("##PDF", "addOnSuccessListener - Account Setup")
                        onSuccess()
                    }
                    .addOnFailureListener {
                        Log.d(
                            "##PDF",
                            "addOnFailureListener - Account Setup: ${it.localizedMessage}"
                        )
                        onFail("Registration/Update Failed")
                    }
            }
    }
}