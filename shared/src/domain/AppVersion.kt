package org.baconana.mybeautifulgit_chan.domain

private val APP_VERSION_PATTERN =
    Regex("""^[vV]?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$""")

/**
 * Semver-like application version. Release tags may carry a leading v and
 * build metadata; prerelease labels are compared with semver precedence.
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: List<String> = emptyList(),
) : Comparable<AppVersion> {
    val isPrerelease: Boolean
        get() = prerelease.isNotEmpty()

    val display: String
        get() = buildString {
            append(major).append('.').append(minor).append('.').append(patch)
            if (prerelease.isNotEmpty()) {
                append('-').append(prerelease.joinToString("."))
            }
        }

    override fun compareTo(other: AppVersion): Int {
        compareValues(major, other.major).takeIf { it != 0 }?.let { return it }
        compareValues(minor, other.minor).takeIf { it != 0 }?.let { return it }
        compareValues(patch, other.patch).takeIf { it != 0 }?.let { return it }
        return comparePrerelease(prerelease, other.prerelease)
    }

    companion object {
        const val CURRENT_TAG = "v1.0.0"
        val current: AppVersion = requireNotNull(parse(CURRENT_TAG))

        fun parse(value: String?): AppVersion? {
            val match = APP_VERSION_PATTERN.matchEntire(value?.trim().orEmpty()) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].toIntOrNull() ?: return null
            val patch = match.groupValues[3].toIntOrNull() ?: return null
            val prerelease = match.groupValues[4]
                .takeIf { it.isNotEmpty() }
                ?.split('.')
                ?.takeIf { parts -> parts.all { it.isNotEmpty() } }
                ?: emptyList()
            return AppVersion(major, minor, patch, prerelease)
        }

        private fun comparePrerelease(left: List<String>, right: List<String>): Int {
            if (left.isEmpty() && right.isEmpty()) return 0
            if (left.isEmpty()) return 1
            if (right.isEmpty()) return -1

            val count = minOf(left.size, right.size)
            for (index in 0 until count) {
                val leftPart = left[index]
                val rightPart = right[index]
                val leftNumber = leftPart.toIntOrNull()
                val rightNumber = rightPart.toIntOrNull()
                val comparison = when {
                    leftNumber != null && rightNumber != null ->
                        compareValues(leftNumber, rightNumber)
                    leftNumber != null -> -1
                    rightNumber != null -> 1
                    else -> leftPart.compareTo(rightPart)
                }
                if (comparison != 0) return comparison
            }
            return compareValues(left.size, right.size)
        }
    }
}
