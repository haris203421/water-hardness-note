package com.waterhardness.note.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.waterhardness.note.core.theme.*

@Composable
fun PrimaryButton(label: Int, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, spring(dampingRatio = 0.85f, stiffness = 600f), label = "buttonScale")
    val shape = RoundedCornerShape(18.dp)
    val available = enabled && !loading
    Button(onClick = onClick, enabled = available, interactionSource = interaction, shape = shape,
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp).scale(scale).shadow(if (available) 6.dp else 0.dp, shape, ambientColor = ActionAqua, spotColor = ActionAqua).clip(shape).background(Brush.horizontalGradient(if (available) listOf(ActionAqua, OceanLine) else listOf(OceanPale, OceanPale))),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Ink, disabledContainerColor = Color.Transparent, disabledContentColor = InkMuted), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        if (loading) { CircularProgressIndicator(Modifier.size(22.dp), color = Ink, strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
        Text(stringResource(label))
    }
}

@Composable
fun SecondaryButton(label: Int, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: Int? = null) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MineralRose), colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceWhite.copy(alpha = 0.88f), contentColor = PrimaryBurgundy, disabledContainerColor = OceanPale, disabledContentColor = InkMuted)) {
        if (icon != null) { Image(painterResource(icon), null, Modifier.size(24.dp), contentScale = ContentScale.Fit); Spacer(Modifier.width(8.dp)) }
        Text(stringResource(label))
    }
}
