package org.baconana.mybeautifulgit_chan.domain

/** Запись в .git/rr-cache: hash конфликта и размеры сохранённых версий. */
data class RerereEntry(
    val hash: String,
    val preimageBytes: Long,
    val postimageBytes: Long,
    val modifiedAt: Long,
)

/** Содержимое выбранной записи; читается отдельно от списка. */
data class RerereDetail(
    val hash: String,
    val preimage: String?,
    val postimage: String?,
    val preimageBytes: Long,
    val postimageBytes: Long,
    val preimageTruncated: Boolean = false,
    val postimageTruncated: Boolean = false,
)

data class RerereState(
    val enabled: Boolean = false,
    val cachePath: String = "",
    val entries: List<RerereEntry> = emptyList(),
    val error: String? = null,
)
