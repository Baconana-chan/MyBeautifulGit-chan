package org.baconana.mybeautifulgit_chan.app

import org.baconana.mybeautifulgit_chan.domain.validCloneTarget
import org.baconana.mybeautifulgit_chan.domain.validRepositoryName
import java.io.File

/**
 * If [base] is an existing directory, clone into a direct child named after
 * the URL. The child must be a single, safe path component and its canonical
 * parent must remain [base]. A future path is returned as-is after the
 * `.`/`..` and option checks in validCloneTarget.
 */
internal fun resolveCloneTarget(base: String, repositoryName: String): String? {
    if (!validCloneTarget(base)) return null
    val cleanBase = base.trim()

    val baseFile = File(cleanBase)
    if (!baseFile.exists()) return cleanBase
    if (!baseFile.isDirectory) return null
    if (!validRepositoryName(repositoryName)) return null

    val target = File(baseFile, repositoryName.trim())
    val canonicalBase = runCatching { baseFile.canonicalFile }.getOrNull() ?: return null
    val canonicalTarget = runCatching { target.canonicalFile }.getOrNull() ?: return null
    if (canonicalTarget.parentFile != canonicalBase) return null
    return target.path
}
