package com.pikaworks.pikaplayer.subtitle

/**
 * 파싱된 자막을 재생 위치로 조회할 수 있게 담아둔다.
 *
 * 재생 중에는 프레임마다 호출되므로 선형 탐색은 쓰지 않는다.
 * 자막 파일은 수천 줄이 흔하다.
 */
class SubtitleTrack(cues: List<SubtitleCue>) {

    /** 시작 시각 오름차순. 파일이 정렬돼 있다는 보장이 없다. */
    private val cues: List<SubtitleCue> = cues.sortedBy { it.startMs }

    val size: Int get() = cues.size
    fun isEmpty(): Boolean = cues.isEmpty()

    /**
     * [positionMs] 시점에 보여야 할 자막.
     *
     * [offsetMs] 는 사용자가 맞춘 싱크 값이다. 양수면 자막이 그만큼 늦게 나온다
     * (자막이 영상보다 빠를 때 쓴다).
     */
    fun cueAt(positionMs: Long, offsetMs: Long = 0L): SubtitleCue? {
        if (cues.isEmpty()) return null
        val t = positionMs - offsetMs

        // 시작 시각이 t 이하인 마지막 자막을 찾는다.
        var lo = 0
        var hi = cues.size - 1
        var found = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (cues[mid].startMs <= t) {
                found = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        if (found < 0) return null

        val cue = cues[found]
        return if (t < cue.endMs) cue else null
    }

    companion object {
        val EMPTY = SubtitleTrack(emptyList())
    }
}
