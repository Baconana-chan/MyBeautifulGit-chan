package org.baconana.mybeautifulgit_chan.domain

/**
 * Provider-agnostic description of a Git remote. Level 1 deliberately does not
 * promise forge permissions, pull requests or checks; those stay UNKNOWN until
 * a provider adapter is added.
 */
enum class RemoteTransport {
    Https,
    Ssh,
    Git,
    Http,
    Local,
    Unknown,
}

enum class RemoteHostKind {
    GitHub,
    GitLab,
    Custom,
    Local,
    Unknown,
}

enum class RemoteIdentityIssue {
    InvalidUrl,
    UnsupportedTransport,
    MissingHost,
    MissingRepository,
    MissingOwner,
}

/** A `url.<base>.insteadOf` rewrite read from Git config. */
data class InsteadOfRule(
    val prefix: String,
    val base: String,
)

data class RemoteUrlIdentity(
    val originalUrl: String,
    val resolvedUrl: String,
    val transport: RemoteTransport,
    val hostKind: RemoteHostKind,
    val host: String? = null,
    val port: Int? = null,
    val owner: String? = null,
    val repository: String? = null,
    val isLocal: Boolean = false,
    val issue: RemoteIdentityIssue? = null,
) {
    val isComplete: Boolean
        get() = issue == null &&
            !isLocal &&
            !host.isNullOrBlank() &&
            !owner.isNullOrBlank() &&
            !repository.isNullOrBlank()

    /** `host/owner/repo`, without guessing missing parts. */
    val fullName: String?
        get() = if (host != null && owner != null && repository != null) {
            "$host/$owner/$repository"
        } else {
            null
        }
}

data class RemoteProfile(
    val name: String,
    val fetchUrls: List<RemoteUrlIdentity>,
    val pushUrls: List<RemoteUrlIdentity> = emptyList(),
) {
    /** Prefer a fully parsed fetch URL, then any fetch URL, then a push URL. */
    val primary: RemoteUrlIdentity?
        get() = fetchUrls.firstOrNull { it.isComplete }
            ?: fetchUrls.firstOrNull()
            ?: pushUrls.firstOrNull { it.isComplete }
            ?: pushUrls.firstOrNull()

    val allUrls: List<RemoteUrlIdentity>
        get() = fetchUrls + pushUrls
}

private val windowsDrivePath = Regex("""^[A-Za-z]:[\\/].*""")

/** Reads `url.<base>.insteadOf` entries without requiring a forge API. */
fun gitInsteadOfRules(entries: List<GitConfigEntry>): List<InsteadOfRule> =
    entries.mapNotNull { entry ->
        if (!entry.key.startsWith("url.", ignoreCase = true)) return@mapNotNull null
        val marker = entry.key.lastIndexOf(".insteadof", ignoreCase = true)
        if (marker <= 4) return@mapNotNull null
        val base = entry.key.substring(4, marker).trim()
        val prefix = entry.value.trim()
        if (base.isEmpty() || prefix.isEmpty()) return@mapNotNull null
        InsteadOfRule(prefix = prefix, base = base)
    }.distinctBy { it.prefix to it.base }
        .sortedByDescending { it.prefix.length }

/**
 * Parses all URL forms accepted by the remote transport layer:
 * scp-like SSH, `ssh://`, `https://`, `git://`, local filesystem paths,
 * explicit ports, and Git config `insteadOf` rewrites.
 *
 * Only unambiguous `host/owner/repo` values are returned. A missing owner or
 * an unsupported transport is reported as an issue, never guessed from the
 * remote name, repository folder or author identity.
 */
