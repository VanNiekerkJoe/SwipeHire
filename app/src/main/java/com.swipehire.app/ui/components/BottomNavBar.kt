package com.swipehire.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swipehire.app.ui.theme.Coral
import com.swipehire.app.ui.theme.Mint40
import com.swipehire.app.ui.theme.Violet40

enum class SwipeHireTab(val route: String, val label: String) {
    DISCOVER("discover", "Discover"),
    MATCHES("matches", "Matches"),
    PROFILE("profile", "Profile"),
    SETTINGS("settings", "Settings"),
}

private fun iconFor(tab: SwipeHireTab): ImageVector = when (tab) {
    SwipeHireTab.DISCOVER -> Icons.Filled.Style
    SwipeHireTab.MATCHES -> Icons.Filled.ChatBubble
    SwipeHireTab.PROFILE -> Icons.Filled.Person
    SwipeHireTab.SETTINGS -> Icons.Filled.Settings
}

/**
 * A floating, glass-style pill nav bar with a selection pill that springs
 * between tabs — replaces the stock Material [androidx.compose.material3.NavigationBar]
 * which reads as a generic, edge-docked template bar.
 */
@Composable
fun SwipeHireBottomBar(
    currentTab: SwipeHireTab,
    hasUnreadMatches: Boolean,
    onTabSelected: (SwipeHireTab) -> Unit
) {
    val tabs = SwipeHireTab.values()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 16.dp)
            .height(66.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 20.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val itemWidth = maxWidth / tabs.size
            val selectedIndex = tabs.indexOf(currentTab).coerceAtLeast(0)
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "navIndicator"
            )

            Box(
                Modifier
                    .padding(vertical = 9.dp)
                    .offset(x = indicatorOffset + 9.dp)
                    .width(itemWidth - 18.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Violet40.copy(alpha = 0.20f), Mint40.copy(alpha = 0.16f))
                        )
                    )
            )

            Row(Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    val selected = tab == currentTab
                    val tint = if (selected) Violet40 else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(selected = selected, onClick = { onTabSelected(tab) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                Icon(iconFor(tab), contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
                                if (tab == SwipeHireTab.MATCHES && hasUnreadMatches) {
                                    Box(
                                        Modifier
                                            .size(7.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = 5.dp, y = (-3).dp)
                                            .clip(CircleShape)
                                            .background(Coral)
                                    )
                                }
                            }
                            if (selected) {
                                Text(
                                    tab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tint,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
