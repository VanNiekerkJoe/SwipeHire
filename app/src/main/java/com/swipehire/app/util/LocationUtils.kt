package com.swipehire.app.util

import android.location.Location

/** Great-circle distance between two points, in kilometres. */
fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0] / 1000.0
}

fun formatDistance(km: Double): String =
    if (km < 1.0) "${(km * 1000).toInt()} m" else String.format("%.1f km", km)