fun parseRemoteUrl(
    raw: String,
    insteadOf: List<InsteadOfRule> = emptyList(),
): RemoteUrlIdentity {
    val trimmed = raw.trim()
    val original = redactRemoteUrl(trimmed)
    if (trimmed.isEmpty() || trimmed.any { it.isISOControl() }) {
        return RemoteUrlIdentity(
            originalUrl = original,
            resolvedUrl = original,
            transport = RemoteTransport.Unknown,
            hostKind = RemoteHostKind.Unknown,
            issue = RemoteIdentityIssue.InvalidUrl,
        )
    }

    val expanded = expandInsteadOf(trimmed, insteadOf)
    val resolved = redactRemoteUrl(expanded)
    if (expanded.startsWith("ext::", ignoreCase = true)) {
        return missing(
            original,
            resolved,
            RemoteIdentityIssue.UnsupportedTransport,
            RemoteTransport.Unknown,
        )
    }

    if (looksLikeLocalRemotePath(expanded)) {
        return RemoteUrlIdentity(
            originalUrl = original,
            resolvedUrl = resolved,
            transport = RemoteTransport.Local,
            hostKind = RemoteHostKind.Local,
            isLocal = true,
        )
    }

    val schemeEnd = expanded.indexOf("://")
    if (schemeEnd > 0) {
        return parseSchemeUrl(original, resolved, expanded, schemeEnd)
    }

    // SCP-like syntax: [user@]host:path. C:\... was handled as a local path
    // above, so the remaining colon is a host separator.
    val withoutUser = expanded.substringAfterLast('@')
    val colon = withoutUser.indexOf(':')
    if (colon <= 0) {
        return RemoteUrlIdentity(
            originalUrl = original,
            resolvedUrl = resolved,
            transport = RemoteTransport.Local,
            hostKind = RemoteHostKind.Local,
            isLocal = true,
        )
    }
    val host = withoutUser.substring(0, colon).trim()
    val path = withoutUser.substring(colon + 1)
    if (!validParserHost(host, allowIpv6 = false)) {
        return missing(original, resolved, RemoteIdentityIssue.MissingHost, RemoteTransport.Ssh)
    }
    return pathIdentity(
        originalUrl = original,
        resolvedUrl = resolved,
        host = host,
        port = null,
        path = path,
        transport = RemoteTransport.Ssh,
    )
}

fun parseRemoteUrls(
    rawUrls: List<String>,
    insteadOf: List<InsteadOfRule> = emptyList(),
): List<RemoteUrlIdentity> = rawUrls
    .map { parseRemoteUrl(it, insteadOf) }
    .distinctBy { it.resolvedUrl }

private fun parseSchemeUrl(
    original: String,
    resolved: String,
    value: String,
    schemeEnd: Int,
): RemoteUrlIdentity {
    val scheme = value.substring(0, schemeEnd).lowercase()
    val transport = when (scheme) {
        "https" -> RemoteTransport.Https
        "ssh" -> RemoteTransport.Ssh
        "git" -> RemoteTransport.Git
        "http" -> RemoteTransport.Http
        else -> RemoteTransport.Unknown
    }
    if (transport == RemoteTransport.Unknown) {
        return missing(
            original,
            resolved,
            RemoteIdentityIssue.UnsupportedTransport,
            RemoteTransport.Unknown,
        )
    }

    val remainder = value.substring(schemeEnd + 3)
        .substringBefore('#')
        .substringBefore('?')
    val slash = remainder.indexOf('/')
    val authority = if (slash >= 0) remainder.substring(0, slash) else remainder
    val path = if (slash >= 0) remainder.substring(slash + 1) else ""
    val endpoint = authority.substringAfterLast('@')
    if (endpoint.isBlank()) {
        return missing(original, resolved, RemoteIdentityIssue.MissingHost, transport)
    }

    var host = endpoint
    var port: Int? = null
    if (endpoint.startsWith("[")) {
        val closing = endpoint.indexOf(']')
        if (closing <= 1) {
            return missing(original, resolved, RemoteIdentityIssue.InvalidUrl, transport)
        }
        host = endpoint.substring(1, closing)
        val suffix = endpoint.substring(closing + 1)
        if (suffix.isNotEmpty()) {
            if (!suffix.startsWith(":")) {
                return missing(original, resolved, RemoteIdentityIssue.InvalidUrl, transport)
            }
            port = suffix.substring(1).toIntOrNull()
            if (port == null || port !in 1..65535) {
                return missing(original, resolved, RemoteIdentityIssue.InvalidUrl, transport)
            }
        }
        if (!validParserHost(host, allowIpv6 = true)) {
            return missing(original, resolved, RemoteIdentityIssue.MissingHost, transport)
        }
    } else {
        val firstColon = endpoint.indexOf(':')
        val lastColon = endpoint.lastIndexOf(':')
        if (firstColon >= 0) {
            if (firstColon != lastColon) {
                return missing(original, resolved, RemoteIdentityIssue.InvalidUrl, transport)
            }
            host = endpoint.substring(0, lastColon)
            port = endpoint.substring(lastColon + 1).toIntOrNull()
            if (port == null || port !in 1..65535) {
                return missing(original, resolved, RemoteIdentityIssue.InvalidUrl, transport)
            }
        }
        if (!validParserHost(host, allowIpv6 = false)) {
            return missing(original, resolved, RemoteIdentityIssue.MissingHost, transport)
        }
    }

    val identity = pathIdentity(
        originalUrl = original,
        resolvedUrl = resolved,
        host = host,
        port = port,
        path = path,
        transport = transport,
    )
    // The app intentionally accepts HTTPS for external hosts, while HTTP is
    // displayed only as an unsupported level-1 transport.
    return if (transport == RemoteTransport.Http) {
        identity.copy(issue = RemoteIdentityIssue.UnsupportedTransport)
    } else {
        identity
    }
}

