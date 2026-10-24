package com.swipehire.app.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.AlertType
import com.swipehire.app.data.Interview
import com.swipehire.app.data.InterviewMode
import com.swipehire.app.data.InterviewStatus
import com.swipehire.app.data.PipelineStage
import com.swipehire.app.util.PipelineRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Firestore access for the interview scheduler (feature 4) and the application
 * pipeline tracker (feature 5).
 *
 * Data layout:
 *   matches/{matchId}.stage, .stageUpdatedAt            current pipeline stage
 *   matches/{matchId}/interviews/{interviewId}          one document per proposal
 *   notifications/{id}                                  alert for the other participant
 *
 * Every write also creates a `notifications` document, so the real-time push
 * (FCM) work picks interview and stage changes up with no extra wiring.
 */
class PipelineRepository {

    private companion object {
        const val TAG = "PipelineRepo"
    }

    private val db: FirebaseFirestore?
        get() = try { FirebaseFirestore.getInstance() } catch (e: Exception) {
            Log.e(TAG, "Firestore unavailable", e); null
        }

    /** Reads users/{uid}.role; null when the user has not chosen a role yet. */
    suspend fun getRole(userId: String): AccountType? = try {
        db?.collection("users")?.document(userId)?.get()?.await()?.getString("role")
            ?.let { runCatching { AccountType.valueOf(it) }.getOrNull() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "getRole failed", e); null
    }

