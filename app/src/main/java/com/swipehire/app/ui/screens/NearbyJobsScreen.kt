package com.swipehire.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.JobPosting
import com.swipehire.app.data.MockData
import com.swipehire.app.ui.theme.Mint40
import com.swipehire.app.ui.theme.Violet40
import com.swipehire.app.ui.theme.glow
import com.swipehire.app.util.distanceKm
import com.swipehire.app.util.formatDistance
import com.swipehire.app.viewmodel.DiscoverViewModel
import com.swipehire.app.viewmodel.NearbyJobsViewModel

// Roodepoort — used as the map's starting view before we have a location fix.
private val FallbackCenter = LatLng(-26.1006, 27.8563)

@Composable
fun NearbyJobsScreen(
    viewModel: NearbyJobsViewModel = viewModel(),
    discoverViewModel: DiscoverViewModel = viewModel(),
    onBack: () -> Unit,
    onOpenJob: (String) -> Unit
) {
    val context = LocalContext.current
    val userLocation by viewModel.userLocation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val jobStack by discoverViewModel.jobStack.collectAsState()

    LaunchedEffect(Unit) {
        discoverViewModel.loadCards(AccountType.STUDENT)
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    // Handles multiple permission requests safely
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) viewModel.fetchCurrentLocation()
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) viewModel.fetchCurrentLocation()
    }

    val rawJobs = if (jobStack.isNotEmpty()) jobStack else MockData.jobPostings

    // Trigger REST API Geocoding calls for job addresses when rawJobs loads
    LaunchedEffect(rawJobs) {
        rawJobs.forEach { job ->
            viewModel.geocodeAddress(job.workAddress)
        }
    }

    val sortedJobs = remember(userLocation, rawJobs) {
        val loc = userLocation
        if (loc == null) {
            rawJobs.map { it to null as Double? }
        } else {
            rawJobs
                .map { job -> job to distanceKm(loc.latitude, loc.longitude, job.latitude, job.longitude) }
                .sortedBy { it.second }
        }
    }

    val cameraCenter = userLocation?.let { LatLng(it.latitude, it.longitude) } ?: FallbackCenter
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cameraCenter, 10.5f)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Jobs near you", style = MaterialTheme.typography.titleMedium)
        }

        if (!hasLocationPermission) {
            LocationPermissionPrompt(
                onGrant = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
            return@Column
        }

        Box(Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 20.dp)) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    userLocation?.let {
                        Marker(
                            state = MarkerState(position = LatLng(it.latitude, it.longitude)),
                            title = "You"
                        )
                    }
                    sortedJobs.forEach { (job, _) ->
                        Marker(
                            state = MarkerState(position = LatLng(job.latitude, job.longitude)),
                            title = job.role,
                            snippet = job.company,
                            onClick = { onOpenJob(job.id); true }
                        )
                    }
                }
            }
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Violet40)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(sortedJobs) { (job, distance) ->
                NearbyJobRow(job = job, distanceKm = distance, onClick = { onOpenJob(job.id) })
            }
        }
    }
}

@Composable
private fun LocationPermissionPrompt(onGrant: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(64.dp).glow(Violet40, radiusMultiplier = 2.2f, alpha = 0.4f),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(44.dp), tint = Violet40)
        }
        Spacer(Modifier.height(16.dp))
        Text("See jobs close to you", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "SwipeHire needs your location to sort listings by distance and show them on the map.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onGrant,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet40)
        ) { Text("Allow location access") }
    }
}

@Composable
private fun NearbyJobRow(job: JobPosting, distanceKm: Double?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Mint40.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(job.logoInitials, color = Mint40, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(job.role, style = MaterialTheme.typography.titleSmall)
                Text(job.company, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (distanceKm != null) {
                Surface(shape = RoundedCornerShape(50), color = Violet40.copy(alpha = 0.12f)) {
                    Text(
                        formatDistance(distanceKm),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Violet40
                    )
                }
            }
        }
    }
}