package com.pikaworks.pikaplayer.ui.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.data.billing.ProductIds
import com.pikaworks.pikaplayer.data.billing.ProductInfo
import com.pikaworks.pikaplayer.entitlement.Tier
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * Pro 안내·구매 화면.
 *
 * 광고가 없어 "광고 제거" 를 팔 수 없다. 기능 차이만으로 설득해야 하므로
 * 무엇이 Free 이고 무엇이 Pro 인지가 한눈에 보여야 한다 — 기획서 3장.
 *
 * 그래서 Free 항목을 먼저, 그것도 전부 체크 표시로 보여준다. "이미 이만큼
 * 무료" 를 감춘 채 잠긴 것만 늘어놓으면 결제 유도로만 읽힌다.
 */
@Composable
fun ProScreen(
    tier: Tier,
    products: Map<String, ProductInfo>,
    onPurchase: (String) -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors

    LazyColumn(modifier = modifier.fillMaxSize().background(colors.background)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 60.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(AppIcons.Back, "뒤로", tint = colors.textPrimary,
                    modifier = Modifier.size(23.dp).clickable(onClick = onBack))
                Text("Pika Pro", fontSize = 20.sp, color = colors.textPrimary)
            }
        }

        item { CurrentTier(tier) }

        item { SectionHeader("무료로 쓰는 것") }
        FREE_ITEMS.forEach { item { FeatureRow(it, unlocked = true) } }

        item { SectionHeader("Pro") }
        PRO_ITEMS.forEach { item { FeatureRow(it, unlocked = tier.atLeast(Tier.PRO)) } }
        item {
            PurchaseButton(
                label = if (tier.atLeast(Tier.PRO)) "구매함" else "Pro 구매",
                price = products[ProductIds.PRO]?.formattedPrice,
                note = "한 번만 결제합니다. 매달 내는 것이 아닙니다.",
                enabled = !tier.atLeast(Tier.PRO),
                onClick = { onPurchase(ProductIds.PRO) },
            )
        }

        item { SectionHeader("Pro+") }
        PRO_PLUS_ITEMS.forEach { item { FeatureRow(it, unlocked = tier.atLeast(Tier.PRO_PLUS)) } }
        item {
            PurchaseButton(
                label = if (tier.atLeast(Tier.PRO_PLUS)) "구독 중" else "Pro+ 구독",
                price = products[ProductIds.PRO_PLUS]?.formattedPrice,
                note = "서버에서 처리하는 기능이라 사용량만큼 비용이 듭니다. 그래서 이것만 구독입니다.",
                enabled = !tier.atLeast(Tier.PRO_PLUS),
                onClick = { onPurchase(ProductIds.PRO_PLUS) },
            )
        }

        item {
            Text(
                "구매 복원",
                fontSize = 13.sp, color = colors.key,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRestore)
                    .padding(horizontal = 20.dp, vertical = 22.dp),
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private val FREE_ITEMS = listOf(
    "전 포맷 재생 · 하드웨어 가속",
    "자막 표시 · 자동 매칭 · 인코딩 변경 · 싱크 조정",
    "제스처 · 재생속도 · 화면비",
    "이어보기 · 폴더 탐색 · 검색",
    "광고 없음",
)

private val PRO_ITEMS = listOf(
    "화면 속 화면(PiP)",
    "네트워크 스트리밍 (SMB · NAS · DLNA)",
    "비공개 폴더 · PIN 잠금",
    "클라우드 연동 · Wi-Fi 파일 전송",
    "Chromecast 송출 · 어린이 잠금",
    "다중 재생목록",
)

private val PRO_PLUS_ITEMS = listOf(
    "AI 화질 향상",
    "자막 자동 생성 · 번역",
)

@Composable
private fun CurrentTier(tier: Tier) {
    val colors = PikaTheme.colors
    val label = when (tier) {
        Tier.FREE -> "지금 등급 · Free"
        Tier.PRO -> "지금 등급 · Pro"
        Tier.PRO_PLUS -> "지금 등급 · Pro+"
    }
    Text(
        label,
        fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.key,
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun SectionHeader(title: String) {
    val colors = PikaTheme.colors
    Text(
        title,
        fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp,
        color = colors.key,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun FeatureRow(label: String, unlocked: Boolean) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (unlocked) AppIcons.Check else AppIcons.Lock,
            null,
            tint = if (unlocked) colors.key else colors.textFaint,
            modifier = Modifier.size(15.dp),
        )
        Text(
            label,
            fontSize = 13.sp, fontWeight = FontWeight.Light,
            color = if (unlocked) colors.textPrimary else colors.textSecondary,
        )
    }
}

@Composable
private fun PurchaseButton(
    label: String,
    price: String?,
    note: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = PikaTheme.colors
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(6.dp))
                .then(
                    if (enabled) Modifier.background(colors.key)
                    else Modifier.border(1.dp, colors.chipBorder, RoundedCornerShape(6.dp))
                )
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                // 가격을 못 받았으면 값을 지어내지 않는다. Play 에 연결되기 전이거나
                // 상품이 아직 심사 중인 상태다.
                if (price != null && enabled) "$label · $price" else label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) colors.background else colors.textMeta,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(note, fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textMeta)
    }
}
