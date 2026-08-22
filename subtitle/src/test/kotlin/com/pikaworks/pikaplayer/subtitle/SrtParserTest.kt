package com.pikaworks.pikaplayer.subtitle

import kotlin.test.Test
import kotlin.test.assertEquals

class SrtParserTest {

    @Test
    fun `번호와 시간과 본문을 읽는다`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:04,000
            이쪽 해안도로가
            진짜 예쁘거든요

            2
            00:01:02,140 --> 00:01:05,000
            다음 자막
        """.trimIndent()

        val cues = SrtParser.parse(srt)
        assertEquals(2, cues.size)
        assertEquals(1000L, cues[0].startMs)
        assertEquals(4000L, cues[0].endMs)
        assertEquals("이쪽 해안도로가\n진짜 예쁘거든요", cues[0].text)
        assertEquals(62140L, cues[1].startMs)
    }

    @Test
    fun `번호 줄이 없어도 읽는다`() {
        val srt = "00:00:02,500 --> 00:00:03,000\n번호 없음"
        val cues = SrtParser.parse(srt)
        assertEquals(1, cues.size)
        assertEquals(2500L, cues[0].startMs)
    }

    @Test
    fun `CRLF 줄바꿈을 처리한다`() {
        val srt = "1\r\n00:00:01,000 --> 00:00:02,000\r\n한 줄\r\n"
        assertEquals(1, SrtParser.parse(srt).size)
    }
}
