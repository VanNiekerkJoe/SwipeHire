package com.swipehire.app.data

import com.google.firebase.auth.FirebaseAuth

/** Central identity handoff: real Firebase UID after SSO, stable demo ID before auth lands. */
fun currentFirebaseUserId(fallback: String): String =
    FirebaseAuth.getInstance().currentUser?.uid ?: fallback
