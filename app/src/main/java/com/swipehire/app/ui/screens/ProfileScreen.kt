package com.swipehire.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.MockData
import com.swipehire.app.data.remote.CreateCompanyDto
import com.swipehire.app.data.remote.CreateStudentDto
import com.swipehire.app.data.repository.AppRepository
import com.swipehire.app.ui.theme.Mint20
import com.swipehire.app.ui.theme.Mint40
import com.swipehire.app.ui.theme.Violet20
import com.swipehire.app.ui.theme.Violet40
import com.swipehire.app.ui.theme.glow
import com.swipehire.app.util.calculateProfileStrength
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    accountType: AccountType,
    onAccountTypeChange: (AccountType) -> Unit = {},
    onOpenCreateJob: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val repository = remember { AppRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var profileId by rememberSaveable { mutableStateOf("s1") }
    var name by rememberSaveable { mutableStateOf("Student User") }
    var headline by rememberSaveable { mutableStateOf("Computer Science") }
    var detail by rememberSaveable { mutableStateOf("Final year") }
    var skillsText by rememberSaveable { mutableStateOf("Kotlin, Android, REST APIs") }
    var about by rememberSaveable { mutableStateOf("Passionate software developer looking for opportunities.") }
    var cvUri by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    val cvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Some document providers do not expose persistable permissions; the URI is still usable now.
            }
            cvUri = uri.toString()
        }
    }

    LaunchedEffect(accountType) {
        saveMessage = null
        if (accountType == AccountType.STUDENT) {
            repository.getStudentsFromApi().firstOrNull()?.let { student ->
                profileId = student.id
                name = student.name
                headline = student.course
                detail = student.year
                skillsText = student.skills.joinToString(", ")
                about = student.blurb
            }
        } else {
            profileId = "c1"
            name = "Nexora Fintech"
            headline = "Fintech"
            detail = "Sandton, Johannesburg"
            skillsText = "Backend, Cloud, Security"
            about = "We build secure, high-throughput payment infrastructure for the South African market."
        }
    }

    val skills = skillsText.split(',').map { it.trim() }.filter { it.isNotBlank() }
    val profileStrength = calculateProfileStrength(
        requiredFields = listOf(name, headline, detail, skillsText, about),
        hasCv = cvUri != null,
        cvRequired = accountType == AccountType.STUDENT
    )
    val accent = if (accountType == AccountType.STUDENT) Violet40 else Mint40
    val accentDeep = if (accountType == AccountType.STUDENT) Violet20 else Mint20
    val initials = name.split(' ').filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifBlank { "SH" }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Profile", style = MaterialTheme.typography.headlineSmall)
            Row {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Open settings")
                }
                OutlinedButton(onClick = { editing = true }, shape = RoundedCornerShape(50)) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Edit")
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        ModeSwitcher(accountType = accountType, onAccountTypeChange = onAccountTypeChange)
        Spacer(Modifier.height(22.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(76.dp).glow(accent, radiusMultiplier = 2.4f, alpha = 0.4f).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accent, accentDeep))),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text("$headline · $detail", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(22.dp))
        Card(colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.1f)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Profile strength", fontWeight = FontWeight.SemiBold)
                    Text("$profileStrength%", color = accent, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { profileStrength / 100f },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (profileStrength == 100) "Complete profiles are prioritised in recommendations."
                    else "Add the remaining details to improve your recommendations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        saveMessage?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = accent, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        Text("About", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(about, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(24.dp))
        Text(if (accountType == AccountType.STUDENT) "Skills" else "Hiring for", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(skills) { tag ->
                Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.12f)) {
                    Text(tag, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = accent, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (accountType == AccountType.STUDENT) {
            Spacer(Modifier.height(24.dp))
            Text("Portfolio", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { cvLauncher.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(cvUri?.substringAfterLast('/')?.let { "CV selected: $it" } ?: "Attach CV (PDF)")
            }
        } else {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onOpenCreateJob, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.AddBusiness, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create a job posting")
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Milestones", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { BadgeChip("First match", accent) }
            if (profileStrength >= 80) item { BadgeChip("Profile pro", accent) }
            item { BadgeChip("Fast responder", accent) }
        }

        Spacer(Modifier.height(24.dp))
        Text("Application activity", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(if (accountType == AccountType.STUDENT) "Interests" else "Candidates", "18", accent, Modifier.weight(1f))
            StatTile("Matches", MockData.matches.size.toString(), accent, Modifier.weight(1f))
            StatTile("Avg. reply", "24h", accent, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
    }

    if (editing) {
        ProfileEditorDialog(
            accountType = accountType,
            initialName = name,
            initialHeadline = headline,
            initialDetail = detail,
            initialSkills = skillsText,
            initialAbout = about,
            onDismiss = { editing = false },
            onSave = { newName, newHeadline, newDetail, newSkills, newAbout ->
                name = newName.trim()
                headline = newHeadline.trim()
                detail = newDetail.trim()
                skillsText = newSkills.trim()
                about = newAbout.trim()
                editing = false
                scope.launch {
                    val updatedSkills = newSkills.split(',').map { it.trim() }.filter { it.isNotBlank() }
                    val updatedInitials = newName.split(' ').filter { it.isNotBlank() }.take(2)
                        .mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifBlank { "SH" }
                    val response = if (accountType == AccountType.STUDENT) {
                        repository.updateStudent(
                            profileId,
                            CreateStudentDto(newName.trim(), newHeadline.trim(), newDetail.trim(), updatedSkills, newAbout.trim(), updatedInitials)
                        )
                    } else {
                        repository.updateCompany(
                            profileId,
                            CreateCompanyDto(newName.trim(), newHeadline.trim(), newDetail.trim(), newAbout.trim(), updatedInitials)
                        )
                    }
                    saveMessage = response?.message ?: "Saved locally. Start the API to sync this profile."
                }
            }
        )
    }
}

@Composable
private fun ProfileEditorDialog(
    accountType: AccountType,
    initialName: String,
    initialHeadline: String,
    initialDetail: String,
    initialSkills: String,
    initialAbout: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var headline by remember(initialHeadline) { mutableStateOf(initialHeadline) }
    var detail by remember(initialDetail) { mutableStateOf(initialDetail) }
    var skills by remember(initialSkills) { mutableStateOf(initialSkills) }
    var about by remember(initialAbout) { mutableStateOf(initialAbout) }
    val valid = name.isNotBlank() && headline.isNotBlank() && detail.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(if (accountType == AccountType.STUDENT) "Full name" else "Company name") }, singleLine = true)
                OutlinedTextField(headline, { headline = it }, label = { Text(if (accountType == AccountType.STUDENT) "Course / headline" else "Industry") }, singleLine = true)
                OutlinedTextField(detail, { detail = it }, label = { Text(if (accountType == AccountType.STUDENT) "Study year" else "Location") }, singleLine = true)
                OutlinedTextField(skills, { skills = it }, label = { Text(if (accountType == AccountType.STUDENT) "Skills (comma separated)" else "Hiring for (comma separated)") })
                OutlinedTextField(about, { about = it }, label = { Text("About") }, minLines = 3)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, headline, detail, skills, about) }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun BadgeChip(label: String, accent: Color) {
    Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.12f), border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))) {
        Text(label, Modifier.padding(horizontal = 13.dp, vertical = 8.dp), color = accent, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ModeSwitcher(accountType: AccountType, onAccountTypeChange: (AccountType) -> Unit) {
    Column {
        Text("Browsing as", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeOption("Student", Icons.Filled.School, accountType == AccountType.STUDENT, Violet40, Modifier.weight(1f)) { onAccountTypeChange(AccountType.STUDENT) }
            ModeOption("Company", Icons.Filled.Business, accountType == AccountType.COMPANY, Mint40, Modifier.weight(1f)) { onAccountTypeChange(AccountType.COMPANY) }
        }
    }
}

@Composable
private fun ModeOption(label: String, icon: ImageVector, selected: Boolean, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.5.dp, if (selected) accent else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}