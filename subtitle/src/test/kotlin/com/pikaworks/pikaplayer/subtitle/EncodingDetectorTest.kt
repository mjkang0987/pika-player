package com.pikaworks.pikaplayer.subtitle

import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncodingDetectorTest {

    private val korean = "이쪽 해안도로가 진짜 예쁘거든요"

    @Test
    fun `UTF-8 BOM 은 그대로 믿는다`() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val bytes = bom + korean.toByteArray(StandardCharsets.UTF_8)
        val d = EncodingDetector.detect(bytes)
        assertEquals(StandardCharsets.UTF_8, d.charset)
        assertEquals(3, d.bomLength)
        assertEquals(korean, EncodingDetector.decode(bytes))
    }

    @Test
    fun `BOM 없는 UTF-8 한국어를 UTF-8 로 읽는다`() {
        val bytes = korean.toByteArray(StandardCharsets.UTF_8)
        assertEquals(StandardCharsets.UTF_8, EncodingDetector.detect(bytes).charset)
        assertEquals(korean, EncodingDetector.decode(bytes))
    }

    @Test
    fun `CP949 한국어를 UTF-8 로 오인하지 않는다`() {
        val bytes = korean.toByteArray(EncodingDetector.korean)
        val detected = EncodingDetector.detect(bytes).charset
        assertEquals(EncodingDetector.korean, detected, "CP949 자막이 UTF-8 로 판별되면 글자가 깨진다")
        assertEquals(korean, EncodingDetector.decode(bytes))
    }

    @Test
    fun `짧은 CP949 문장도 복구한다`() {
        // '한' 처럼 우연히 유효한 UTF-8 시퀀스가 되는 바이트쌍이 있어
        // 유효성 검사만으로는 부족하다.
        for (s in listOf("한", "안녕", "자막", "제주도 여행")) {
            val bytes = s.toByteArray(EncodingDetector.korean)
            assertEquals(s, EncodingDetector.decode(bytes), "복구 실패: $s")
        }
    }

    @Test
    fun `영어 자막은 UTF-8 로 둔다`() {
        val bytes = "This coastal road is really pretty.".toByteArray(StandardCharsets.UTF_8)
        assertEquals(StandardCharsets.UTF_8, EncodingDetector.detect(bytes).charset)
    }

    @Test
    fun `인코딩을 강제하면 판별을 건너뛴다`() {
        val bytes = korean.toByteArray(EncodingDetector.korean)
        val wrong = EncodingDetector.decode(bytes, StandardCharsets.ISO_8859_1)
        assertTrue(wrong != korean, "강제 지정이 무시되면 안 된다")
        assertEquals(korean, EncodingDetector.decode(bytes, EncodingDetector.korean))
    }
}
