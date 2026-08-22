package com.pikaworks.pikaplayer.subtitle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubtitleTrackTest {

    private val track = SubtitleTrack(
        listOf(
            SubtitleCue(1000, 4000, "첫 자막"),
            SubtitleCue(6500, 9000, "둘째 자막"),
            SubtitleCue(12000, 15000, "셋째 자막"),
        )
    )

    @Test
    fun `구간 안이면 해당 자막을 준다`() {
        assertEquals("첫 자막", track.cueAt(1000)?.text)
        assertEquals("첫 자막", track.cueAt(2500)?.text)
        assertEquals("둘째 자막", track.cueAt(7000)?.text)
    }

    @Test
    fun `자막이 없는 구간은 null`() {
        assertNull(track.cueAt(0))
        assertNull(track.cueAt(5000))
        assertNull(track.cueAt(99_000))
    }

    @Test
    fun `끝 시각은 포함하지 않는다`() {
        assertEquals("첫 자막", track.cueAt(3999)?.text)
        assertNull(track.cueAt(4000), "끝나는 순간에도 남아 있으면 다음 자막과 겹친다")
    }

    @Test
    fun `싱크 오프셋을 적용한다`() {
        // +500ms: 자막이 0.5초 늦게 나온다
        assertNull(track.cueAt(1200, offsetMs = 500))
        assertEquals("첫 자막", track.cueAt(1600, offsetMs = 500)?.text)
        // -500ms: 0.5초 일찍
        assertEquals("첫 자막", track.cueAt(600, offsetMs = -500)?.text)
    }

    @Test
    fun `정렬되지 않은 입력도 처리한다`() {
        val messy = SubtitleTrack(
            listOf(
                SubtitleCue(6500, 9000, "나중"),
                SubtitleCue(1000, 4000, "먼저"),
            )
        )
        assertEquals("먼저", messy.cueAt(2000)?.text)
        assertEquals("나중", messy.cueAt(7000)?.text)
    }

    @Test
    fun `겹치는 자막은 나중에 시작한 쪽을 준다`() {
        val overlapping = SubtitleTrack(
            listOf(
                SubtitleCue(1000, 8000, "긴 자막"),
                SubtitleCue(3000, 5000, "끼어든 자막"),
            )
        )
        assertEquals("긴 자막", overlapping.cueAt(2000)?.text)
        assertEquals("끼어든 자막", overlapping.cueAt(4000)?.text)
    }

    @Test
    fun `빈 트랙은 항상 null`() {
        assertNull(SubtitleTrack.EMPTY.cueAt(1000))
        assertEquals(0, SubtitleTrack.EMPTY.size)
    }
}
