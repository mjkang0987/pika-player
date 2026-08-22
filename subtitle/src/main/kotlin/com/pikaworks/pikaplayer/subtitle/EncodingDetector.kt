package com.pikaworks.pikaplayer.subtitle

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * 외부 자막 파일의 인코딩을 판별한다.
 *
 * 국내 자막은 `.smi` + CP949 조합이 흔한데, 파일 자체에는 인코딩 정보가 없다.
 * 그냥 UTF-8로 읽으면 글자가 깨지고, 사용자는 "자막이 안 나온다"로 받아들인다.
 * 기획서 7.2 — 이탈로 직결되는 지점.
 *
 * 판별 순서:
 *  1. BOM 이 있으면 그대로 믿는다
 *  2. 바이트열이 유효한 UTF-8 인지 검사한다
 *  3. 유효하더라도 결과가 깨진 글자로 보이면 한국어 인코딩으로 되돌린다
 *  4. 그 외에는 CP949 로 본다
 *
 * 3번이 필요한 이유: CP949 한글 바이트쌍이 우연히 유효한 UTF-8 시퀀스가 되는
 * 경우가 있다. 예를 들어 '한'(0xC7 0xD1)은 UTF-8 규칙상 2바이트 문자로도 읽힌다.
 * 검사를 통과했다고 UTF-8 이라고 단정하면 안 된다.
 */
object EncodingDetector {

    /**
     * 한국어 인코딩 이름은 플랫폼마다 다르다. 사용 가능한 첫 번째를 쓴다.
     *
     * 마지막 후보인 EUC-KR 은 CP949 의 부분집합이라, 확장 한글이 섞인 파일에서는
     * 일부 글자가 치환 문자로 떨어질 수 있다. 앞의 후보가 하나라도 있으면 문제없다.
     */
    private val KOREAN_CANDIDATES = listOf("x-windows-949", "MS949", "windows-949", "EUC-KR")

    val korean: Charset by lazy {
        KOREAN_CANDIDATES.firstNotNullOfOrNull { name ->
            runCatching { Charset.forName(name) }.getOrNull()
        } ?: StandardCharsets.UTF_8
    }

    data class Detected(val charset: Charset, val bomLength: Int)

    fun detect(bytes: ByteArray): Detected {
        bom(bytes)?.let { return it }

        val utf8 = decodeStrictly(bytes, StandardCharsets.UTF_8)
        if (utf8 != null && !looksMojibake(utf8)) {
            return Detected(StandardCharsets.UTF_8, 0)
        }
        return Detected(korean, 0)
    }

    /** 판별 결과대로 읽어 문자열로 돌려준다. [forced] 를 주면 판별을 건너뛴다. */
    fun decode(bytes: ByteArray, forced: Charset? = null): String {
        if (forced != null) {
            val skip = bom(bytes)?.bomLength ?: 0
            return String(bytes, skip, bytes.size - skip, forced)
        }
        val (charset, skip) = detect(bytes)
        return String(bytes, skip, bytes.size - skip, charset)
    }

    private fun bom(bytes: ByteArray): Detected? = when {
        bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
            Detected(StandardCharsets.UTF_8, 3)
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
            Detected(StandardCharsets.UTF_16LE, 2)
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
            Detected(StandardCharsets.UTF_16BE, 2)
        else -> null
    }

    /** 디코딩에 실패하면 null. 관대한 치환 대신 예외를 받도록 설정한다. */
    private fun decodeStrictly(bytes: ByteArray, charset: Charset): String? = try {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (e: CharacterCodingException) {
        null
    }

    /**
     * CP949 를 UTF-8 로 잘못 읽으면 라틴 확장 영역 글자가 잔뜩 나온다.
     * 한글이 하나라도 있으면 제대로 읽힌 것으로 본다.
     */
    private fun looksMojibake(text: String): Boolean {
        if (text.isEmpty()) return false
        var hangul = 0
        var latinExtended = 0
        var letters = 0
        for (ch in text) {
            when {
                ch in '가'..'힣' || ch in '㄰'..'㆏' -> { hangul++; letters++ }
                ch in 'À'..'ɏ' -> { latinExtended++; letters++ }
                ch.isLetter() -> letters++
            }
        }
        if (hangul > 0) return false
        if (letters == 0) return false
        return latinExtended.toDouble() / letters > 0.3
    }
}
