package org.baconana.mybeautifulgit_chan.domain

/**
 * Wire values stay playful (chan/kun); stable/beta remain accepted aliases
 * for old settings files and command-line experiments. Human labels live in Strings.
 */
enum class UpdateChannel(val wireName: String) {
    Chan("chan"),
    Kun("kun"),
    ;

    /** kun is the beta channel and also receives stable releases. */
    val includesPrereleases: Boolean
        get() = this == Kun

    companion object {
        fun parse(value: String?): UpdateChannel = when (value?.trim()?.lowercase()) {
            "kun", "beta" -> Kun
            "chan", "stable" -> Chan
            else -> Chan
        }
    }
}
