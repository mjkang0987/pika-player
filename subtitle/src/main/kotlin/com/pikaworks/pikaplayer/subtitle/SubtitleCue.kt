package com.pikaworks.pikaplayer.subtitle

/** 자막 한 줄. 시간은 밀리초. */
data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

enum class SubtitleFormat(val extensions: List<String>, val label: String) {
    SRT(listOf("srt"), "SRT"),
    SMI(listOf("smi", "sami"), "SMI"),
    ASS(listOf("ass", "ssa"), "ASS"),
    VTT(listOf("vtt"), "VTT");

    companion object {
        fun fromFileName(name: String): SubtitleFormat? {
            val ext = name.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { ext in it.extensions }
        }
    }
}
