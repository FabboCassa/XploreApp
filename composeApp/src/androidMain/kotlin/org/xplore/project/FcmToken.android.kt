package org.xplore.project

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

actual suspend fun getCurrentFcmToken(): String? = runCatching {
    FirebaseMessaging.getInstance().token.await()
}.getOrNull()
