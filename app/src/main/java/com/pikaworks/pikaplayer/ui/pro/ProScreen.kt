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
import com.pikaworks.pikaplayer.ui.ScreenHeader
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
            ScreenHeader("Pika Pro", onBack)
        }

        item { CurrentTier(tier) }

        item { SectionHeader("무료로 쓰는 것") }
        ProFeatures.free.forEach { item { FeatureRow(it, unlocked = true) } }

        item { SectionHeader("Pro") }
        ProFeatures.pro.forEach { item { FeatureRow(it, unlocked = tier.atLeast(Tier.PRO)) } }
        item {
            // 가격을 못 받았으면 살 수도 없다. Play 에 연결되기 전이거나 상품이
            // 아직 심사 중인 상태다. 버튼을 눌리게 두면 눌러도 아무 일이 없어
            // 고장으로 읽힌다.
            val price = products[ProductIds.PRO]?.formattedPrice
            val owned = tier.atLeast(Tier.PRO)
            PurchaseButton(
                label = when {
                    owned -> "구매함"
                    price == null -> "가격을 불러오는 중"
                    else -> "Pro 구매 · $price"
                },
                note = "한 번만 결제합니다. 매달 내는 것이 아닙니다.",
                enabled = !owned && price != null,
                onClick = { onPurchase(ProductIds.PRO) },
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

@Composable
private fun CurrentTier(tier: Tier) {
    val colors = PikaTheme.colors
    val label = when (tier) {
        Tier.FREE -> "지금 등급 · Free"
        Tier.PRO -> "지금 등급 · Pro"
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
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) colors.background else colors.textMeta,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(note, fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textMeta)
    }
}
