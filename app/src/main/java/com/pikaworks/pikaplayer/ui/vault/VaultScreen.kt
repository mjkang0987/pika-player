package com.pikaworks.pikaplayer.ui.vault

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.data.media.FolderOption
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.ScreenHeader
import com.pikaworks.pikaplayer.ui.theme.PikaTheme
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.pikaworks.pikaplayer.R

/**
 * 비공개 폴더 설정(Pro).
 *
 * 무엇을 약속하는지 화면에서 분명히 한다. 파일을 암호화하지 않고 목록에서 감출
 * 뿐이라, 다른 앱이나 PC 로 보면 그대로 보인다. 이걸 적어두지 않으면 사용자가
 * "숨겼는데 갤러리에 뜬다" 를 결함으로 읽는다.
 *
 * [folders] 는 감춘 것까지 전부 담아야 한다. 감춘 폴더가 후보에서 빠지면 되돌릴
 * 방법이 사라진다.
 */
@Composable
fun VaultScreen(
    folders: List<FolderOption>,
    hidden: Set<String>,
    onToggle: (String, Boolean) -> Unit,
    onChangePin: () -> Unit,
    onDisable: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        // 제목은 목록과 함께 밀려 올라가지 않는다. 어느 화면에 있는지와
        // 뒤로 갈 길은 스크롤 위치와 상관없이 늘 보여야 한다.
        ScreenHeader(stringResource(R.string.vault_title), onBack)

        LazyColumn(modifier = Modifier.weight(1f)) {

            item {
                Text(
                    stringResource(R.string.vault_description),
                    fontSize = 12.sp, fontWeight = FontWeight.Light,
                    lineHeight = 18.sp, color = colors.textMeta,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                )
            }

            item { SectionHeader(stringResource(R.string.vault_section_folders)) }
            if (folders.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.vault_no_folders),
                        fontSize = 13.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
            items(folders, key = { it.key }) { folder ->
                val isHidden = folder.key in hidden
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(folder.key, !isHidden) }
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(AppIcons.NavFolder, null, tint = colors.textSecondary, modifier = Modifier.size(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder.name, fontSize = 14.sp, color = colors.textPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(3.dp))
                        Text(pluralStringResource(R.plurals.vault_folder_video_count, folder.videoCount, folder.videoCount), fontSize = 10.sp,
                            fontWeight = FontWeight.Light, color = colors.textMeta)
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isHidden) colors.key else colors.chipBorder),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isHidden) {
                            Icon(AppIcons.Check, stringResource(R.string.vault_hidden), tint = colors.background, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }

            item { SectionHeader("PIN") }
            item { ActionRow(stringResource(R.string.vault_change_pin), colors.textPrimary, onChangePin) }
            item { ActionRow(stringResource(R.string.vault_disable), colors.textMeta, onDisable) }
            item {
                Text(
                    stringResource(R.string.vault_disable_note),
                    fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textFaint,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 32.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = PikaTheme.colors
    Text(
        title,
        fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp,
        color = colors.key,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp),
    )
}

@Composable
private fun ActionRow(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 50.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = color)
    }
}
