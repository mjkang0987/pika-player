package com.pikaworks.pikaplayer.subtitle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubtitleLoaderTest {

    @Test
    fun `CP949 로 저장된 smi 파일을 끝까지 읽는다`() {
        val text = "<SYNC Start=1000><P Class=KRCC>이쪽 해안도로가 진짜 예쁘거든요"
        val bytes = text.toByteArray(EncodingDetector.korean)

        val loaded = SubtitleLoader.load("제주도_여행_2일차.smi", bytes)!!
        assertEquals(SubtitleFormat.SMI, loaded.format)
        assertEquals(EncodingDetector.korean, loaded.charset)
        assertEquals(1, loaded.cues.size)
        assertEquals("이쪽 해안도로가 진짜 예쁘거든요", loaded.cues[0].text)
    }

    @Test
    fun `확장자로 형식을 가른다`() {
        assertEquals(SubtitleFormat.SRT, SubtitleFormat.fromFileName("a.SRT"))
        assertEquals(SubtitleFormat.SMI, SubtitleFormat.fromFileName("a.smi"))
        assertNull(SubtitleFormat.fromFileName("a.mp4"))
    }
}
