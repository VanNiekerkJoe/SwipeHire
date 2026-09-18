package com.swipehire.app.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.swipehire.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** The user's current location, once we have permission and a fix. Null until then. */
data class UserLocation(val latitude: Double, val longitude: Double)

class NearbyJobsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _userLocation = MutableStateFlow<UserLocation?>(null)
    val userLocation: StateFlow<UserLocation?> = _userLocation

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Call once ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION has been granted. */
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cancellationSource = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationSource.token
                ).await()

                _userLocation.value = location?.let { UserLocation(it.latitude, it.longitude) }
            } catch (e: Exception) {
                _userLocation.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Triggers a POST call to the C# Web API via Retrofit to geocode an address. */
    fun geocodeAddress(address: String) {
        viewModelScope.launch {
            try {
                repository.geocodeAddress(address)
            } catch (e: Exception) {
                // Ignore or log geocoding errors
            }
        }
    }
}