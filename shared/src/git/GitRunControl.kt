package org.baconana.mybeautifulgit_chan.git

/**
 * Управляет одним запуском git: позволяет прервать процесс и получать
 * необязательный поток progress-строк из stderr.
 */
class GitRunControl(
    private val onProgress: ((String) -> Unit)? = null,
) {
    @Volatile
    private var cancelRequested = false

    fun cancel() {
        cancelRequested = true
    }

    fun isCancelled(): Boolean = cancelRequested

    /** Вызывается reader-потоком backend'а; callback не должен бросать исключения наружу. */
    fun reportProgress(text: String) {
        if (cancelRequested || text.isEmpty()) return
        runCatching { onProgress?.invoke(text) }
    }
}