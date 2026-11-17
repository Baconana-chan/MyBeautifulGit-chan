package org.baconana.mybeautifulgit_chan.ai

enum class AiCommitStyle(val wireName: String) {
    Conventional("conventional"),
    Plain("plain");

    companion object {
        fun parse(value: String?): AiCommitStyle =
            entries.firstOrNull { it.wireName.equals(value?.trim(), ignoreCase = true) }
                ?: Conventional
    }
}

enum class AiDiffSource {
    Staged,
    Worktree,
}

enum class AiMessageLanguage(val wireName: String) {
    Auto("auto"),
    English("english"),
    Russian("russian");

    companion object {
        fun parse(value: String?): AiMessageLanguage =
            entries.firstOrNull { it.wireName.equals(value?.trim(), ignoreCase = true) }
                ?: Auto
    }
}

/**
 * Opt-in configuration for an OpenAI-compatible commit-message endpoint.
 *
 * The API key is deliberately not part of this object: it lives in the
 * encrypted credential store, never in settings.properties.
 */
data class AiSettings(
    val enabled: Boolean = false,
    val endpoint: String = "",
    val model: String = "gpt-4o-mini",
    val messageLanguage: AiMessageLanguage = AiMessageLanguage.Auto,
    val style: AiCommitStyle = AiCommitStyle.Conventional,
    val maxDiffChars: Int = 12_000,
    val temperature: Double = 0.2,
    val allowLocalHttp: Boolean = false,
) {
    companion object {
        const val MIN_DIFF_CHARS = 1_000
        const val MAX_DIFF_CHARS = 100_000
        const val MIN_TEMPERATURE = 0.0
        const val MAX_TEMPERATURE = 2.0
    }
}
