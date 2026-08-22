package com.pikaworks.pikaplayer.subtitle

/**
 * SubRip(.srt) 파서.
 *
 *   1
 *   00:00:01,000 --> 00:00:04,000
 *   첫 줄
 *   둘째 줄
 *
 * 번호 줄은 빠져 있거나 순서가 어긋난 파일이 흔해서 신뢰하지 않는다.
 * 시간 줄을 찾는 것을 기준으로 삼는다.
 */
object SrtParser {

    private val TIME_LINE = Regex(
        """(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})"""
    )

    fun parse(text: String): List<SubtitleCue> {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val cues = mutableListOf<SubtitleCue>()

        var i = 0
        while (i < lines.size) {
            val m = TIME_LINE.find(lines[i])
            if (m == null) { i++; continue }

            val start = toMs(m.groupValues, 1)
            val end = toMs(m.groupValues, 5)

            val body = StringBuilder()
            i++
            while (i < lines.size && lines[i].isNotBlank() && !TIME_LINE.containsMatchIn(lines[i])) {
                if (body.isNotEmpty()) body.append('\n')
                body.append(lines[i].trim())
                i++
            }

            val content = body.toString().trim()
            if (content.isNotEmpty()) cues += SubtitleCue(start, end, content)
        }
        return cues
    }

    private fun toMs(g: List<String>, base: Int): Long {
        val h = g[base].toLong()
        val m = g[base + 1].toLong()
        val s = g[base + 2].toLong()
        // 밀리초 자리가 1~2 자리인 파일이 있다. 자릿수에 맞춰 보정한다.
        val fracRaw = g[base + 3]
        val ms = fracRaw.toLong() * when (fracRaw.length) { 1 -> 100; 2 -> 10; else -> 1 }
        return ((h * 60 + m) * 60 + s) * 1000 + ms
    }
}
