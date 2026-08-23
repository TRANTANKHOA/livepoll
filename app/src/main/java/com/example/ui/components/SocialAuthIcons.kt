package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuthProvider

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = w * 0.42f
        val strokeW = w * 0.18f

        // Blue right & top-right
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeW)
        )
        // Green bottom-right & bottom
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeW)
        )
        // Yellow bottom-left
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeW)
        )
        // Red top-left
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeW)
        )

        // Middle blue bar
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(cx, cy - strokeW / 2),
            size = Size(radius + strokeW / 2, strokeW)
        )
    }
}

@Composable
fun FacebookLogoIcon(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(0xFF1877F2), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "f",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.72f).sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun AppleLogoIcon(modifier: Modifier = Modifier, size: Dp = 24.dp, tint: Color = Color.Black) {
    Box(
        modifier = modifier
            .size(size)
            .background(tint, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "",
            color = if (tint == Color.Black || tint == Color(0xFF1A1A1A)) Color.White else Color.Black,
            fontSize = (size.value * 0.65f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProviderBadge(provider: AuthProvider, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    when (provider) {
        AuthProvider.GOOGLE -> GoogleLogoIcon(modifier = modifier, size = size)
        AuthProvider.FACEBOOK -> FacebookLogoIcon(modifier = modifier, size = size)
        AuthProvider.APPLE -> AppleLogoIcon(modifier = modifier, size = size)
        AuthProvider.GUEST -> {
            Box(
                modifier = modifier
                    .size(size)
                    .background(Color(0xFF757575), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👤",
                    fontSize = (size.value * 0.55f).sp
                )
            }
        }
    }
}
