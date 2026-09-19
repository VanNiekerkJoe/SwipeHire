package com.swipehire.app.util

fun calculateProfileStrength(requiredFields: List<String>, hasCv: Boolean = false, cvRequired: Boolean = false): Int {
    val completed = requiredFields.count { it.isNotBlank() } + if (cvRequired && hasCv) 1 else 0
    val total = requiredFields.size + if (cvRequired) 1 else 0
    return if (total == 0) 0 else completed * 100 / total
}

fun matchingSkills(candidateSkills: List<String>, desiredSkills: Collection<String>): List<String> {
    val normalizedDesired = desiredSkills.map { it.trim().lowercase() }.toSet()
    return candidateSkills.filter { it.trim().lowercase() in normalizedDesired }
}
