package com.pikaworks.pikaplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

enum class Tab(val label: String, val icon: ImageVector) {
    LIBRARY("보관함", AppIcons.NavLibrary),
    FOLDER("폴더", AppIcons.NavFolder),
    RECENT("최근", AppIcons.NavRecent),
    SETTINGS("설정", AppIcons.NavSettings),
}

@Composable
fun BottomNav(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val colors = PikaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(start = 30.dp, end = 30.dp, top = 10.dp, bottom = 26.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Tab.entries.forEach { tab ->
            val active = tab == current
            Column(
                modifier = Modifier
                    .widthIn(min = 56.dp)
                    .heightIn(min = 44.dp)
                    .clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (active) colors.key else colors.textFaint,
                    modifier = Modifier.size(21.dp),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    tab.label,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.Normal else FontWeight.Light,
                    color = if (active) colors.key else colors.textFaint,
                )
            }
        }
    }
}
