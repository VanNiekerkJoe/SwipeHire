package com.swipehire.app.util

import com.swipehire.app.data.AccountType
import com.swipehire.app.data.Interview
import com.swipehire.app.data.InterviewMode
import com.swipehire.app.data.InterviewStatus
import com.swipehire.app.data.PipelineStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PipelineUtilsTest {
    private val now = 1_000_000_000_000L
    private val hour = 60 * 60 * 1000L

    private fun interview(start: Long, minutes: Int = 30, status: InterviewStatus = InterviewStatus.ACCEPTED) =
        Interview("i1", "m1", "company", start, minutes, InterviewMode.VIDEO, "", status)

    @Test
    fun `company can move matched to interview but not straight to offer`() {
        assertTrue(PipelineRules.canMove(AccountType.COMPANY, PipelineStage.MATCHED, PipelineStage.INTERVIEW))
        assertFalse(PipelineRules.canMove(AccountType.COMPANY, PipelineStage.MATCHED, PipelineStage.OFFER))
    }

    @Test
    fun `only the student can mark an offer as hired`() {
        assertTrue(PipelineRules.canMove(AccountType.STUDENT, PipelineStage.OFFER, PipelineStage.HIRED))
        assertFalse(PipelineRules.canMove(AccountType.COMPANY, PipelineStage.OFFER, PipelineStage.HIRED))
    }

    @Test
    fun `terminal stages allow no actions for anyone`() {
        for (stage in listOf(PipelineStage.HIRED, PipelineStage.DECLINED)) {
            assertTrue(PipelineRules.actionsFor(AccountType.COMPANY, stage).isEmpty())
            assertTrue(PipelineRules.actionsFor(AccountType.STUDENT, stage).isEmpty())
        }
    }

    @Test
    fun `accepting an interview advances matched only`() {
        assertEquals(PipelineStage.INTERVIEW, PipelineRules.stageAfterInterviewAccepted(PipelineStage.MATCHED))
        assertEquals(PipelineStage.OFFER, PipelineRules.stageAfterInterviewAccepted(PipelineStage.OFFER))
    }

    @Test
    fun `scheduling is only allowed before an offer`() {
        assertTrue(PipelineRules.canSchedule(PipelineStage.MATCHED))
        assertFalse(PipelineRules.canSchedule(PipelineStage.OFFER))
        assertFalse(PipelineRules.canSchedule(PipelineStage.DECLINED))
    }

    @Test
    fun `unknown stage name falls back to matched`() {
        assertEquals(PipelineStage.MATCHED, PipelineStage.fromName(null))
        assertEquals(PipelineStage.MATCHED, PipelineStage.fromName("garbage"))
        assertEquals(PipelineStage.OFFER, PipelineStage.fromName("OFFER"))
    }

    @Test
    fun `slot in the past or under an hour away is rejected`() {
        assertNotNull(InterviewRules.validate(now - hour, now, 30, emptyList()))
        assertNotNull(InterviewRules.validate(now + hour / 2, now, 30, emptyList()))
    }

    @Test
    fun `valid slot is accepted`() {
        assertNull(InterviewRules.validate(now + 2 * hour, now, 30, emptyList()))
    }

    @Test
    fun `duration outside 15 to 120 minutes is rejected`() {
        assertNotNull(InterviewRules.validate(now + 2 * hour, now, 5, emptyList()))
        assertNotNull(InterviewRules.validate(now + 2 * hour, now, 180, emptyList()))
    }

    @Test
    fun `overlapping accepted interview is a conflict but back to back is not`() {
        val existing = listOf(interview(now + 2 * hour, 60))
        assertTrue(InterviewRules.conflicts(now + 2 * hour + hour / 2, 30, existing))
        assertFalse(InterviewRules.conflicts(now + 3 * hour, 30, existing))
    }

    @Test
    fun `cancelled and declined interviews never conflict`() {
        val existing = listOf(
            interview(now + 2 * hour, 60, InterviewStatus.CANCELLED),
            interview(now + 2 * hour, 60, InterviewStatus.DECLINED)
        )
        assertFalse(InterviewRules.conflicts(now + 2 * hour, 30, existing))
    }

    @Test
    fun `second proposal is blocked while one is still pending`() {
        val pending = listOf(interview(now + 5 * hour, 30, InterviewStatus.PROPOSED))
        assertNotNull(InterviewRules.validate(now + 8 * hour, now, 30, pending))
    }
}
