package com.expenso.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenso.app.ui.theme.LocalAppColors

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomDestinations = listOf(
    BottomDestination("home", "Home", Icons.Filled.Home),
    BottomDestination("expenses", "Expenses", Icons.Filled.ReceiptLong),
    BottomDestination("analytics", "Analytics", Icons.Filled.PieChart),
    BottomDestination("emi", "EMI & Bills", Icons.Filled.AccountBalance),
    BottomDestination("settings", "Settings", Icons.Filled.Settings)
)

@Composable
fun FloatingBottomNav(
    currentRoute: String?,
    onNavigate: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalAppColors.current
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, app.line.copy(alpha = if (app.isDark) 1f else 0.8f)),
            shadowElevation = if (app.isDark) 0.dp else 14.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomDestinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    val tint by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else app.muted,
                        animationSpec = tween(200),
                        label = "navTint"
                    )
                    val pillColor by animateColorAsState(
                        targetValue = if (selected) app.indigoSoft else Color.Transparent,
                        animationSpec = tween(200),
                        label = "navPill"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = tween(200),
                        label = "navScale"
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(pillColor)
                            .clickable { onNavigate(destination) }
                            .semantics { this.selected = selected }
                            .padding(vertical = 7.dp, horizontal = 1.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                tint = tint,
                                modifier = Modifier
                                    .size(21.dp)
                                    .scale(iconScale)
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.sp),
                            color = tint,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalAppColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(19.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, app.line),
            modifier = Modifier.size(54.dp),
            shadowElevation = if (app.isDark) 0.dp else 3.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = app.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
