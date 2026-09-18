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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.MockData
import com.swipehire.app.data.StudentProfile
import com.swipehire.app.data.remote.CreateCompanyDto
import com.swipehire.app.data.remote.CreateStudentDto
import com.swipehire.app.data.repository.AppRepository
import com.swipehire.app.ui.theme.Mint20
import com.swipehire.app.ui.theme.Mint40
import com.swipehire.app.ui.theme.Violet20
import com.swipehire.app.ui.theme.Violet40
import com.swipehire.app.ui.theme.glow
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    accountType: AccountType,
    onAccountTypeChange: (AccountType) -> Unit = {}
) {
    val repository = remember { AppRepository() }
    val scope = rememberCoroutineScope()
    var apiStudentProfile by remember { mutableStateOf<StudentProfile?>(null) }

    // Fetch initial profile from C# API
    LaunchedEffect(accountType) {
        if (accountType == AccountType.STUDENT) {
            val apiStudents = repository.getStudentsFromApi()
            if (apiStudents.isNotEmpty()) {
                apiStudentProfile = apiStudents.first()
            }
        }
    }

    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profile", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(
                onClick = {
                    scope.launch {
                        if (accountType == AccountType.STUDENT) {
                            // Call PUT /api/students/{id} via C# REST API
                            repository.updateStudent(
                                id = apiStudentProfile?.id ?: "s1",
                                dto = CreateStudentDto(
                                    name = apiStudentProfile?.name ?: "Student User",
                                    course = apiStudentProfile?.course ?: "Computer Science",
                                    year = apiStudentProfile?.year ?: "Final year",
                                    skills = apiStudentProfile?.skills ?: listOf("Kotlin", "Android", "REST APIs"),
                                    blurb = apiStudentProfile?.blurb ?: "Passionate software developer looking for opportunities.",
                                    avatarInitials = apiStudentProfile?.avatarInitials ?: "ST"
                                )
                            )
                        } else {
                            // Call PUT /api/companies/{id} via C# REST API
                            repository.updateCompany(
                                id = "c1",
                                dto = CreateCompanyDto(
                                    name = "Nexora Fintech",
                                    industry = "Fintech",
                                    location = "Sandton, Johannesburg",
                                    description = "We build secure payment infrastructure.",
                                    logoInitials = "NX"
                                )
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("Edit")
            }
        }
        Spacer(Modifier.height(18.dp))

        ModeSwitcher(accountType = accountType, onAccountTypeChange = onAccountTypeChange)
        Spacer(Modifier.height(24.dp))

        val accent = if (accountType == AccountType.STUDENT) Violet40 else Mint40
        val accentDeep = if (accountType == AccountType.STUDENT) Violet20 else Mint20
        val student = apiStudentProfile ?: MockData.studentProfiles.firstOrNull()

        val name = if (accountType == AccountType.STUDENT) student?.name ?: "Student User" else "Nexora Fintech"
        val subtitle = if (accountType == AccountType.STUDENT)
            "${student?.course ?: "Computer Science"} · ${student?.year ?: "Final year"}"
        else "Fintech · Johannesburg"
        val initials = if (accountType == AccountType.STUDENT) student?.avatarInitials ?: "ST" else "NX"

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(76.dp)
                    .glow(accent, radiusMultiplier = 2.4f, alpha = 0.4f)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accent, accentDeep))),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(28.dp))
        Text("About", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            if (accountType == AccountType.STUDENT) student?.blurb ?: "Passionate software developer looking for opportunities."
            else "We build secure, high-throughput payment infrastructure for the South African market.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(26.dp))
        Text(
            if (accountType == AccountType.STUDENT) "Skills" else "Hiring for",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(10.dp))
        val tags = if (accountType == AccountType.STUDENT) student?.skills ?: listOf("Kotlin", "Android", "REST APIs")
        else listOf("Backend", "Cloud", "Security")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags) { tag ->
                Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.12f)) {
                    Text(tag, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = accent, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        Text("Application activity", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = if (accountType == AccountType.STUDENT) "Applications" else "Candidates viewed",
                value = "18",
                accent = accent,
                modifier = Modifier.weight(1f)
            )
            StatTile(label = "Matches", value = MockData.matches.size.toString(), accent = accent, modifier = Modifier.weight(1f))
            StatTile(label = "Response rate", value = "76%", accent = accent, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ModeSwitcher(accountType: AccountType, onAccountTypeChange: (AccountType) -> Unit) {
    Column {
        Text("Browsing as", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeOption(
                label = "Student",
                icon = Icons.Filled.School,
                selected = accountType == AccountType.STUDENT,
                accent = Violet40,
                modifier = Modifier.weight(1f),
                onClick = { onAccountTypeChange(AccountType.STUDENT) }
            )
            ModeOption(
                label = "Company",
                icon = Icons.Filled.Business,
                selected = accountType == AccountType.COMPANY,
                accent = Mint40,
                modifier = Modifier.weight(1f),
                onClick = { onAccountTypeChange(AccountType.COMPANY) }
            )
        }
    }
}

@Composable
private fun ModeOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.5.dp, if (selected) accent else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}