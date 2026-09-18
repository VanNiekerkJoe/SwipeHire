package com.swipehire.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.swipehire.app.data.MockData
import kotlinx.coroutines.tasks.await

object FirestoreSeeder {

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    suspend fun seedDatabase(): Boolean {
        var successCount = 0

        // 1. Seed Jobs (j1 through j5)
        val jobsCollection = db.collection("jobs")
        for (job in MockData.jobPostings) {
            try {
                val dto = JobPostingDto(
                    id = job.id,
                    company = job.company,
                    role = job.role,
                    location = job.location,
                    workAddress = job.workAddress,
                    latitude = job.latitude,
                    longitude = job.longitude,
                    tags = job.tags,
                    blurb = job.blurb,
                    logoInitials = job.logoInitials,
                    remoteType = job.remoteType.name,
                    salaryRange = job.salaryRange
                )
                jobsCollection.document(job.id).set(dto).await()
                successCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Seed Student Profiles (s1 through s4)
        val studentsCollection = db.collection("students")
        for (student in MockData.studentProfiles) {
            try {
                val dto = StudentProfileDto(
                    id = student.id,
                    name = student.name,
                    course = student.course,
                    year = student.year,
                    skills = student.skills,
                    blurb = student.blurb,
                    avatarInitials = student.avatarInitials
                )
                studentsCollection.document(student.id).set(dto).await()
                successCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Seed Matches and Nested Chat Messages (m1, m2, m3)
        val matchesCollection = db.collection("matches")
        for (match in MockData.matches) {
            try {
                val matchData = hashMapOf(
                    "id" to match.id,
                    "name" to match.name,
                    "subtitle" to match.subtitle,
                    "avatarInitials" to match.avatarInitials,
                    "lastMessage" to match.lastMessage,
                    "unread" to match.unread
                )
                // Write parent match document
                matchesCollection.document(match.id).set(matchData).await()

                // Write child messages under messages subcollection
                val messagesCollection = matchesCollection.document(match.id).collection("messages")
                for (msg in match.messages) {
                    val msgData = hashMapOf(
                        "id" to msg.id,
                        "text" to msg.text,
                        "senderId" to if (msg.fromMe) "me" else "other",
                        "timestamp" to com.google.firebase.Timestamp.now()
                    )
                    messagesCollection.document(msg.id).set(msgData).await()
                }
                successCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return successCount > 0
    }
}