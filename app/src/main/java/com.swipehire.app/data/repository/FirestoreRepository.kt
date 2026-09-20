package com.swipehire.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.ChatMessage
import com.swipehire.app.data.CompanyProfile
import com.swipehire.app.data.JobPosting
import com.swipehire.app.data.MatchChat
import com.swipehire.app.data.StudentProfile
import com.swipehire.app.data.remote.ChatMessageDto
import com.swipehire.app.data.remote.CompanyProfileDto
import com.swipehire.app.data.remote.CreateCompanyDto
import com.swipehire.app.data.remote.CreateJobPostingDto
import com.swipehire.app.data.remote.CreateStudentDto
import com.swipehire.app.data.remote.JobPostingDto
import com.swipehire.app.data.remote.MatchDto
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
            ?.mapNotNull { it.toObject(JobPostingDto::class.java)?.toDomain() }
            ?.filter { it.companyId.isNotBlank() }.orEmpty()
    } catch (_: Exception) { emptyList() }

    suspend fun getCompanyJobs(companyId: String): List<JobPosting> = try {
        db?.collection("jobs")?.whereEqualTo("companyId", companyId)?.get()?.await()?.documents
            ?.mapNotNull { it.toObject(JobPostingDto::class.java)?.toDomain() }.orEmpty()
    } catch (_: Exception) { emptyList() }

    suspend fun getStudentProfiles(): List<StudentProfile> = try {
        db?.collection("students")?.get()?.await()?.documents
            ?.filter { it.getString("userId") == it.id }
            ?.mapNotNull { it.toObject(StudentProfileDto::class.java)?.toDomain() }.orEmpty()
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
            unread = userId in dto.unreadBy
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
                trySend(snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessageDto::class.java)?.let {
                        ChatMessage(it.id, it.text, it.senderId == currentUserId)
                    }
                }.orEmpty())
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
                mapOf("senderId" to senderId, "text" to text.trim(), "timestamp" to Timestamp.now())
            ).await()
            val recipients = participants.filter { it != senderId }
            matchRef.set(
                mapOf("lastMessage" to text.trim(), "unreadBy" to recipients, "updatedAt" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            ).await()
            true
        } catch (_: Exception) { false }
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
                settings["profileVisible"] as? Boolean ?: true
            )
        } catch (_: Exception) { null }
    }

    suspend fun updateUserSettings(userId: String, settings: UserSettingsDto): UserSettingsDto? = try {
        val values = mapOf(
            "pushNotifications" to settings.pushNotifications,
            "matchAlerts" to settings.matchAlerts,
            "messageAlerts" to settings.messageAlerts,
            "profileVisible" to settings.profileVisible
        )
        db?.collection("users")?.document(userId)?.set(
            mapOf("settings" to values, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge()
        )?.await()
        settings
    } catch (_: Exception) { null }

    suspend fun createJob(job: CreateJobPostingDto): ProfileResponseDto? {
        return try {
            val document = db?.collection("jobs")?.document() ?: return null
            document.set(mapOf(
                "companyId" to job.companyId, "company" to job.company, "role" to job.role,
                "location" to job.location, "workAddress" to job.workAddress,
                "latitude" to job.latitude, "longitude" to job.longitude, "tags" to job.tags,
                "blurb" to job.blurb, "logoInitials" to job.logoInitials,
                "remoteType" to job.remoteType, "salaryRange" to job.salaryRange,
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
            }
            SwipeResponse(true, matchId)
        } catch (_: Exception) { null }
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
