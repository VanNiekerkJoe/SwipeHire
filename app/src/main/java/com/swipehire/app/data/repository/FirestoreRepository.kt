package com.swipehire.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.swipehire.app.data.ChatMessage
import com.swipehire.app.data.JobPosting
import com.swipehire.app.data.StudentProfile
import com.swipehire.app.data.remote.ChatMessageDto
import com.swipehire.app.data.remote.JobPostingDto
import com.swipehire.app.data.remote.StudentProfileDto
import com.swipehire.app.data.remote.SavedItemsDto
import com.swipehire.app.data.remote.UserSettingsDto
import com.swipehire.app.data.remote.CreateJobPostingDto
import com.swipehire.app.data.remote.CreateStudentDto
import com.swipehire.app.data.remote.CreateCompanyDto
import com.swipehire.app.data.remote.ProfileResponseDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    // Safely obtain instance without crashing if initialization was delayed
    private val db: FirebaseFirestore?
        get() {
            return try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
        }

    // Fetch Job Postings from Firestore
    suspend fun getJobPostings(): List<JobPosting> {
        val firestore = db ?: return emptyList()
        return try {
            val snapshot = firestore.collection("jobs").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(JobPostingDto::class.java)?.toDomain()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Fetch Student Profiles from Firestore
    suspend fun getStudentProfiles(): List<StudentProfile> {
        val firestore = db ?: return emptyList()
        return try {
            val snapshot = firestore.collection("students").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(StudentProfileDto::class.java)?.toDomain()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Real-time listener for Chat Messages under a specific match subcollection
    fun getLiveMessages(matchId: String, currentUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("matches")
            .document(matchId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val dto = doc.toObject(ChatMessageDto::class.java)
                    dto?.let {
                        ChatMessage(
                            id = it.id,
                            text = it.text,
                            fromMe = it.senderId == currentUserId
                        )
                    }
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    // Send a message to a match chat
    suspend fun sendMessage(matchId: String, senderId: String, text: String): Boolean {
        val firestore = db ?: return false
        return try {
            val msg = hashMapOf(
                "senderId" to senderId,
                "text" to text,
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("matches")
                .document(matchId)
                .collection("messages")
                .add(msg)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getSavedItems(userId: String): SavedItemsDto? {
        val firestore = db ?: return null
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            SavedItemsDto(
                savedJobIds = (snapshot.get("savedJobIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                savedStudentIds = (snapshot.get("savedStudentIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setSavedItem(userId: String, kind: String, itemId: String, saved: Boolean): SavedItemsDto? {
        val firestore = db ?: return null
        return try {
            val field = if (kind == "job") "savedJobIds" else "savedStudentIds"
            val update = if (saved) FieldValue.arrayUnion(itemId) else FieldValue.arrayRemove(itemId)
            firestore.collection("users").document(userId)
                .set(mapOf(field to update, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()
            getSavedItems(userId)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserSettings(userId: String): UserSettingsDto? {
        val firestore = db ?: return null
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            val settings = snapshot.get("settings") as? Map<*, *> ?: emptyMap<Any, Any>()
            UserSettingsDto(
                pushNotifications = settings["pushNotifications"] as? Boolean ?: true,
                matchAlerts = settings["matchAlerts"] as? Boolean ?: true,
                messageAlerts = settings["messageAlerts"] as? Boolean ?: true,
                profileVisible = settings["profileVisible"] as? Boolean ?: true
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserSettings(userId: String, settings: UserSettingsDto): UserSettingsDto? {
        val firestore = db ?: return null
        return try {
            val values = mapOf(
                "pushNotifications" to settings.pushNotifications,
                "matchAlerts" to settings.matchAlerts,
                "messageAlerts" to settings.messageAlerts,
                "profileVisible" to settings.profileVisible
            )
            firestore.collection("users").document(userId)
                .set(mapOf("settings" to values, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()
            settings
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createJob(job: CreateJobPostingDto): ProfileResponseDto? {
        val firestore = db ?: return null
        return try {
            val document = firestore.collection("jobs").document()
            document.set(
                mapOf(
                    "company" to job.company, "role" to job.role, "location" to job.location,
                    "workAddress" to job.workAddress, "latitude" to job.latitude, "longitude" to job.longitude,
                    "tags" to job.tags, "blurb" to job.blurb, "logoInitials" to job.logoInitials,
                    "remoteType" to job.remoteType, "salaryRange" to job.salaryRange,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            ProfileResponseDto(document.id, "Job posting created successfully.", true)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteJob(id: String): ProfileResponseDto? {
        val firestore = db ?: return null
        return try {
            firestore.collection("jobs").document(id).delete().await()
            ProfileResponseDto(id, "Job posting deleted.", true)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateStudent(id: String, student: CreateStudentDto): ProfileResponseDto? {
        val firestore = db ?: return null
        return try {
            firestore.collection("students").document(id).set(
                mapOf(
                    "name" to student.name, "course" to student.course, "year" to student.year,
                    "skills" to student.skills, "blurb" to student.blurb,
                    "avatarInitials" to student.avatarInitials, "updatedAt" to FieldValue.serverTimestamp()
                ), SetOptions.merge()
            ).await()
            ProfileResponseDto(id, "Student profile updated successfully.", true)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createStudent(student: CreateStudentDto): ProfileResponseDto? {
        val firestore = db ?: return null
        val id = firestore.collection("students").document().id
        return updateStudent(id, student)?.copy(message = "Student profile created successfully.")
    }

    suspend fun updateCompany(id: String, company: CreateCompanyDto): ProfileResponseDto? {
        val firestore = db ?: return null
        return try {
            firestore.collection("companies").document(id).set(
                mapOf(
                    "name" to company.name, "industry" to company.industry, "location" to company.location,
                    "description" to company.description, "logoInitials" to company.logoInitials,
                    "updatedAt" to FieldValue.serverTimestamp()
                ), SetOptions.merge()
            ).await()
            ProfileResponseDto(id, "Company profile updated successfully.", true)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createCompany(company: CreateCompanyDto): ProfileResponseDto? {
        val firestore = db ?: return null
        val id = firestore.collection("companies").document().id
        return updateCompany(id, company)?.copy(message = "Company profile created successfully.")
    }

    suspend fun recordSwipe(userId: String, targetId: String, isLike: Boolean): Boolean {
        val firestore = db ?: return false
        return try {
            val safeId = "${userId}_$targetId".replace('/', '_')
            firestore.collection("swipes").document(safeId).set(
                mapOf(
                    "userId" to userId, "targetId" to targetId, "isLike" to isLike,
                    "createdAt" to FieldValue.serverTimestamp()
                ), SetOptions.merge()
            ).await()
            if (isLike) {
                val matchId = "m_$safeId"
                firestore.collection("matches").document(matchId).set(
                    mapOf(
                        "id" to matchId, "participantIds" to listOf(userId, targetId),
                        "targetId" to targetId, "lastMessage" to "You matched - start the conversation!",
                        "unread" to true, "createdAt" to FieldValue.serverTimestamp()
                    ), SetOptions.merge()
                ).await()
            }
            isLike
        } catch (e: Exception) {
            false
        }
    }
}