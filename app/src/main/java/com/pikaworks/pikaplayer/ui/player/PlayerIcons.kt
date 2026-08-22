package com.pikaworks.pikaplayer.ui.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 플레이어 아이콘. 시안의 SVG 패스를 그대로 옮겼다.
 *
 * 색은 여기서 흰색으로 두고 실제 색은 `Icon(tint = ...)` 로 입힌다.
 * 아이콘마다 색을 박아두면 테마 전환에서 어긋난다.
 */
object PlayerIcons {

    val Back = stroke("M15 5 8 12l7 7")

    /**
     * 10초 이동.
     *
     * 화살촉을 원 위쪽으로 빼고 원호를 275도만 그린다. 숫자가 들어갈 가운데를
     * 비워두기 위한 것 — 화살촉이 가운데를 가로지르면 '10' 과 겹친다.
     * 숫자는 벡터가 아니라 Text 로 겹쳐 그린다(폰트를 따라가게).
     */
    val Replay10 = stroke(
        "M12 3.6A8.4 8.4 0 1 1 7.2 5",
        "M12 3.6H8.4",
        "M10.4 1.6 8.4 3.6l2 2",
    )
    val Forward10 = stroke(
        "M12 3.6A8.4 8.4 0 1 0 16.8 5",
        "M12 3.6H15.6",
        "M13.4 1.6 15.6 3.6l-2 2",
    )

    val PreviousTrack = stroke("M11 5 4 12l7 7", "M20 5v14")
    val NextTrack = stroke("M13 5l7 7-7 7", "M4 5v14")

    val Play = fill("M9 6.5v11l9-5.5z")
    val Pause = fill("M7.6 4.6h3.8v14.8H7.6zM12.6 4.6h3.8v14.8h-3.8z")

    val Subtitle = stroke(
        "M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
        "M7 14h5M14.5 14h2.5",
    )
    val AspectRatio = stroke(
        "M3 7.5a1.5 1.5 0 0 1 1.5-1.5h15A1.5 1.5 0 0 1 21 7.5v9a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 16.5z",
        "M8 6v12",
    )
    val Rotate = stroke(
        "M4 9a8 8 0 0 1 13.7-3.2L20 8",
        "M20 4v4h-4",
        "M20 15a8 8 0 0 1-13.7 3.2L4 16",
        "M4 20v-4h4",
    )
    val Lock = stroke(
        "M7 10h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z",
        "M8.5 10V7.5a3.5 3.5 0 0 1 7 0V10",
    )
    val Fullscreen = stroke(
        "M4 9V5.5A1.5 1.5 0 0 1 5.5 4H9",
        "M15 4h3.5A1.5 1.5 0 0 1 20 5.5V9",
        "M20 15v3.5a1.5 1.5 0 0 1-1.5 1.5H15",
        "M9 20H5.5A1.5 1.5 0 0 1 4 18.5V15",
    )

    val Brightness = stroke(
        "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z",
        "M12 3v2M12 19v2M3 12h2M19 12h2M6 6l1.4 1.4M16.6 16.6 18 18M18 6l-1.4 1.4M7.4 16.6 6 18",
    )
    val Volume = stroke(
        "M4 9.5h3.5L12 5.5v13L7.5 14.5H4z",
        "M16 9.2a4 4 0 0 1 0 5.6",
    )

    private fun stroke(vararg pathData: String, width: Float = 1.9f): ImageVector =
        ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            pathData.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun fill(vararg pathData: String): ImageVector =
        ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            pathData.forEach { d ->
                addPath(pathData = addPathNodes(d), fill = SolidColor(Color.White))
            }
        }.build()
}
