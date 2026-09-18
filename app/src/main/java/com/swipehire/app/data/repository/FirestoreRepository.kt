package com.swipehire.app.data.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.swipehire.app.data.ChatMessage
import com.swipehire.app.data.JobPosting
import com.swipehire.app.data.StudentProfile
import com.swipehire.app.data.remote.ChatMessageDto
import com.swipehire.app.data.remote.JobPostingDto
import com.swipehire.app.data.remote.StudentProfileDto
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
}