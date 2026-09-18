package com.swipehire.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.swipehire.app.data.AccountType
import com.swipehire.app.data.MockData
import com.swipehire.app.ui.theme.Violet40
import com.swipehire.app.ui.theme.glow
import com.swipehire.app.viewmodel.DiscoverViewModel

@Composable
fun JobLocationScreen(
    jobId: String,
    onBack: () -> Unit,
    viewModel: DiscoverViewModel = viewModel()
) {
    val jobStack by viewModel.jobStack.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCards(AccountType.STUDENT)
    }

    val job = jobStack.find { it.id == jobId } ?: MockData.jobPostings.find { it.id == jobId } ?: return
    val context = LocalContext.current
    val jobLatLng = LatLng(job.latitude, job.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(jobLatLng, 14f)
    }

    LaunchedEffect(jobLatLng) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(jobLatLng, 14f)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text(job.role, style = MaterialTheme.typography.titleMedium)
                Text(job.company, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    Marker(
                        state = MarkerState(position = jobLatLng),
                        title = job.role,
                        snippet = job.workAddress
                    )
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Violet40)
                    Spacer(Modifier.width(8.dp))
                    Text(job.workAddress, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val uri = Uri.parse("google.navigation:q=${job.latitude},${job.longitude}&mode=d")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val webUri = Uri.parse(
                            "https://www.google.com/maps/dir/?api=1&destination=${job.latitude},${job.longitude}"
                        )
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).glow(Violet40, radiusMultiplier = 1.6f, alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet40)
            ) {
                Icon(Icons.Filled.Directions, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Get directions", fontWeight = FontWeight.Bold)
            }
        }
    }
}