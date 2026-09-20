@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.swipehire.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swipehire.app.data.RemoteType
import com.swipehire.app.data.currentFirebaseUserId
import com.swipehire.app.data.remote.CreateJobPostingDto
import com.swipehire.app.data.repository.AppRepository
import com.swipehire.app.ui.tr
import kotlinx.coroutines.launch

@Composable
fun CreateJobScreen(onBack: () -> Unit, onPublished: () -> Unit) {
    val repository = remember { AppRepository() }
    val scope = rememberCoroutineScope()
    val companyId = currentFirebaseUserId()
    var company by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var remoteType by remember { mutableStateOf(RemoteType.HYBRID) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(companyId) {
        if (companyId != null) company = repository.getCompanyProfile(companyId)?.name.orEmpty()
    }

    val valid = companyId != null && company.isNotBlank() && role.isNotBlank() && location.isNotBlank() &&
        address.isNotBlank() && salary.isNotBlank() && tags.isNotBlank() && description.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Create job")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(tr("Publish a role with a verified map location"), style = MaterialTheme.typography.titleLarge)
            Text(
                "SwipeHire geocodes the work address through the custom REST API, stores the coordinates, and uses them for maps, directions, and nearby-job distances.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(company, {}, Modifier.fillMaxWidth(), label = { Text(tr("Company")) }, singleLine = true, readOnly = true)
            OutlinedTextField(role, { role = it }, Modifier.fillMaxWidth(), label = { Text(tr("Job title")) }, singleLine = true)
            OutlinedTextField(location, { location = it }, Modifier.fillMaxWidth(), label = { Text("Area, city") }, placeholder = { Text("Sandton, Johannesburg") }, singleLine = true)
            OutlinedTextField(
                address,
                { address = it },
                Modifier.fillMaxWidth(),
                label = { Text(tr("Work address")) },
                placeholder = { Text("123 Rivonia Road, Sandton") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                singleLine = true
            )
            OutlinedTextField(salary, { salary = it }, Modifier.fillMaxWidth(), label = { Text(tr("Salary range")) }, placeholder = { Text("R18k - R24k / month") }, singleLine = true)
            OutlinedTextField(tags, { tags = it }, Modifier.fillMaxWidth(), label = { Text(tr("Required skills")) }, placeholder = { Text("Kotlin, Android, REST APIs") })
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text(tr("Description")) }, minLines = 3)

            Text(tr("Work arrangement"), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RemoteType.entries.forEach { type ->
                    FilterChip(
                        selected = remoteType == type,
                        onClick = { remoteType = type },
                        label = { Text(type.label) }
                    )
                }
            }

            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(4.dp))
            Button(
                enabled = valid && !isSaving,
                onClick = {
                    scope.launch {
                        isSaving = true
                        message = null
                        val coordinates = repository.geocodeAddress(address.trim())
                        if (coordinates == null) {
                            message = "Address not found. Check the API is running and configure GoogleMaps:ApiKey for new addresses."
                            isSaving = false
                            return@launch
                        }

                        val words = company.trim().split(' ').filter { it.isNotBlank() }
                        val logo = words.take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifBlank { "SH" }
                        val result = repository.createJob(
                            CreateJobPostingDto(
                                companyId = companyId ?: return@launch,
                                company = company.trim(), role = role.trim(),
                                location = "${location.trim()} - ${remoteType.label}", workAddress = address.trim(),
                                latitude = coordinates.first, longitude = coordinates.second,
                                tags = tags.split(',').map { it.trim() }.filter { it.isNotBlank() },
                                blurb = description.trim(), logoInitials = logo,
                                remoteType = remoteType.name, salaryRange = salary.trim()
                            )
                        )
                        isSaving = false
                        if (result?.success == true) onPublished()
                        else message = result?.message ?: "Could not publish. Start the SwipeHire API and try again."
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSaving) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text(tr("Geocode and publish"))
            }
        }
    }
}
