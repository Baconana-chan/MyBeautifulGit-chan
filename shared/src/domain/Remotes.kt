package org.baconana.mybeautifulgit_chan.domain

/** Как git fetch должен обращаться с тегами. */
enum class FetchTags {
    Default,
    Tags,
    NoTags,
}

data class FetchOptions(
    val prune: Boolean = false,
    val tags: FetchTags = FetchTags.Default,
    val depth: String = "",
    val filter: String = "",
)

/**
 * Собирает ровно ту команду, которую выполняет контроллер.
 * Валидация остаётся снаружи: здесь нет UI-состояния.
 */
fun fetchArgs(remote: String, options: FetchOptions): List<String> = buildList {
    add("fetch")
    if (options.prune) add("--prune")
    when (options.tags) {
        FetchTags.Default -> Unit
        FetchTags.Tags -> add("--tags")
        FetchTags.NoTags -> add("--no-tags")
    }
    options.depth.trim().takeIf { it.isNotEmpty() }?.let { add("--depth=$it") }
    options.filter.trim().takeIf { it.isNotEmpty() }?.let { add("--filter=$it") }
    add(remote.trim())
}

fun validRemoteName(value: String): Boolean {
    val clean = value.trim()
    return clean.isNotEmpty() &&
        !clean.startsWith("-") &&
        !clean.contains("..") &&
        clean.none { it.isWhitespace() || it.isISOControl() }
}

private val supportedRemoteSchemes = setOf("https", "ssh", "git")
private val windowsDrivePath = Regex("""^[A-Za-z]:[\\/].*""")
private val schemePrefix = Regex("""^([A-Za-z][A-Za-z0-9+.-]*):""")
private val scpLikeUrl = Regex("""^[^@/\s]+@[^/:\s]+:.+""")
private val hostLikeUrl = Regex("""^[A-Za-z0-9._-]+:.+""")

/**
 * Remote and submodule URLs use a deliberately small transport allowlist.
 *
 * `https`, `ssh` and `git` are supported. Plain filesystem paths remain valid
 * for local workflows, but the explicit `file://` URI and Git's shell-backed
 * `ext::` transport are rejected. Any other URL scheme is rejected before Git
 * is started. Network invocations also pass `protocol.ext.allow=never` so a
 * user's global Git config cannot re-enable `ext::`.
 */
internal fun validTransportUrl(value: String): Boolean {
    if (value.any { it.isISOControl() }) return false
    val clean = value.trim()
    if (clean.isEmpty() || clean.startsWith("-")) return false
    if (looksLikeLocalPath(clean)) return true

    val schemeEnd = clean.indexOf("://")
    if (schemeEnd >= 0) {
        val scheme = clean.substring(0, schemeEnd).lowercase()
        if (scheme !in supportedRemoteSchemes) return false
        val authority = clean.substring(schemeEnd + 3)
        if (authority.isEmpty() || authority.any { it.isWhitespace() }) return false
        return validAuthority(authority)
    }

    val scheme = schemePrefix.find(clean)?.groupValues?.getOrNull(1)?.lowercase()
    if (scheme != null && scheme in setOf("http", "https", "ssh", "git", "file", "ext")) return false

    if (scpLikeUrl.matches(clean) || hostLikeUrl.matches(clean)) {
        val hostPart = if ('@' in clean) {
            clean.substringAfter('@').substringBefore(':')
        } else {
            clean.substringBefore(':')
        }
        val pathPart = clean.substringAfter(':', "")
        return validHost(hostPart) && pathPart.isNotEmpty()
    }

    // Unknown explicit schemes and URL-like values must not fall through as
    // local paths. A relative filesystem path normally has no colon.
    if (scheme != null || clean.contains(':')) return false
    return true
}

private fun looksLikeLocalPath(value: String): Boolean =
    value.startsWith("/") ||
        value.startsWith("./") ||
        value.startsWith("../") ||
        value.startsWith("~/") ||
        value.startsWith("\\\\") ||
        windowsDrivePath.matches(value)

