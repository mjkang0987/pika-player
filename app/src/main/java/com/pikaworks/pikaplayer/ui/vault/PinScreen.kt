package com.pikaworks.pikaplayer.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.ScreenHeader
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/** PIN 길이. 짧으면 외우기 쉽고 길면 대입이 어렵다. 4는 너무 얕다. */
const val PIN_LENGTH = 6

/**
 * PIN 입력.
 *
 * 시스템 키보드를 쓰지 않고 숫자판을 직접 그린다. 숫자만 받는데 전체 키보드가
 * 올라오면 자리를 크게 먹고, 화면 아래가 밀려 입력한 자릿수가 가린다.
 *
 * 화면이 두 가지 일(새로 정하기 / 확인하기)을 겸한다. 배치가 같아서 따로 만들면
 * 두 벌을 나란히 고쳐야 한다 — 문구와 다음 동작만 다르다.
 */
@Composable
fun PinScreen(
    title: String,
    subtitle: String,
    entered: String,
    /** 남은 잠금 시간. 0 이면 입력할 수 있다. */
    lockedForMs: Long,
    error: String?,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors
    val locked = lockedForMs > 0

    Column(
        modifier = modifier.fillMaxSize().background(colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeader(title, onBack)

        Spacer(Modifier.height(22.dp))
        Text(
            subtitle,
            fontSize = 13.sp, fontWeight = FontWeight.Light, color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(PIN_LENGTH) { index ->
                val filled = index < entered.length
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (filled) colors.key else colors.chipBorder),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        // 안내 자리를 늘 비워 둔다. 오류가 났을 때만 나타나면 숫자판이 위아래로 뛴다.
        Box(modifier = Modifier.heightIn(min = 20.dp), contentAlignment = Alignment.Center) {
            val message = when {
                locked -> "너무 많이 틀렸습니다 · ${formatWait(lockedForMs)} 후에 다시"
                error != null -> error
                else -> ""
            }
            Text(message, fontSize = 12.sp, color = colors.textMeta, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(16.dp))
        // 숫자판은 남는 높이에 맞춘다.
        //
        // 72dp 로 못 박아 두었더니 4줄 + 여백이 318dp 였고, 작은 기기(≈558dp
        // 높이)에서는 하단 네비게이션에 마지막 줄이 잘렸다. 숫자판은 스크롤할
        // 것이 아니라 한눈에 다 보여야 하는 물건이라, 자리에 맞춰 줄인다.
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val gaps = KEY_GAP * 3
            // 자리를 꽉 채우지 않는다. 남긴 만큼이 숫자판 위아래 여백이 된다
            // (가운데 정렬이라 절반씩 나뉜다). 딱 맞추면 하단 네비게이션에
            // 붙어 보인다.
            val breathing = 56.dp
            // 아래 한계는 44dp. 손가락으로 겨눌 수 있는 최소 크기라 더 줄이지 않는다.
            val keySize = ((maxHeight - gaps - breathing) / 4).coerceIn(44.dp, 72.dp)
            Keypad(
                keySize = keySize,
                enabled = !locked,
                onDigit = onDigit,
                onBackspace = onBackspace,
            )
        }
    }
}

/** 숫자 사이 간격. 키 크기를 계산할 때도 쓴다. */
private val KEY_GAP = 10.dp

/** 남은 시간을 사람이 읽는 단위로. 초 단위까지 보여주면 계속 다시 그려야 한다. */
private fun formatWait(ms: Long): String {
    val totalSec = (ms + 999) / 1000
    return if (totalSec < 60) "${totalSec}초" else "${(totalSec + 59) / 60}분"
}

@Composable
private fun Keypad(
    keySize: Dp,
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KEY_GAP),
    ) {
        listOf("123", "456", "789").forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
                line.forEach { digit -> Key(digit.toString(), keySize, enabled) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP)) {
            // 1·4·7 줄과 세로로 맞추려고 빈자리를 채운다.
            Spacer(Modifier.width(keySize))
            Key("0", keySize, enabled) { onDigit('0') }
            Key("⌫", keySize, enabled, onClick = onBackspace)
        }
    }
}

@Composable
private fun Key(label: String, size: Dp, enabled: Boolean, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2))
            .background(colors.surface)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 22.sp,
            fontWeight = FontWeight.Light,
            color = if (enabled) colors.textPrimary else colors.textFaint,
        )
    }
}
