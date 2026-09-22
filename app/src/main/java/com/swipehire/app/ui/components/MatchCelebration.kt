package com.swipehire.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swipehire.app.ui.theme.Mint40
import com.swipehire.app.ui.theme.Sky
import com.swipehire.app.ui.theme.Violet40
import com.swipehire.app.ui.theme.auroraMesh
import com.swipehire.app.ui.theme.glow
import com.swipehire.app.ui.tr

/**
 * Full-screen "It's a Match!" moment — the payoff a swipe-to-decide flow
 * needs, since without it a match and a pass currently feel identical.
 */
@Composable
fun MatchCelebrationOverlay(
    visible: Boolean,
    name: String,
    subtitle: String,
    initials: String,
    isCompanySide: Boolean,
    onKeepSwiping: () -> Unit,
    onSendMessage: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(180))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .auroraMesh(dark = true)
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(tween(280), initialScale = 0.8f) + fadeIn(tween(280)),
                exit = scaleOut(tween(150), targetScale = 0.85f) + fadeOut(tween(150))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(96.dp)
                            .glow(Mint40, radiusMultiplier = 2.8f, alpha = 0.7f)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Mint40, Violet40, Sky))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(24.dp))
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = Mint40, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(tr("It's a Match!"), color = Color.White, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(name, color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(subtitle, color = Mint40, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (isCompanySide)
                            "You both swiped right. Say hello and get the conversation going."
                        else
                            "You both swiped right — time to make a great first impression.",
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(36.dp))

                    Button(
                        onClick = onSendMessage,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Violet40)
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Send a message"), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onKeepSwiping,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(tr("Keep swiping"), color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