    /** Live pipeline stage of one match. Emits MATCHED until the field exists. */
    fun observeStage(matchId: String): Flow<PipelineStage> = callbackFlow {
        val firestore = db
        if (firestore == null) { close(); return@callbackFlow }
        val listener = firestore.collection("matches").document(matchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "stage listener failed for $matchId", error)
                    return@addSnapshotListener
                }
                trySend(PipelineStage.fromName(snapshot?.getString("stage")))
            }
        awaitClose { listener.remove() }
    }

    /** Live list of interviews for one match, soonest first. */
    fun observeInterviews(matchId: String): Flow<List<Interview>> = callbackFlow {
        val firestore = db
        if (firestore == null) { trySend(emptyList()); close(); return@callbackFlow }
        val listener = firestore.collection("matches").document(matchId)
            .collection("interviews").orderBy("startAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "interview listener failed for $matchId", error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toInterview(matchId) })
            }
        awaitClose { listener.remove() }
    }

    private fun DocumentSnapshot.toInterview(matchId: String): Interview? {
        val start = getTimestamp("startAt") ?: return null // malformed record: skip it
        return Interview(
            id = id,
            matchId = matchId,
            proposedBy = getString("proposedBy").orEmpty(),
            startAtMillis = start.toDate().time,
            durationMinutes = (getLong("durationMinutes") ?: 30L).toInt(),
            mode = InterviewMode.fromName(getString("mode")),
            note = getString("note").orEmpty(),
            status = InterviewStatus.entries.firstOrNull { it.name == getString("status") }
                ?: InterviewStatus.PROPOSED
        )
    }

    suspend fun proposeInterview(
        matchId: String, proposerId: String, startAtMillis: Long,
        durationMinutes: Int, mode: InterviewMode, note: String
    ): Boolean {
        val firestore = db ?: return false
        return try {
            val matchRef = firestore.collection("matches").document(matchId)
            val other = otherParticipant(matchRef.get().await(), proposerId) ?: return false
            matchRef.collection("interviews").add(
                mapOf(
                    "proposedBy" to proposerId,
                    "startAt" to Timestamp(Date(startAtMillis)),
                    "durationMinutes" to durationMinutes,
                    "mode" to mode.name,
                    "note" to note.trim().take(200),
                    "status" to InterviewStatus.PROPOSED.name,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            notify(other, proposerId, AlertType.INTERVIEW, "Interview proposed",
                describeSlot(startAtMillis, mode), matchId)
            Log.d(TAG, "Interview proposed on match $matchId")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "proposeInterview failed", e); false
        }
    }

    /** The non-proposer accepts or declines. Accepting also advances a fresh match to INTERVIEW. */
    suspend fun respondToInterview(matchId: String, interviewId: String, responderId: String, accept: Boolean): Boolean {
        val firestore = db ?: return false
        return try {
            val matchRef = firestore.collection("matches").document(matchId)
            val ref = matchRef.collection("interviews").document(interviewId)
            val snapshot = ref.get().await()
            val interview = snapshot.toInterview(matchId) ?: return false
            if (interview.proposedBy == responderId || interview.status != InterviewStatus.PROPOSED) {
                Log.w(TAG, "respondToInterview ignored: not allowed in current state")
                return false
            }
            val newStatus = if (accept) InterviewStatus.ACCEPTED else InterviewStatus.DECLINED
            ref.update(mapOf("status" to newStatus.name, "respondedAt" to FieldValue.serverTimestamp())).await()
            if (accept) {
                val current = PipelineStage.fromName(matchRef.get().await().getString("stage"))
                val next = PipelineRules.stageAfterInterviewAccepted(current)
                if (next != current) {
                    matchRef.update(mapOf("stage" to next.name, "stageUpdatedAt" to FieldValue.serverTimestamp())).await()
                    Log.d(TAG, "Match $matchId advanced to $next after accepted interview")
                }
            }
            notify(interview.proposedBy, responderId, AlertType.INTERVIEW,
                if (accept) "Interview accepted" else "Interview declined",
                describeSlot(interview.startAtMillis, interview.mode), matchId)
            Log.d(TAG, "Interview $interviewId -> $newStatus")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "respondToInterview failed", e); false
        }
    }

    /** Only the proposer may cancel, and only while the proposal is still pending or accepted. */
    suspend fun cancelInterview(matchId: String, interviewId: String, actorId: String): Boolean {
        val firestore = db ?: return false
        return try {
            val matchRef = firestore.collection("matches").document(matchId)
            val ref = matchRef.collection("interviews").document(interviewId)
            val interview = ref.get().await().toInterview(matchId) ?: return false
            if (interview.proposedBy != actorId ||
                (interview.status != InterviewStatus.PROPOSED && interview.status != InterviewStatus.ACCEPTED)
            ) {
                Log.w(TAG, "cancelInterview ignored: not allowed in current state")
                return false
            }
            ref.update(mapOf("status" to InterviewStatus.CANCELLED.name, "respondedAt" to FieldValue.serverTimestamp())).await()
            otherParticipant(matchRef.get().await(), actorId)?.let {
                notify(it, actorId, AlertType.INTERVIEW, "Interview cancelled",
                    describeSlot(interview.startAtMillis, interview.mode), matchId)
            }
            Log.d(TAG, "Interview $interviewId cancelled")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "cancelInterview failed", e); false
        }
    }

    /** Moves the match to [newStage] if [role] is allowed to make that transition from the stored stage. */
    suspend fun updateStage(matchId: String, actorId: String, role: AccountType, newStage: PipelineStage): Boolean {
        val firestore = db ?: return false
        return try {
            val matchRef = firestore.collection("matches").document(matchId)
            val snapshot = matchRef.get().await()
            val current = PipelineStage.fromName(snapshot.getString("stage"))
            if (!PipelineRules.canMove(role, current, newStage)) {
                Log.w(TAG, "Blocked stage change $current -> $newStage for $role")
                return false
            }
            matchRef.update(mapOf("stage" to newStage.name, "stageUpdatedAt" to FieldValue.serverTimestamp())).await()
            otherParticipant(snapshot, actorId)?.let {
                notify(it, actorId, AlertType.STAGE, "Application status updated", newStage.label, matchId)
            }
            Log.d(TAG, "Match $matchId stage $current -> $newStage by $role")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "updateStage failed", e); false
        }
    }

    private fun otherParticipant(match: DocumentSnapshot, me: String): String? =
        (match.get("participantIds") as? List<*>)?.filterIsInstance<String>()?.firstOrNull { it != me }

    private fun describeSlot(startAtMillis: Long, mode: InterviewMode): String =
        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(startAtMillis)) + " · " + mode.label

    /** Same document shape as FirestoreRepository.createNotification so AlertsViewModel reads it unchanged. */
    private suspend fun notify(
        userId: String, actorId: String, type: AlertType, title: String, body: String, matchId: String
    ) {
        val firestore = db ?: return
        firestore.collection("notifications").add(
            mapOf(
                "userId" to userId,
                "actorId" to actorId,
                "type" to type.name,
                "title" to title,
                "body" to body,
                "matchId" to matchId,
                "createdAt" to FieldValue.serverTimestamp(),
                "read" to false
            )
        ).await()
    }
}