private fun validAuthority(authority: String): Boolean {
    val endpoint = authority.substringBefore('/').substringBefore('?').substringBefore('#')
    if (endpoint.isEmpty()) return false
    val hostPort = endpoint.substringAfterLast('@')
    if (hostPort.isEmpty()) return false

    var host = hostPort
    var port: String? = null
    if (hostPort.startsWith("[")) {
        // IPv6 literals are valid only inside brackets: https://[::1]/repo.
        val closing = hostPort.indexOf(']')
        if (closing <= 1) return false
        host = hostPort.substring(1, closing)
        val suffix = hostPort.substring(closing + 1)
        if (suffix.isNotEmpty()) {
            if (!suffix.startsWith(":")) return false
            port = suffix.substring(1)
        }
        if (!validBracketedHost(host)) return false
    } else {
        // An unbracketed host may have at most one colon (the port separator).
        // This rejects https://::1 and https://host:22:33 instead of treating
        // the first part as a hostname with an embedded colon.
        if (hostPort.count { it == ':' } > 1) return false
        val colon = hostPort.lastIndexOf(':')
        if (colon >= 0) {
            host = hostPort.substring(0, colon)
            port = hostPort.substring(colon + 1)
        }
        if (!validHost(host)) return false
    }

    if (port != null && !validPort(port)) return false
    return true
}

internal fun validHost(host: String): Boolean {
    if (host.isEmpty() || host == "." || host == ".." || host.startsWith("-")) return false
    if (host.any { it.isWhitespace() || it.isISOControl() || it == '@' || it == '/' || it == ':' }) {
        return false
    }
    return host.all { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' }
}

private fun validBracketedHost(host: String): Boolean {
    if (host.isEmpty() || ':' !in host) return false
    return host.all { it.isLetterOrDigit() || it == ':' || it == '.' || it == '%' }
}

private fun validPort(port: String): Boolean {
    if (port.isEmpty() || port.any { it !in '0'..'9' }) return false
    val value = port.toIntOrNull() ?: return false
    return value in 1..65535
}

fun validRemoteUrl(value: String): Boolean = validTransportUrl(value)

private val urlUserinfo = Regex("""([A-Za-z][A-Za-z0-9+.-]*://)([^/@\s?#]+@)""")

/** Replaces every URL userinfo with `***`; the complete URL is never shown. */
fun redactUrlCredentials(value: String): String =
    urlUserinfo.replace(value) { match ->
        val scheme = match.groupValues[1].removeSuffix("://").lowercase()
        val userinfo = match.groupValues[2].removeSuffix("@")
        // `ssh://git@host` is a normal SSH username, not a secret. Passwords or
        // tokens in SSH userinfo are still hidden.
        if (scheme == "ssh" && ':' !in userinfo) match.value
        else match.groupValues[1] + "***@"
    }

fun urlContainsCredentials(value: String): Boolean =
    urlUserinfo.findAll(value).any { match ->
        val scheme = match.groupValues[1].removeSuffix("://").lowercase()
        val userinfo = match.groupValues[2].removeSuffix("@")
        scheme != "ssh" || ':' in userinfo
    } || scpLikeCredentials(value)

/** Redacts explicit credentials in both scheme URLs and scp-like remotes. */
fun redactRemoteUrl(value: String): String {
    if (value.contains("://")) return redactUrlCredentials(value)
    val at = value.lastIndexOf('@')
    if (scpLikeCredentials(value)) {
        return "***@" + value.substring(at + 1)
    }
    return value
}

private val scpLikeUserinfo = Regex("""^[^@/\\\s]+:[^@/\\\s]*@""")

private fun scpLikeCredentials(value: String): Boolean =
    scpLikeUserinfo.containsMatchIn(value)

fun validFetchDepth(value: String): Boolean {
    val clean = value.trim()
    return clean.isEmpty() || clean.matches(Regex("^[1-9][0-9]{0,6}$"))
}

fun validFetchFilter(value: String): Boolean {
    val clean = value.trim()
    return clean.isEmpty() || (
        !clean.startsWith("-") &&
            clean.none { it.isWhitespace() || it.isISOControl() }
        )
}