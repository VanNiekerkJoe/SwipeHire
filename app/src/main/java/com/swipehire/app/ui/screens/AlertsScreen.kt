package com.swipehire.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipehire.app.data.AlertType
import com.swipehire.app.data.AppAlert
import com.swipehire.app.ui.theme.Coral
import com.swipehire.app.ui.theme.Violet40
import com.swipehire.app.ui.tr
import com.swipehire.app.viewmodel.AlertsViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun AlertsScreen(
    notificationsEnabled: Boolean,
    matchAlertsEnabled: Boolean,
    messageAlertsEnabled: Boolean,
    onOpenChat: (String) -> Unit,
    onOpenMatches: () -> Unit,
    viewModel: AlertsViewModel = viewModel()
) {
    val alerts by viewModel.alerts.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }

    val visibleAlerts = if (!notificationsEnabled) emptyList() else alerts.filter { alert ->
        when (alert.type) {
            AlertType.MATCH -> matchAlertsEnabled
            AlertType.MESSAGE -> messageAlertsEnabled
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(tr("Alerts"), style = MaterialTheme.typography.headlineSmall)
        Text(
            tr("Matches and messages that need your attention."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(16.dp))

        if (!notificationsEnabled) {
            EmptyAlerts(tr("Alerts are switched off in Settings."))
        } else if (visibleAlerts.isEmpty()) {
            EmptyAlerts(tr("You have no new alerts."))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visibleAlerts, key = { it.id }) { alert ->
                    AlertCard(alert) {
                        viewModel.markRead(alert.id)
                        if (alert.type == AlertType.MESSAGE && alert.matchId.isNotBlank()) {
                            onOpenChat(alert.matchId)
                        } else {
                            onOpenMatches()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: AppAlert, onClick: () -> Unit) {
    val displayTitle = when {
        alert.type == AlertType.MATCH -> tr("New match")
        alert.title.startsWith("New message from ") ->
            "${tr("New message from")} ${alert.title.removePrefix("New message from ")}"
        else -> alert.title
    }
    val displayBody = if (alert.type == AlertType.MATCH && alert.body.startsWith("You matched with ")) {
        "${tr("You matched with")} ${alert.body.removePrefix("You matched with ")}"
    } else alert.body
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isRead) MaterialTheme.colorScheme.surface
            else Violet40.copy(alpha = 0.10f)
        )
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(Violet40.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (alert.type == AlertType.MATCH) Icons.Filled.Favorite else Icons.Filled.ChatBubble,
                    contentDescription = null,
                    tint = if (alert.type == AlertType.MATCH) Coral else Violet40
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(displayTitle, fontWeight = if (alert.isRead) FontWeight.Medium else FontWeight.Bold)
                Text(displayBody, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(alert.createdAtMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!alert.isRead) Box(Modifier.size(8.dp).background(Coral, CircleShape))
        }
    }
}

@Composable
private fun EmptyAlerts(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray)
    }
}
