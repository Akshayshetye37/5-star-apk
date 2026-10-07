package com.example.ui.aurelia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun AureliaCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AureliaDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = AureliaColors.Surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        content()
    }
}

@Composable
fun AureliaMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = AureliaColors.Gold,
    onClick: (() -> Unit)? = null
) {
    AureliaCard(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick)
            else Modifier
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = AureliaColors.Muted,
                    fontSize = AureliaTypography.Caption
                )
                Box(
                    Modifier
                        .size(34.dp)
                        .background(accent.copy(alpha = .10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                color = AureliaColors.Ink,
                fontSize = AureliaTypography.Title,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                color = AureliaColors.Muted,
                fontSize = AureliaTypography.Caption
            )
        }
    }
}

@Composable
fun AureliaAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AureliaCard(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(AureliaColors.Primary.copy(alpha = .08f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = AureliaColors.Primary)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = AureliaColors.Ink,
                    fontSize = AureliaTypography.Body,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    color = AureliaColors.Muted,
                    fontSize = AureliaTypography.Caption
                )
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = AureliaColors.Muted)
        }
    }
}
