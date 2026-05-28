package com.ether4o4.morsvitaest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import morsvitaest.composeapp.generated.resources.Res
import morsvitaest.composeapp.generated.resources.banner
import org.jetbrains.compose.resources.painterResource

@Composable
fun LogoAnimation(
    modifier: Modifier = Modifier,
    width: Dp = 300.dp,
    height: Dp = 132.dp,
) {
    Image(
        painter = painterResource(Res.drawable.banner),
        contentDescription = "MorsVitaEst",
        modifier = modifier.size(width = width, height = height),
        contentScale = ContentScale.Fit,
    )
}
