package io.github.gythialy.sporteditor.data

/** Compares dotted version tags such as `v1.1.0` or `1.0.1`. */
internal object VersionComparator {

    fun isNewer(latest: String, current: String): Boolean {
        val latestParts = parse(latest) ?: return false
        val currentParts = parse(current) ?: return false
        val size = maxOf(latestParts.size, currentParts.size)
        for (index in 0 until size) {
            val latestPart = latestParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (latestPart != currentPart) return latestPart > currentPart
        }
        return false
    }

    private fun parse(value: String): List<Int>? {
        val cleaned = value.trim().removePrefix("v").substringBefore('-').substringBefore('+')
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split('.')
        return parts.map { it.toIntOrNull() ?: return null }
    }
}
