package com.ether4o4.morsvitaest.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ether4o4.morsvitaest.ui.emberRed
import com.ether4o4.morsvitaest.ui.gradientBrush
import com.ether4o4.morsvitaest.ui.morsRed
import kotlinx.coroutines.delay

/**
 * Split-second "NeverSoft Services" boot splash. Shown once over everything at
 * launch, holds for a moment, then fades out and calls [onFinished] so the host
 * can drop it from composition. It never blocks input for long and always
 * dismisses itself.
 */
@Composable
fun BootSplash(onFinished: () -> Unit) {
    var fadingOut by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (fadingOut) 0f else 1f,
        animationSpec = tween(durationMillis = 350),
        finishedListener = { value ->
            if (value == 0f) onFinished()
        },
    )

    LaunchedEffect(Unit) {
        delay(900)
        fadingOut = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF260A0A), Color(0xFF120404)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(gradientBrush),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "N",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displaySmall,
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "NeverSoft Services",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(Modifier.height(20.dp))

            LinearProgressIndicator(
                modifier = Modifier.width(120.dp).height(3.dp),
                color = emberRed,
                trackColor = morsRed.copy(alpha = 0.25f),
            )
        }
    }
}
