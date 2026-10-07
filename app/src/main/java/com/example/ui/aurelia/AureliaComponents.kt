package com.example.ui.aurelia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AureliaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AureliaDimens.Radius),
        colors = CardDefaults.cardColors(
            containerColor = AureliaColors.Surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            AureliaColors.Line
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(AureliaDimens.Card),
            content = content
        )
    }
}

@Composable
fun AureliaButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 46.dp),
            shape = RoundedCornerShape(AureliaDimens.SmallRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = AureliaColors.Brand,
                contentColor = Color.White
            )
        ) {
            Text(text, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 46.dp),
            shape = RoundedCornerShape(AureliaDimens.SmallRadius),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AureliaColors.Ink
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                AureliaColors.Line
            )
        ) {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AureliaHeroTitle(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            color = AureliaColors.Ink,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            fontSize = AureliaTypography.Hero
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = subtitle,
            color = AureliaColors.Muted,
            fontSize = AureliaTypography.Body
        )
    }
}

@Composable
fun AureliaKpiCard(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    AureliaCard(modifier = modifier) {
        Text(
            label.uppercase(),
            color = AureliaColors.Muted,
            fontSize = AureliaTypography.Caption,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            value,
            color = AureliaColors.Ink,
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(3.dp))
        Text(
            subtitle,
            color = AureliaColors.Muted,
            fontSize = AureliaTypography.Caption
        )
    }
}

@Composable
fun AureliaAttentionRow(
    text: String,
    value: String,
    danger: Boolean = false,
    warning: Boolean = false
) {
    val border = when {
        danger -> Color(0xFFF2C7C7)
        warning -> Color(0xFFF0DFB8)
        else -> AureliaColors.Line
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFFBFEFE),
                RoundedCornerShape(AureliaDimens.SmallRadius)
            )
            .then(
                Modifier.padding(13.dp)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            color = AureliaColors.Ink,
            fontSize = AureliaTypography.Body,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            color = when {
                danger -> AureliaColors.Danger
                warning -> AureliaColors.Warn
                else -> AureliaColors.Ink
            },
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun AureliaRoomCard(
    roomNumber: String,
    roomType: String,
    rate: String,
    status: String,
    guest: String?,
    checkout: String?,
    onFolio: () -> Unit,
    onManage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                AureliaColors.Surface,
                RoundedCornerShape(18.dp)
            )
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                roomNumber,
                color = AureliaColors.Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                status,
                color = when (status.uppercase()) {
                    "AVAILABLE" -> AureliaColors.Ok
                    "OCCUPIED" -> Color(0xFF146B72)
                    "RESERVED" -> Color(0xFF7652A8)
                    "CLEANING" -> Color(0xFF94620E)
                    else -> AureliaColors.Danger
                },
                modifier = Modifier
                    .background(
                        AureliaColors.Soft,
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(Modifier.height(7.dp))

        Text(
            "$roomType · $rate",
            color = AureliaColors.Muted,
            fontSize = AureliaTypography.Body
        )

        if (!guest.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                guest,
                color = AureliaColors.Ink,
                fontWeight = FontWeight.Bold
            )
            if (!checkout.isNullOrBlank()) {
                Text(
                    checkout,
                    color = AureliaColors.Muted,
                    fontSize = AureliaTypography.Caption
                )
            }
        }

        Spacer(Modifier.height(11.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AureliaButton(
                "Folio",
                modifier = Modifier.weight(1f),
                onClick = onFolio
            )
            AureliaButton(
                "Manage",
                modifier = Modifier.weight(1f),
                onClick = onManage
            )
        }
    }
}

@Composable
fun AureliaSectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = AureliaColors.Ink,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = AureliaTypography.Section,
            fontWeight = FontWeight.Bold
        )

        if (action != null && onAction != null) {
            AureliaButton(
                action,
                modifier = Modifier.heightIn(min = 40.dp),
                onClick = onAction
            )
        }
    }
}