private fun pathIdentity(
    originalUrl: String,
    resolvedUrl: String,
    host: String,
    port: Int?,
    path: String,
    transport: RemoteTransport,
): RemoteUrlIdentity {
    val segments = path.trim('/').split('/').filter { it.isNotEmpty() }
    if (segments.isEmpty()) {
        return missing(originalUrl, resolvedUrl, RemoteIdentityIssue.MissingRepository, transport)
    }
    var repository = segments.last()
    if (repository.endsWith(".git", ignoreCase = true)) {
        repository = repository.dropLast(4)
    }
    if (repository.isBlank() || repository == "." || repository == "..") {
        return missing(originalUrl, resolvedUrl, RemoteIdentityIssue.MissingRepository, transport)
    }
    val owner = segments.dropLast(1).joinToString("/").ifBlank { null }
    val normalizedHost = host.lowercase()
    val hostKind = when (normalizedHost) {
        "github.com" -> RemoteHostKind.GitHub
        "gitlab.com" -> RemoteHostKind.GitLab
        else -> RemoteHostKind.Custom
    }
    return RemoteUrlIdentity(
        originalUrl = originalUrl,
        resolvedUrl = resolvedUrl,
        transport = transport,
        hostKind = hostKind,
        host = normalizedHost,
        port = port,
        owner = owner,
        repository = repository,
        issue = if (owner == null) RemoteIdentityIssue.MissingOwner else null,
    )
}

private fun missing(
    original: String,
    resolved: String,
    issue: RemoteIdentityIssue,
    transport: RemoteTransport,
): RemoteUrlIdentity = RemoteUrlIdentity(
    originalUrl = original,
    resolvedUrl = resolved,
    transport = transport,
    hostKind = RemoteHostKind.Unknown,
    issue = issue,
)

private fun looksLikeLocalRemotePath(value: String): Boolean =
    value.startsWith("/") ||
        value.startsWith("./") ||
        value.startsWith("../") ||
        value.startsWith("~/") ||
        value.startsWith("\\\\") ||
        windowsDrivePath.matches(value) ||
        (!value.contains(':') && !value.contains("://"))

private fun validParserHost(host: String, allowIpv6: Boolean): Boolean {
    if (host.isBlank() || host == "." || host == ".." || host.startsWith("-")) return false
    return host.all { character ->
        character.isLetterOrDigit() ||
            character == '.' ||
            character == '-' ||
            character == '_' ||
            (allowIpv6 && (character == ':' || character == '%'))
    }
}

private fun expandInsteadOf(value: String, rules: List<InsteadOfRule>): String {
    if (rules.isEmpty()) return value
    var current = value
    val seen = HashSet<String>()
    repeat(8) {
        val rule = rules
            .asSequence()
            .filter { it.prefix.isNotEmpty() && current.startsWith(it.prefix) }
            .maxByOrNull { it.prefix.length }
            ?: return current
        val next = rule.base + current.removePrefix(rule.prefix)
        if (next == current || !seen.add(next)) return current
        current = next
    }
    return current
}
