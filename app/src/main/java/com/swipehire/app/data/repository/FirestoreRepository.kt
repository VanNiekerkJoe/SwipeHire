package com.swipehire.app.data.repository

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.AlertType
import com.swipehire.app.data.AppAlert
import com.swipehire.app.data.ChatMessage
import com.swipehire.app.data.CompanyProfile
import com.swipehire.app.data.JobPosting
import com.swipehire.app.data.MatchChat
import com.swipehire.app.data.MessageDeliveryState
import com.swipehire.app.data.StudentProfile
import com.swipehire.app.data.remote.ChatMessageDto
import com.swipehire.app.data.remote.CompanyProfileDto
import com.swipehire.app.data.remote.CreateCompanyDto
import com.swipehire.app.data.remote.CreateJobPostingDto
import com.swipehire.app.data.remote.CreateStudentDto
import com.swipehire.app.data.remote.JobPostingDto
import com.swipehire.app.data.remote.MatchDto
import com.swipehire.app.data.remote.NotificationDto
import com.swipehire.app.data.remote.ProfileResponseDto
import com.swipehire.app.data.remote.SavedItemsDto
import com.swipehire.app.data.remote.StudentProfileDto
import com.swipehire.app.data.remote.SwipeResponse
import com.swipehire.app.data.remote.UserSettingsDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db: FirebaseFirestore?
        get() = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }

    suspend fun getJobPostings(): List<JobPosting> = try {
        db?.collection("jobs")?.get()?.await()?.documents
            ?.mapNotNull { it.toObject(JobPostingDto::class.java) }
            ?.filter { it.profileVisible }
            ?.map { it.toDomain() }
            ?.filter { it.companyId.isNotBlank() }.orEmpty()
    } catch (_: Exception) { emptyList() }

    suspend fun getCompanyJobs(companyId: String): List<JobPosting> = try {
        db?.collection("jobs")?.whereEqualTo("companyId", companyId)?.get()?.await()?.documents
            ?.mapNotNull { it.toObject(JobPostingDto::class.java)?.toDomain() }.orEmpty()
    } catch (_: Exception) { emptyList() }

    suspend fun getStudentProfiles(): List<StudentProfile> = try {
        db?.collection("students")?.get()?.await()?.documents
            ?.filter { it.getString("userId") == it.id }
            ?.mapNotNull { it.toObject(StudentProfileDto::class.java) }
            ?.filter { it.profileVisible }
            ?.map { it.toDomain() }.orEmpty()
    } catch (_: Exception) { emptyList() }

    suspend fun getStudentProfile(userId: String): StudentProfile? = try {
        db?.collection("students")?.document(userId)?.get()?.await()
            ?.toObject(StudentProfileDto::class.java)?.toDomain()
    } catch (_: Exception) { null }

    suspend fun getCompanyProfile(userId: String): CompanyProfile? = try {
        db?.collection("companies")?.document(userId)?.get()?.await()
            ?.toObject(CompanyProfileDto::class.java)?.toDomain()
    } catch (_: Exception) { null }

    suspend fun getAccountType(userId: String): AccountType? = try {
        db?.collection("users")?.document(userId)?.get()?.await()?.getString("role")
            ?.let { runCatching { AccountType.valueOf(it) }.getOrNull() }
    } catch (_: Exception) { null }

    suspend fun setAccountType(userId: String, type: AccountType): Boolean = try {
        db?.collection("users")?.document(userId)?.set(
            mapOf("role" to type.name, "updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge()
        )?.await()
        true
    } catch (_: Exception) { false }

    fun getMatches(userId: String): Flow<List<MatchChat>> = callbackFlow {
        val firestore = db
        if (firestore == null) { trySend(emptyList()); close(); return@callbackFlow }
        val listener = firestore.collection("matches")
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                launch {
                    val matches = snapshot?.documents.orEmpty().mapNotNull { document ->
                        // A malformed legacy record must not bring down the real-time listener.
                        val dto = runCatching { document.toMatchDto() }.getOrNull()
                            ?: return@mapNotNull null
                        if (!dto.mutual) return@mapNotNull null
                        toMatchChat(dto, userId)
                    }
                    trySend(matches)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getMatch(matchId: String, userId: String): MatchChat? {
        return try {
            val dto = db?.collection("matches")?.document(matchId)?.get()?.await()
                ?.toMatchDto() ?: return null
            if (!dto.mutual || userId !in dto.participantIds) null else toMatchChat(dto, userId)
        } catch (_: Exception) { null }
    }

    private fun DocumentSnapshot.toMatchDto(): MatchDto? =
        toObject(MatchDto::class.java)?.let { dto ->
            // Older documents may not contain an explicit `id`, so use the
            // Firestore document ID as a safe fallback.
            dto.copy(id = dto.id.ifBlank { id })
        }

    private suspend fun toMatchChat(dto: MatchDto, userId: String): MatchChat {
        val otherId = dto.participantIds.firstOrNull { it != userId }.orEmpty()
        val student = getStudentProfile(otherId)
        val company = if (student == null) getCompanyProfile(otherId) else null
        return MatchChat(
            id = dto.id,
            participantIds = dto.participantIds,
            name = student?.name ?: company?.name ?: "SwipeHire user",
            subtitle = student?.let { "${it.course} · ${it.year}" }
                ?: company?.let { "${it.industry} · ${it.location}" }.orEmpty(),
            avatarInitials = student?.avatarInitials ?: company?.logoInitials ?: "SH",
            lastMessage = dto.lastMessage.ifBlank { "You matched — start the conversation!" },
            unread = userId in dto.unreadBy,
            otherUserId = otherId,
            studentCvPath = student?.cvPath.orEmpty(),
            studentCvFileName = student?.cvFileName.orEmpty()
        )
    }

    fun getLiveMessages(matchId: String, currentUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        val firestore = db
        if (firestore == null) { trySend(emptyList()); close(); return@callbackFlow }
        val match = firestore.collection("matches").document(matchId).get().await()
        val participants = (match.get("participantIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
        if (match.getBoolean("mutual") != true || currentUserId !in participants) {
            trySend(emptyList()); close(); return@callbackFlow
        }

        firestore.collection("matches").document(matchId).set(
            mapOf("unreadBy" to FieldValue.arrayRemove(currentUserId)), SetOptions.merge()
        ).await()
        val listener = firestore.collection("matches").document(matchId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val documents = snapshot?.documents.orEmpty()
                trySend(documents.mapNotNull { doc ->
                    doc.toObject(ChatMessageDto::class.java)?.let {
                        val fromMe = it.senderId == currentUserId
                        val deliveryState = when {
                            !fromMe -> MessageDeliveryState.READ
                            doc.metadata.hasPendingWrites() -> MessageDeliveryState.SENDING
                            it.readBy.any { readerId -> readerId != currentUserId } -> MessageDeliveryState.READ
                            else -> MessageDeliveryState.SENT
                        }
                        ChatMessage(
                            id = it.id,
                            text = it.text,
                            fromMe = fromMe,
                            sentAtMillis = it.timestamp?.toDate()?.time ?: System.currentTimeMillis(),
                            deliveryState = deliveryState
                        )
                    }
                })

                val unreadMessages = documents.filter { document ->
                    document.getString("senderId") != currentUserId &&
                        currentUserId !in (document.get("readBy") as? List<*>)?.filterIsInstance<String>().orEmpty()
                }
                if (unreadMessages.isNotEmpty()) {
                    launch {
                        val batch = firestore.batch()
                        unreadMessages.forEach { document ->
                            batch.update(document.reference, "readBy", FieldValue.arrayUnion(currentUserId))
                        }
                        runCatching { batch.commit().await() }
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(matchId: String, senderId: String, text: String): Boolean {
        val firestore = db ?: return false
        return try {
            val matchRef = firestore.collection("matches").document(matchId)
            val snapshot = matchRef.get().await()
            val participants = (snapshot.get("participantIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            if (snapshot.getBoolean("mutual") != true || senderId !in participants) return false
            matchRef.collection("messages").add(
                mapOf(
                    "senderId" to senderId,
                    "text" to text.trim(),
                    "timestamp" to FieldValue.serverTimestamp(),
                    "readBy" to listOf(senderId)
                )
            ).await()
            val recipients = participants.filter { it != senderId }
            matchRef.set(
                mapOf("lastMessage" to text.trim(), "unreadBy" to recipients, "updatedAt" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            ).await()
            val senderName = getStudentProfile(senderId)?.name
                ?: getCompanyProfile(senderId)?.name
                ?: "SwipeHire user"
            recipients.forEach { recipientId ->
                createNotification(
                    userId = recipientId,
                    actorId = senderId,
                    type = AlertType.MESSAGE,
                    title = "New message from $senderName",
                    body = text.trim(),
                    matchId = matchId
                )
            }
            true
        } catch (_: Exception) { false }
    }

    fun getAlerts(userId: String): Flow<List<AppAlert>> = callbackFlow {
        val firestore = db
        if (firestore == null) { trySend(emptyList()); close(); return@callbackFlow }
        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                val alerts = snapshot?.documents.orEmpty().mapNotNull { document ->
                    document.toObject(NotificationDto::class.java)?.let { notification ->
                        AppAlert(
                            id = document.id,
                            type = runCatching { AlertType.valueOf(notification.type) }.getOrDefault(AlertType.MESSAGE),
                            title = notification.title,
                            body = notification.body,
                            matchId = notification.matchId,
                            actorId = notification.actorId,
                            createdAtMillis = notification.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                            isRead = notification.read
                        )
                    }
                }.sortedByDescending { it.createdAtMillis }
                trySend(alerts)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markAlertRead(userId: String, alertId: String): Boolean {
        return try {
            val reference = db?.collection("notifications")?.document(alertId) ?: return false
            val snapshot = reference.get().await()
            if (snapshot.getString("userId") != userId) return false
            reference.update("read", true).await()
            true
        } catch (_: Exception) { false }
    }

    private suspend fun createNotification(
        userId: String,
        actorId: String,
        type: AlertType,
        title: String,
        body: String,
        matchId: String,
        stableId: String? = null
    ) {
        val firestore = db ?: return
        val reference = stableId?.let { firestore.collection("notifications").document(it) }
            ?: firestore.collection("notifications").document()
        reference.set(
            mapOf(
                "userId" to userId,
                "actorId" to actorId,
                "type" to type.name,
                "title" to title,
                "body" to body,
                "matchId" to matchId,
                "createdAt" to FieldValue.serverTimestamp(),
                "read" to false
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun uploadStudentCv(userId: String, uri: Uri, fileName: String): Pair<String, String>? {
        return try {
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "cv.pdf" }
            val path = "cvs/$userId/${System.currentTimeMillis()}_$safeName"
            FirebaseStorage.getInstance().reference.child(path).putFile(uri).await()
            db?.collection("students")?.document(userId)?.set(
                mapOf(
                    "userId" to userId,
                    "cvPath" to path,
                    "cvFileName" to fileName,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )?.await()
            val firestore = db ?: return null
            val matches = firestore.collection("matches")
                .whereArrayContains("participantIds", userId).get().await().documents
            matches.filter { it.getBoolean("mutual") == true }.forEach { match ->
                val participants = (match.get("participantIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
                val companyId = participants.firstOrNull { it != userId } ?: return@forEach
                firestore.collection("cvAccess").document(userId).collection("companies").document(companyId).set(
                    mapOf(
                        "studentId" to userId,
                        "companyId" to companyId,
                        "matchId" to match.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }
            path to fileName
        } catch (_: Exception) { null }
    }

    suspend fun getCvDownloadUrl(path: String): Uri? = try {
        FirebaseStorage.getInstance().reference.child(path).downloadUrl.await()
    } catch (_: Exception) { null }

    suspend fun getIncomingInterestCount(userId: String): Int = try {
        db?.collection("swipes")?.whereEqualTo("targetUserId", userId)?.get()?.await()?.documents
            ?.count { it.getBoolean("isLike") == true } ?: 0
    } catch (_: Exception) { 0 }

    suspend fun getAverageReplyTimeMillis(userId: String): Long? {
        val firestore = db ?: return null
        return try {
            val matches = firestore.collection("matches")
                .whereArrayContains("participantIds", userId).get().await().documents
            val responseTimes = mutableListOf<Long>()
            for (match in matches) {
                val messages = match.reference.collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING).get().await().documents
                var waitingSince: Long? = null
                messages.forEach { message ->
                    val senderId = message.getString("senderId").orEmpty()
                    val timestamp = message.getTimestamp("timestamp")?.toDate()?.time ?: return@forEach
                    if (senderId == userId) {
                        waitingSince?.let { receivedAt ->
                            if (timestamp >= receivedAt) responseTimes += timestamp - receivedAt
                        }
                        waitingSince = null
                    } else if (waitingSince == null) {
                        waitingSince = timestamp
                    }
                }
            }
            responseTimes.takeIf { it.isNotEmpty() }?.average()?.toLong()
        } catch (_: Exception) { null }
    }

    suspend fun getSavedItems(userId: String): SavedItemsDto? {
        return try {
            val snapshot = db?.collection("users")?.document(userId)?.get()?.await() ?: return null
            SavedItemsDto(
                (snapshot.get("savedJobIds") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                (snapshot.get("savedStudentIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            )
        } catch (_: Exception) { null }
    }

    suspend fun setSavedItem(userId: String, kind: String, itemId: String, saved: Boolean): SavedItemsDto? = try {
        val field = if (kind == "job") "savedJobIds" else "savedStudentIds"
        val update = if (saved) FieldValue.arrayUnion(itemId) else FieldValue.arrayRemove(itemId)
        db?.collection("users")?.document(userId)?.set(
            mapOf(field to update, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge()
        )?.await()
        getSavedItems(userId)
    } catch (_: Exception) { null }

    suspend fun getUserSettings(userId: String): UserSettingsDto? {
        return try {
            val snapshot = db?.collection("users")?.document(userId)?.get()?.await() ?: return null
            val settings = snapshot.get("settings") as? Map<*, *> ?: emptyMap<Any, Any>()
            UserSettingsDto(
                settings["pushNotifications"] as? Boolean ?: true,
                settings["matchAlerts"] as? Boolean ?: true,
                settings["messageAlerts"] as? Boolean ?: true,
                settings["profileVisible"] as? Boolean ?: true,
                settings["language"] as? String ?: "en"
            )
        } catch (_: Exception) { null }
    }

    suspend fun updateUserSettings(userId: String, settings: UserSettingsDto): UserSettingsDto? {
        return try {
            val values = mapOf(
            "pushNotifications" to settings.pushNotifications,
            "matchAlerts" to settings.matchAlerts,
            "messageAlerts" to settings.messageAlerts,
            "profileVisible" to settings.profileVisible,
            "language" to settings.language
        )
            val firestore = db ?: return null
        firestore.collection("users").document(userId).set(
            mapOf("settings" to values, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge()
        ).await()
        val role = firestore.collection("users").document(userId).get().await().getString("role")
        if (role == AccountType.STUDENT.name) {
            firestore.collection("students").document(userId).set(
                mapOf("profileVisible" to settings.profileVisible), SetOptions.merge()
            ).await()
        } else if (role == AccountType.COMPANY.name) {
            firestore.collection("companies").document(userId).set(
                mapOf("profileVisible" to settings.profileVisible), SetOptions.merge()
            ).await()
            val jobs = firestore.collection("jobs").whereEqualTo("companyId", userId).get().await()
            if (!jobs.isEmpty) {
                val batch = firestore.batch()
                jobs.documents.forEach { batch.update(it.reference, "profileVisible", settings.profileVisible) }
                batch.commit().await()
            }
        }
            settings
        } catch (_: Exception) { null }
    }

    suspend fun createJob(job: CreateJobPostingDto): ProfileResponseDto? {
        return try {
            val document = db?.collection("jobs")?.document() ?: return null
            val visible = getUserSettings(job.companyId)?.profileVisible ?: true
            document.set(mapOf(
                "companyId" to job.companyId, "company" to job.company, "role" to job.role,
                "location" to job.location, "workAddress" to job.workAddress,
                "latitude" to job.latitude, "longitude" to job.longitude, "tags" to job.tags,
                "blurb" to job.blurb, "logoInitials" to job.logoInitials,
                "remoteType" to job.remoteType, "salaryRange" to job.salaryRange,
                "profileVisible" to visible,
                "createdAt" to FieldValue.serverTimestamp()
            )).await()
            ProfileResponseDto(document.id, "Job posting created successfully.", true)
        } catch (_: Exception) { null }
    }

    suspend fun deleteJob(id: String, companyId: String): ProfileResponseDto? {
        return try {
            val reference = db?.collection("jobs")?.document(id) ?: return null
            val snapshot = reference.get().await()
            if (snapshot.getString("companyId") != companyId) {
                ProfileResponseDto(id, "You do not own this job.", false)
            } else {
                reference.delete().await()
                ProfileResponseDto(id, "Job posting deleted.", true)
            }
        } catch (_: Exception) { null }
    }

    suspend fun updateStudent(id: String, student: CreateStudentDto): ProfileResponseDto? = try {
        db?.collection("students")?.document(id)?.set(mapOf(
            "userId" to id, "name" to student.name, "course" to student.course, "year" to student.year,
            "skills" to student.skills, "blurb" to student.blurb, "avatarInitials" to student.avatarInitials,
            "updatedAt" to FieldValue.serverTimestamp()
        ), SetOptions.merge())?.await()
        ProfileResponseDto(id, "Student profile updated successfully.", true)
    } catch (_: Exception) { null }

    suspend fun updateCompany(id: String, company: CreateCompanyDto): ProfileResponseDto? = try {
        db?.collection("companies")?.document(id)?.set(mapOf(
            "userId" to id, "name" to company.name, "industry" to company.industry, "location" to company.location,
            "description" to company.description, "logoInitials" to company.logoInitials,
            "hiringFor" to company.hiringFor,
            "updatedAt" to FieldValue.serverTimestamp()
        ), SetOptions.merge())?.await()
        ProfileResponseDto(id, "Company profile updated successfully.", true)
    } catch (_: Exception) { null }

    suspend fun getSwipedTargetIds(userId: String): Set<String> = try {
        db?.collection("swipes")?.whereEqualTo("userId", userId)?.get()?.await()?.documents
            ?.mapNotNull { it.getString("targetId") }
            ?.toSet().orEmpty()
    } catch (_: Exception) { emptySet() }

    suspend fun recordSwipe(userId: String, targetId: String, targetUserId: String, isLike: Boolean): SwipeResponse? {
        val firestore = db ?: return null
        return try {
            val safeId = "${userId}_$targetId".replace('/', '_')
            firestore.collection("swipes").document(safeId).set(mapOf(
                "userId" to userId, "targetId" to targetId, "targetUserId" to targetUserId,
                "isLike" to isLike, "createdAt" to FieldValue.serverTimestamp()
            ), SetOptions.merge()).await()
            if (userId == targetUserId) return SwipeResponse(false, null)

            val participants = listOf(userId, targetUserId).sorted()
            val pairId = participants.joinToString("__")
            val pairReference = firestore.collection("swipePairs").document(pairId)
            val hasActiveLike = firestore.collection("swipes").whereEqualTo("userId", userId).get().await()
                .documents.any {
                    it.getString("targetUserId") == targetUserId && it.getBoolean("isLike") == true
                }
            pairReference.set(mapOf(
                "participantIds" to participants,
                "likedBy" to if (hasActiveLike) FieldValue.arrayUnion(userId) else FieldValue.arrayRemove(userId),
                "updatedAt" to FieldValue.serverTimestamp()
            ), SetOptions.merge()).await()

            val reciprocalLike = firestore.collection("swipes")
                .whereEqualTo("targetUserId", userId).get().await().documents.any {
                    it.getString("userId") == targetUserId && it.getBoolean("isLike") == true
                }
            if (reciprocalLike) {
                pairReference.set(
                    mapOf("likedBy" to FieldValue.arrayUnion(targetUserId)),
                    SetOptions.merge()
                ).await()
            }

            if (!hasActiveLike) return SwipeResponse(false, null)
            val likedBy = (pairReference.get().await().get("likedBy") as? List<*>)
                ?.filterIsInstance<String>().orEmpty()
            if (!likedBy.containsAll(participants)) return SwipeResponse(false, null)

            val matchId = "match_$pairId"
            val matchReference = firestore.collection("matches").document(matchId)
            if (!matchReference.get().await().exists()) {
                matchReference.set(mapOf(
                    "id" to matchId, "pairId" to pairId, "mutual" to true,
                    "participantIds" to participants, "targetId" to targetId,
                    "lastMessage" to "You matched — start the conversation!",
                    "unreadBy" to listOf(targetUserId), "createdAt" to FieldValue.serverTimestamp()
                )).await()
                grantCvAccessForMatch(participants, matchId)
                participants.forEach { recipientId ->
                    val otherId = participants.first { it != recipientId }
                    val otherName = getStudentProfile(otherId)?.name
                        ?: getCompanyProfile(otherId)?.name
                        ?: "SwipeHire user"
                    createNotification(
                        userId = recipientId,
                        actorId = otherId,
                        type = AlertType.MATCH,
                        title = "New match",
                        body = "You matched with $otherName.",
                        matchId = matchId,
                        stableId = "${matchId}_$recipientId"
                    )
                }
            }
            SwipeResponse(true, matchId)
        } catch (_: Exception) { null }
    }

    private suspend fun grantCvAccessForMatch(participants: List<String>, matchId: String) {
        val firestore = db ?: return
        var studentId: String? = null
        for (participantId in participants) {
            if (firestore.collection("students").document(participantId).get().await().exists()) {
                studentId = participantId
                break
            }
        }
        val resolvedStudentId = studentId ?: return
        val companyId = participants.firstOrNull { it != resolvedStudentId } ?: return
        firestore.collection("cvAccess").document(resolvedStudentId).collection("companies").document(companyId).set(
            mapOf(
                "studentId" to resolvedStudentId,
                "companyId" to companyId,
                "matchId" to matchId,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteSwipe(userId: String, targetId: String, targetUserId: String): Boolean {
        val firestore = db ?: return false
        return try {
            val swipes = firestore.collection("swipes").whereEqualTo("userId", userId).get().await()
            swipes.documents.filter { it.getString("targetId") == targetId }.forEach { it.reference.delete().await() }

            if (userId != targetUserId) {
                val participants = listOf(userId, targetUserId).sorted()
                val pairId = participants.joinToString("__")
                val hasActiveLike = firestore.collection("swipes").whereEqualTo("userId", userId).get().await()
                    .documents.any {
                        it.getString("targetUserId") == targetUserId && it.getBoolean("isLike") == true
                    }
                firestore.collection("swipePairs").document(pairId).set(mapOf(
                    "participantIds" to participants,
                    "likedBy" to if (hasActiveLike) FieldValue.arrayUnion(userId) else FieldValue.arrayRemove(userId),
                    "updatedAt" to FieldValue.serverTimestamp()
                ), SetOptions.merge()).await()
            }
            true
        } catch (_: Exception) { false }
    }
}
