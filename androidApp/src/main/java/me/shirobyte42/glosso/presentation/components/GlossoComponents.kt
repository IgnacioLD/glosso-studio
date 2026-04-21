package me.shirobyte42.glosso.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Standard card used throughout the app. Provides consistent shape, color and border.
 */
@Composable
fun GlossoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

/**
 * Standard primary action button (large, full-width CTA).
 */
@Composable
fun GlossoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        enabled = enabled,
        shape = RoundedCornerShape(20.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.Black,
            letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
        )
    }
}

/**
 * Standard outlined secondary button.
 */
@Composable
fun GlossoOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

/**
 * Stat display card used on the Home screen.
 */
@Composable
fun GlossoStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.07f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Selectable chip used for filter/toggle selections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossoChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * Section header label used throughout screens.
 */
@Composable
fun GlossoSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}
