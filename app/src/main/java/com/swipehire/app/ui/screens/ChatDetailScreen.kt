package com.swipehire.app.ui.screens

import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipehire.app.data.ChatMessage
import com.swipehire.app.data.MessageDeliveryState
import com.swipehire.app.data.currentFirebaseUserId
import com.swipehire.app.data.repository.AppRepository
import com.swipehire.app.ui.theme.Violet40
import com.swipehire.app.ui.theme.glow
import com.swipehire.app.ui.tr
import com.swipehire.app.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatDetailScreen(
    matchId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val currentUserId = currentFirebaseUserId()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AppRepository() }
    var input by remember { mutableStateOf("") }

    // Observe real-time messages
    val liveMessages by viewModel.messages.collectAsState()
    val match by viewModel.match.collectAsState()
    val matchResolved by viewModel.matchResolved.collectAsState()

    LaunchedEffect(matchId, currentUserId) {
        if (currentUserId != null) viewModel.observeMessages(matchId, currentUserId)
    }

    val displayMessages = liveMessages

    if (!matchResolved) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (match == null) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(tr("Chat unavailable"), style = MaterialTheme.typography.titleMedium)
            }
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    tr("Messaging becomes available only after both accounts like each other."),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.dp, Color.Transparent),
            shadowElevation = 2.dp
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(match?.name ?: "Chat", style = MaterialTheme.typography.titleMedium)
                    Text(match?.subtitle ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                if (!match?.studentCvPath.isNullOrBlank()) {
                    TextButton(onClick = {
                        val cvPath = match?.studentCvPath.orEmpty()
                        scope.launch {
                            repository.getCvDownloadUrl(cvPath)?.let { url ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                    }) { Text(tr("View CV")) }
                }
            }
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayMessages) { message -> MessageBubble(message) }
        }

        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(tr("Message...")) },
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Violet40,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            Spacer(Modifier.width(10.dp))
            IconButton(
                onClick = {
                    if (input.isNotBlank()) {
                        currentUserId?.let { viewModel.sendMessage(matchId, it, input) }
                        input = ""
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .glow(Violet40, radiusMultiplier = 2.2f, alpha = 0.4f)
                    .clip(CircleShape)
                    .background(Violet40)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (message.fromMe) 18.dp else 4.dp,
                bottomEnd = if (message.fromMe) 4.dp else 18.dp
            ),
            color = if (message.fromMe) Violet40 else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(
                    message.text,
                    color = if (message.fromMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.sentAtMillis))
                val state = when (message.deliveryState) {
                    MessageDeliveryState.SENDING -> tr("Sending")
                    MessageDeliveryState.SENT -> tr("Sent")
                    MessageDeliveryState.READ -> tr("Read")
                }
                Text(
                    if (message.fromMe) "$time · $state" else time,
                    color = if (message.fromMe) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
