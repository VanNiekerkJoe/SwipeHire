package com.swipehire.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.repository.AppRepository
import com.swipehire.app.ui.theme.Mint20
import com.swipehire.app.ui.theme.Mint40
import com.swipehire.app.ui.theme.Violet20
import com.swipehire.app.ui.theme.Violet40
import com.swipehire.app.ui.theme.glow
import kotlinx.coroutines.launch

@Composable
fun RoleSelectScreen(onRoleChosen: (AccountType) -> Unit) {
    val repository = remember { AppRepository() }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Who's\nswiping today?", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "You can switch this anytime from Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        RoleCard(
            title = "I'm a Student",
            subtitle = "Discover roles from companies that swipe on you",
            icon = Icons.Filled.School,
            gradient = Brush.linearGradient(listOf(Violet40, Violet20)),
            accent = Violet40,
            onClick = {
                scope.launch {
                    repository.getJobsFromApi()
                }
                onRoleChosen(AccountType.STUDENT)
            }
        )
        Spacer(Modifier.height(16.dp))
        RoleCard(
            title = "I'm Hiring",
            subtitle = "Swipe through student profiles to find your next grad",
            icon = Icons.Filled.Business,
            gradient = Brush.linearGradient(listOf(Mint40, Mint20)),
            accent = Mint40,
            onClick = {
                scope.launch {
                    repository.getStudentsFromApi()
                }
                onRoleChosen(AccountType.COMPANY)
            }
        )
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
    ) {
        Row(
            Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .glow(accent, radiusMultiplier = 2.2f, alpha = 0.35f)
                    .clip(CircleShape)
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = accent)
        }
    }
}