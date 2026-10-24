package com.swipehire.app.ui.components

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.Interview
import com.swipehire.app.data.InterviewMode
import com.swipehire.app.data.InterviewStatus
import com.swipehire.app.data.PipelineStage
import com.swipehire.app.ui.theme.Coral
import com.swipehire.app.ui.theme.Mint40
import com.swipehire.app.ui.theme.Violet40
import com.swipehire.app.ui.tr
import com.swipehire.app.util.PipelineRules
import com.swipehire.app.viewmodel.PipelineViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

private const val TAG = "PipelineUi"

private val timelineStages = listOf(PipelineStage.MATCHED, PipelineStage.INTERVIEW, PipelineStage.OFFER, PipelineStage.HIRED)

/** Colour used for a stage chip. */
private fun stageColor(stage: PipelineStage): Color = when (stage) {
    PipelineStage.MATCHED -> Violet40
    PipelineStage.INTERVIEW -> Violet40
    PipelineStage.OFFER -> Mint40
    PipelineStage.HIRED -> Mint40
    PipelineStage.DECLINED -> Coral
}

/** Small pill showing the current stage (used on the Matches list and the chat panel header). */
@Composable
fun StageChip(stage: PipelineStage, modifier: Modifier = Modifier) {
    val color = stageColor(stage)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            tr(stage.label),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Four-step timeline Matched -> Interview -> Offer -> Hired; a declined match shows a single red chip. */
@Composable
fun StageTimeline(stage: PipelineStage, modifier: Modifier = Modifier) {
    if (stage == PipelineStage.DECLINED) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            StageChip(stage)
        }
        return
    }
    val reached = PipelineRules.timelineIndex(stage)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        timelineStages.forEachIndexed { index, step ->
            val done = index <= reached
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.weight(1f).height(2.dp)
                            .background(if (index == 0) Color.Transparent else if (done) Violet40 else MaterialTheme.colorScheme.outlineVariant)
                    )
                    Box(
                        Modifier.size(22.dp).background(if (done) Violet40 else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Box(
                        Modifier.weight(1f).height(2.dp)
                            .background(if (index == timelineStages.lastIndex) Color.Transparent else if (index < reached) Violet40 else MaterialTheme.colorScheme.outlineVariant)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    tr(step.label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (index == reached) FontWeight.Bold else FontWeight.Normal,
                    color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun actionLabel(role: AccountType, target: PipelineStage): String = when (target) {
    PipelineStage.INTERVIEW -> "Move to interview"
    PipelineStage.OFFER -> "Make offer"
    PipelineStage.HIRED -> "Accept offer"
    PipelineStage.DECLINED -> if (role == AccountType.COMPANY) "Reject" else "Withdraw"
    PipelineStage.MATCHED -> "Matched"
}

private val slotFormat get() = SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault())

/** Opens the device calendar with the interview pre-filled. No calendar permission is needed. */
private fun addToCalendar(context: Context, interview: Interview, withName: String) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, "SwipeHire interview – $withName")
        putExtra(CalendarContract.Events.DESCRIPTION, interview.note)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, interview.startAtMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, interview.startAtMillis + interview.durationMinutes * 60_000L)
    }
    try {
        context.startActivity(intent)
        Log.d(TAG, "Opened calendar for interview ${interview.id}")
    } catch (e: Exception) {
        Log.w(TAG, "No calendar app available", e)
        Toast.makeText(context, "No calendar app found", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Collapsible "Application status" panel placed between the chat header and the
 * message list. Shows the stage timeline, role-based stage actions and interviews.
 */
@Composable
fun PipelinePanel(
    matchId: String,
    currentUserId: String?,
    otherName: String,
    viewModel: PipelineViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showSchedule by remember { mutableStateOf(false) }

    LaunchedEffect(matchId, currentUserId) {
        if (currentUserId != null) viewModel.start(matchId, currentUserId)
    }

    val pendingForMe = state.interviews.any { it.status == InterviewStatus.PROPOSED && it.proposedBy != currentUserId }
    // Surface an interview waiting on me without making the user hunt for it.
    LaunchedEffect(pendingForMe) { if (pendingForMe) expanded = true }

    val role = state.role
    val actions = if (role != null) PipelineRules.actionsFor(role, state.stage).toList().sortedBy { it.ordinal } else emptyList()
    val canSchedule = role == AccountType.COMPANY && PipelineRules.canSchedule(state.stage)

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr("Application status"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(10.dp))
                StageChip(state.stage)
                Spacer(Modifier.weight(1f))
                if (pendingForMe) Box(Modifier.size(8.dp).background(Coral, CircleShape))
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = tr("Application status"))
                }
            }

            if (expanded) {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    StageTimeline(state.stage, Modifier.padding(vertical = 6.dp))

                    if (actions.isNotEmpty() || canSchedule) {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (canSchedule) {
                                Button(onClick = { showSchedule = true }, enabled = !state.busy) {
                                    Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr("Schedule interview"))
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            actions.forEach { target ->
                                OutlinedButton(
                                    onClick = {
                                        if (currentUserId != null) viewModel.moveTo(matchId, currentUserId, target)
                                    },
                                    enabled = !state.busy && role != null
                                ) { Text(tr(actionLabel(role ?: AccountType.STUDENT, target))) }
                            }
                        }
                    }

                    state.error?.let {
                        Text(tr(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                    }

                    Text(
                        tr("Interviews"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                    if (state.interviews.isEmpty()) {
                        Text(tr("No interviews yet"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.interviews.forEach { interview ->
                            InterviewCard(
                                interview = interview,
                                currentUserId = currentUserId,
                                busy = state.busy,
                                onAccept = { currentUserId?.let { viewModel.respond(matchId, interview.id, it, true) } },
                                onDecline = { currentUserId?.let { viewModel.respond(matchId, interview.id, it, false) } },
                                onCancel = { currentUserId?.let { viewModel.cancel(matchId, interview.id, it) } },
                                onAddToCalendar = { addToCalendar(context, interview, otherName) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    if (showSchedule && currentUserId != null) {
        ScheduleInterviewDialog(
            onDismiss = { showSchedule = false },
            onSubmit = { start, minutes, mode, note, report ->
                viewModel.propose(matchId, currentUserId, start, minutes, mode, note) { error ->
                    if (error == null) showSchedule = false
                    report(error)
                }
            }
        )
    }
}

@Composable
private fun InterviewCard(
    interview: Interview,
    currentUserId: String?,
    busy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit,
    onAddToCalendar: () -> Unit
) {
    val mine = interview.proposedBy == currentUserId
    val statusColor = when (interview.status) {
        InterviewStatus.PROPOSED -> Violet40
        InterviewStatus.ACCEPTED -> Mint40
        InterviewStatus.DECLINED, InterviewStatus.CANCELLED -> Coral
    }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Event, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(slotFormat.format(Date(interview.startAtMillis)), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.14f)) {
                    Text(
                        tr(interview.status.name.lowercase().replaceFirstChar { it.uppercase() }),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }
            Text(
                "${tr(interview.mode.label)} · ${interview.durationMinutes} ${tr("minutes")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (interview.note.isNotBlank()) {
                Text(interview.note, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (interview.status) {
                    InterviewStatus.PROPOSED -> if (mine) {
                        TextButton(onClick = onCancel, enabled = !busy) { Text(tr("Cancel")) }
                    } else {
                        Button(onClick = onAccept, enabled = !busy) { Text(tr("Accept")) }
                        OutlinedButton(onClick = onDecline, enabled = !busy) { Text(tr("Decline")) }
                    }
                    InterviewStatus.ACCEPTED -> {
                        Button(onClick = onAddToCalendar) { Text(tr("Add to calendar")) }
                        if (mine) TextButton(onClick = onCancel, enabled = !busy) { Text(tr("Cancel")) }
                    }
                    else -> Unit
                }
            }
        }
    }
}

/**
 * Dialog for proposing an interview time. [onSubmit] gets the chosen slot plus a
 * callback it must call with the resulting error key (or null) so the dialog can show it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleInterviewDialog(
    onDismiss: () -> Unit,
    onSubmit: (Long, Int, InterviewMode, String, (String?) -> Unit) -> Unit
) {
    val dateState = rememberDatePickerState()
    val timeState = rememberTimePickerState(initialHour = 10, initialMinute = 0, is24Hour = true)
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var minutes by remember { mutableStateOf(30) }
    var mode by remember { mutableStateOf(InterviewMode.VIDEO) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var timePicked by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    val dateMillis = dateState.selectedDateMillis
    val dateLabel = dateMillis?.let {
        // The picker returns UTC midnight; format in UTC so the shown day matches the tapped day.
        SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(Date(it))
    } ?: tr("Pick a date")
    val timeLabel = if (timePicked) "%02d:%02d".format(timeState.hour, timeState.minute) else tr("Pick a time")

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text(tr("Schedule interview")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDate = true }) { Text(dateLabel) }
                    OutlinedButton(onClick = { showTime = true }) { Text(timeLabel) }
                }
                Text(tr("Duration"), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15, 30, 45, 60).forEach { option ->
                        FilterChip(selected = minutes == option, onClick = { minutes = option }, label = { Text("$option") })
                    }
                }
                Text(tr("Format"), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InterviewMode.entries.forEach { option ->
                        FilterChip(selected = mode == option, onClick = { mode = option }, label = { Text(tr(option.label)) })
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 200) note = it },
                    label = { Text(tr("Note (optional)")) },
                    supportingText = { Text("${note.length}/200") },
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(tr(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                enabled = !submitting,
                onClick = {
                    if (dateMillis == null) { error = "Pick a date first"; return@Button }
                    if (!timePicked) { error = "Pick a time first"; return@Button }
                    // Combine the picked calendar day with the picked clock time in the device time zone.
                    val day = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    val start = LocalDateTime.of(day, LocalTime.of(timeState.hour, timeState.minute))
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    submitting = true
                    error = null
                    onSubmit(start, minutes, mode, note) { result ->
                        submitting = false
                        error = result
                    }
                }
            ) { Text(tr("Send proposal")) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !submitting) { Text(tr("Cancel")) } }
    )

    if (showDate) {
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = { TextButton(onClick = { showDate = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(tr("Cancel")) } }
        ) { DatePicker(state = dateState) }
    }
    if (showTime) {
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = { TextButton(onClick = { timePicked = true; showTime = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(tr("Cancel")) } },
            text = { TimePicker(state = timeState) }
        )
    }
}
