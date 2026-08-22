package com.pikaworks.pikaplayer.subtitle

import java.nio.charset.Charset

/**
 * 바이트 → 인코딩 판별 → 형식별 파싱을 한 번에.
 *
 * 파일 읽기(Android URI 처리)는 호출하는 쪽 몫이다. 이 모듈은 순수 로직만 담아
 * 기기 없이도 테스트할 수 있게 둔다.
 */
object SubtitleLoader {

    data class Loaded(
        val cues: List<SubtitleCue>,
        val format: SubtitleFormat,
        val charset: Charset,
    )

    fun load(fileName: String, bytes: ByteArray, forcedCharset: Charset? = null): Loaded? {
        val format = SubtitleFormat.fromFileName(fileName) ?: return null
        val charset = forcedCharset ?: EncodingDetector.detect(bytes).charset
        val text = EncodingDetector.decode(bytes, charset)

        val cues = when (format) {
            SubtitleFormat.SMI -> SmiParser.parse(text)
            SubtitleFormat.SRT -> SrtParser.parse(text)
            // ASS/VTT 는 재생 엔진이 기본 지원하므로 직접 파싱하지 않는다.
            else -> emptyList()
        }
        return Loaded(cues, format, charset)
    }
}
