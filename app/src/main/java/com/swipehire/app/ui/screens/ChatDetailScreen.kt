package com.swipehire.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.swipehire.app.data.ChatMessage
import com.swipehire.app.data.MockData
import com.swipehire.app.ui.theme.Violet40
import com.swipehire.app.ui.theme.glow

@Composable
fun ChatDetailScreen(matchId: String, onBack: () -> Unit) {
    val match = MockData.matches.find { it.id == matchId } ?: return
    var input by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(match.messages) }

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
                    Text(match.name, style = MaterialTheme.typography.titleMedium)
                    Text(match.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message -> MessageBubble(message) }
        }

        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message...") },
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
                        messages = messages + ChatMessage(
                            id = (messages.size + 1).toString(),
                            text = input,
                            fromMe = true
                        )
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
            Text(
                message.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (message.fromMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
