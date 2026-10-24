package com.swipehire.app.data

/**
 * Hiring pipeline stages for a mutual match (User Defined Feature 5).
 * The current stage is stored on the match document as `stage` (enum name).
 * [label] is the English display text and doubles as the key passed to `tr()`.
 */
enum class PipelineStage(val label: String) {
    MATCHED("Matched"),
    INTERVIEW("Interview"),
    OFFER("Offer"),
    HIRED("Hired"),
    DECLINED("Declined");

    /** HIRED and DECLINED are final: nothing can move a match out of them. */
    val isTerminal: Boolean get() = this == HIRED || this == DECLINED

    companion object {
        /** Unknown or missing values (older match documents) fall back to MATCHED. */
        fun fromName(name: String?): PipelineStage = entries.firstOrNull { it.name == name } ?: MATCHED
    }
}

/** Lifecycle of a single interview proposal (User Defined Feature 4). */
enum class InterviewStatus { PROPOSED, ACCEPTED, DECLINED, CANCELLED }

enum class InterviewMode(val label: String) {
    IN_PERSON("In person"),
    VIDEO("Video call"),
    PHONE("Phone call");

    companion object {
        fun fromName(name: String?): InterviewMode = entries.firstOrNull { it.name == name } ?: VIDEO
    }
}

/** Stored at matches/{matchId}/interviews/{interviewId}. */
data class Interview(
    val id: String,
    val matchId: String,
    /** Firebase UID of whoever proposed the slot; the other participant responds. */
    val proposedBy: String,
    val startAtMillis: Long,
    val durationMinutes: Int,
    val mode: InterviewMode,
    val note: String,
    val status: InterviewStatus
)
