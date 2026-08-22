package com.pikaworks.pikaplayer.subtitle

/**
 * SAMI(.smi) 파서.
 *
 * 국내 자막 파일에서 비중이 큰 형식인데 재생 엔진이 기본 지원하지 않을 수 있어
 * 직접 파싱한다. 형식은 HTML 을 닮았지만 닫는 태그가 없는 경우가 많다.
 *
 *   <SYNC Start=1000><P Class=KRCC>첫 줄<br>둘째 줄
 *   <SYNC Start=4000><P Class=KRCC>&nbsp;
 *
 * `&nbsp;` 만 있는 SYNC 는 새 자막이 아니라 **앞 자막을 지우는 신호**다.
 * 이걸 자막으로 취급하면 빈 줄이 계속 떠 있게 된다.
 */
object SmiParser {

    private val SYNC = Regex("""<sync\b[^>]*?\bstart\s*=\s*"?(-?\d+)"?[^>]*>""", RegexOption.IGNORE_CASE)
    private val BR = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
    private val TAG = Regex("""<[^>]*>""")

    /** 마지막 자막처럼 끝 시각을 알 수 없을 때 쓰는 기본 노출 시간 */
    private const val DEFAULT_TAIL_MS = 5_000L

    fun parse(text: String): List<SubtitleCue> {
        val syncs = SYNC.findAll(text).toList()
        if (syncs.isEmpty()) return emptyList()

        val cues = mutableListOf<SubtitleCue>()
        var openStart: Long? = null
        var openText: String? = null

        for ((i, m) in syncs.withIndex()) {
            val startMs = m.groupValues[1].toLongOrNull() ?: continue
            val bodyEnd = if (i + 1 < syncs.size) syncs[i + 1].range.first else text.length
            val body = clean(text.substring(m.range.last + 1, bodyEnd))

            // 열려 있던 자막은 이 시각에 닫는다 — 내용이 있든 없든.
            if (openStart != null && openText != null && startMs > openStart) {
                cues += SubtitleCue(openStart, startMs, openText)
            }

            if (body.isEmpty()) {
                openStart = null
                openText = null
            } else {
                openStart = startMs
                openText = body
            }
        }

        if (openStart != null && openText != null) {
            cues += SubtitleCue(openStart, openStart + DEFAULT_TAIL_MS, openText)
        }
        return cues
    }

    private fun clean(raw: String): String =
        raw.replace(BR, "\n")
            .replace(TAG, "")
            .let(::decodeEntities)
            .lines()
            .joinToString("\n") { it.trim() }
            .trim()

    private fun decodeEntities(s: String): String =
        s.replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
}
