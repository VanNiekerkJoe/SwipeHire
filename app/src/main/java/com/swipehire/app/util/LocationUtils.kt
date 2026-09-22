package com.swipehire.app.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Locale

/** Great-circle distance between two points, in kilometres. */
fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0088
    val latitudeDelta = Math.toRadians(lat2 - lat1)
    val longitudeDelta = Math.toRadians(lon2 - lon1)
    val startLatitude = Math.toRadians(lat1)
    val endLatitude = Math.toRadians(lat2)
    val haversine = sin(latitudeDelta / 2).let { it * it } +
        cos(startLatitude) * cos(endLatitude) * sin(longitudeDelta / 2).let { it * it }
    return earthRadiusKm * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
}

fun formatDistance(km: Double): String =
    if (km < 1.0) "${(km * 1000).toInt()} m" else String.format(Locale.US, "%.1f km", km)
