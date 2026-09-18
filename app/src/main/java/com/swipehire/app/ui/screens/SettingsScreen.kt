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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipehire.app.data.AccountType
import com.swipehire.app.ui.theme.Coral
import com.swipehire.app.ui.theme.Mint40
import com.swipehire.app.ui.theme.ThemeMode
import com.swipehire.app.ui.theme.Violet40
import com.swipehire.app.viewmodel.SettingsViewModel

/**
 * The Settings screen — this is the "Application State" deliverable: every control here
 * reads from and writes to DataStore via [SettingsViewModel], so changes survive process death.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onLoggedOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(20.dp)
        )

        AccountTypeNote(current = state.accountType)

        SettingsSection(title = "Notifications", icon = Icons.Filled.NotificationsActive) {
            SwitchRow(
                label = "Push notifications",
                subtitle = "Master switch for all SwipeHire alerts",
                checked = state.pushNotifications,
                onCheckedChange = { viewModel.setPushNotifications(it) }
            )
            SwitchRow(
                label = "New match alerts",
                subtitle = "Get notified the moment there's a mutual match",
                checked = state.matchAlerts,
                enabled = state.pushNotifications,
                onCheckedChange = { viewModel.setMatchAlerts(it) }
            )
            SwitchRow(
                label = "Message alerts",
                subtitle = "Get notified about new chat messages",
                checked = state.messageAlerts,
                enabled = state.pushNotifications,
                onCheckedChange = { viewModel.setMessageAlerts(it) }
            )
        }

        SettingsSection(title = "Privacy", icon = Icons.Filled.Visibility) {
            SwitchRow(
                label = "Profile visibility",
                subtitle = if (state.accountType == AccountType.STUDENT)
                    "Let companies discover your profile while swiping" else
                    "Let students discover your job postings while swiping",
                checked = state.profileVisible,
                onCheckedChange = { viewModel.setProfileVisible(it) }
            )
        }

        SettingsSection(title = "Appearance", icon = Icons.Filled.Palette) {
            ThemeOptionRow("Light", ThemeMode.LIGHT, state.themeMode) { viewModel.setThemeMode(it) }
            ThemeOptionRow("Dark", ThemeMode.DARK, state.themeMode) { viewModel.setThemeMode(it) }
            ThemeOptionRow("Match system", ThemeMode.SYSTEM, state.themeMode) { viewModel.setThemeMode(it) }
        }

        SettingsSection(title = "Security", icon = Icons.Filled.Fingerprint) {
            SwitchRow(
                label = "Biometric lock",
                subtitle = "Require fingerprint or face unlock to open SwipeHire",
                checked = state.biometricLock,
                onCheckedChange = { viewModel.setBiometricLock(it) }
            )
        }

        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onLoggedOut,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null, tint = Coral)
            Spacer(Modifier.width(8.dp))
            Text("Log out", color = Coral, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    Column(Modifier.padding(bottom = 18.dp)) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(
                    Modifier.size(22.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(8.dp))
            }
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.padding(vertical = 4.dp)) { content() }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Mint40)
        )
    }
}

@Composable
private fun ThemeOptionRow(label: String, mode: ThemeMode, current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = current == mode, onClick = { onSelect(mode) }, colors = RadioButtonDefaults.colors(selectedColor = Violet40))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AccountTypeNote(current: AccountType) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (current == AccountType.STUDENT) Icons.Filled.School else Icons.Filled.Business,
                contentDescription = null,
                tint = if (current == AccountType.STUDENT) Violet40 else Mint40,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Browsing as ${if (current == AccountType.STUDENT) "Student" else "Company"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("Switch this from the Profile tab", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}