package com.pikaworks.pikaplayer.subtitle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmiParserTest {

    private val sample = """
        <SAMI>
        <HEAD><STYLE TYPE="text/css"><!-- P { margin:0; } --></STYLE></HEAD>
        <BODY>
        <SYNC Start=1000><P Class=KRCC>이쪽 해안도로가<br>진짜 예쁘거든요
        <SYNC Start=4000><P Class=KRCC>&nbsp;
        <SYNC Start=6500><P Class=KRCC>다음 자막
        <SYNC Start=9000><P Class=KRCC>&nbsp;
        </BODY>
        </SAMI>
    """.trimIndent()

    @Test
    fun `SYNC 시각과 본문을 뽑는다`() {
        val cues = SmiParser.parse(sample)
        assertEquals(2, cues.size)
        assertEquals(1000L, cues[0].startMs)
        assertEquals(4000L, cues[0].endMs)
        assertEquals("이쪽 해안도로가\n진짜 예쁘거든요", cues[0].text)
        assertEquals(6500L, cues[1].startMs)
        assertEquals(9000L, cues[1].endMs)
    }

    @Test
    fun `nbsp 만 있는 SYNC 는 자막이 아니라 지우는 신호다`() {
        val cues = SmiParser.parse(sample)
        assertTrue(cues.none { it.text.isBlank() }, "빈 자막이 목록에 남으면 화면에 빈 줄이 떠 있게 된다")
    }

    @Test
    fun `속성 표기가 달라도 읽는다`() {
        val variants = """
            <sync start="1500"><p class=krcc>따옴표 있는 형태
            <SYNC START=3000 ><P>대문자와 공백
        """.trimIndent()
        val cues = SmiParser.parse(variants)
        assertEquals(2, cues.size)
        assertEquals(1500L, cues[0].startMs)
        assertEquals("따옴표 있는 형태", cues[0].text)
        assertEquals(3000L, cues[1].startMs)
    }

    @Test
    fun `마지막 자막은 기본 시간만큼 노출한다`() {
        val cues = SmiParser.parse("<SYNC Start=2000><P>끝 자막")
        assertEquals(1, cues.size)
        assertTrue(cues[0].endMs > cues[0].startMs)
    }

    @Test
    fun `SYNC 가 없으면 빈 목록`() {
        assertTrue(SmiParser.parse("<SAMI><BODY></BODY></SAMI>").isEmpty())
    }
}
