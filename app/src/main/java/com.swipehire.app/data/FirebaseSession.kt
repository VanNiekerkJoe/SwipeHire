package com.swipehire.app.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth

/** Returns the authenticated Firebase UID. There are deliberately no demo-user fallbacks. */
fun currentFirebaseUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

/** Ends both the Firebase session and Credential Manager's active provider session. */
suspend fun signOutFirebaseUser(context: Context) {
    FirebaseAuth.getInstance().signOut()
    runCatching {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }
}
