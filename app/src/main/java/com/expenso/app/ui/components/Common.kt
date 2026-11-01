package com.expenso.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenso.app.data.AccountType
import com.expenso.app.data.DueStatus
import com.expenso.app.data.FinanceMath
import com.expenso.app.data.Money
import com.expenso.app.data.local.TransactionEntity
import com.expenso.app.data.TxType
import com.expenso.app.ui.theme.LocalAppColors
import java.time.LocalDate

fun parseHexColor(hex: String, fallback: Color = Color(0xFF625BFF)): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
}

fun categoryIcon(iconKey: String): ImageVector = when (iconKey) {
    "restaurant" -> Icons.Filled.Restaurant
    "groceries" -> Icons.Filled.ShoppingCart
    "dining" -> Icons.Filled.LocalCafe
    "transport" -> Icons.Filled.DirectionsCar
    "shopping" -> Icons.Filled.ShoppingBag
    "entertainment" -> Icons.Filled.Movie
    "bills" -> Icons.Filled.ReceiptLong
    "health" -> Icons.Filled.Favorite
    "education" -> Icons.Filled.School
    "travel" -> Icons.Filled.Flight
    "loans" -> Icons.Filled.AccountBalance
    "credit_card" -> Icons.Filled.CreditCard
    "salary" -> Icons.Filled.Work
    "freelance" -> Icons.Filled.LaptopMac
    "investments" -> Icons.Filled.TrendingUp
    "gifts" -> Icons.Filled.CardGiftcard
    "income" -> Icons.Filled.Paid
    else -> Icons.Filled.Category
}

fun categoryIconFor(name: String): ImageVector = categoryIcon(
    when (name) {
        "Food" -> "restaurant"
        "Groceries" -> "groceries"
        "Dining" -> "dining"
        "Transport" -> "transport"
        "Shopping" -> "shopping"
        "Entertainment" -> "entertainment"
        "Bills" -> "bills"
        "Health" -> "health"
        "Education" -> "education"
        "Travel" -> "travel"
        "Loans" -> "loans"
        "Credit Card" -> "credit_card"
        "Salary" -> "salary"
        "Freelance" -> "freelance"
        "Investments" -> "investments"
        "Gifts" -> "gifts"
        else -> "category"
    }
)

@Composable
fun ScreenBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val app = LocalAppColors.current
    val cardColor = MaterialTheme.colorScheme.surface
    val border = BorderStroke(1.dp, app.line.copy(alpha = if (app.isDark) 1f else 0.75f))
    val shadowModifier = if (app.isDark) modifier else modifier.shadow(
        elevation = 7.dp,
        shape = shape,
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.05f),
        spotColor = Color.Black.copy(alpha = 0.07f)
    )

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = shadowModifier,
            shape = shape,
            color = cardColor,
            border = border
        ) {
            Column(modifier = Modifier.padding(padding), content = content)
        }
    } else {
        Surface(
            modifier = shadowModifier,
            shape = shape,
            color = cardColor,
            border = border
        ) {
            Column(modifier = Modifier.padding(padding), content = content)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun IconCircle(
    icon: ImageVector,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 21.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun CategoryAvatar(
    name: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconKey: String? = null
) {
    val color = parseHexColor(colorHex)
    IconCircle(
        icon = if (iconKey != null) categoryIcon(iconKey) else categoryIconFor(name),
        tint = color,
        background = color.copy(alpha = if (LocalAppColors.current.isDark) 0.22f else 0.12f),
        modifier = modifier,
        size = size
    )
}

@Composable
fun StatusPill(status: String, label: String? = null, modifier: Modifier = Modifier) {
    val app = LocalAppColors.current
    val (color, background, text) = when (status) {
        DueStatus.PAID -> Triple(app.income, app.incomeSoft, label ?: "Paid")
        DueStatus.OVERDUE -> Triple(app.overdue, app.overdueSoft, label ?: "Overdue")
        else -> Triple(app.warning, app.warningSoft, label ?: "Upcoming")
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (app.isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEFF0F5))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val bg by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = tween(200),
                label = "segmentBackground"
            )
            val fg by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else app.muted,
                animationSpec = tween(200),
                label = "segmentText"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(option) },
                        role = Role.Tab
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, LocalAppColors.current.line)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp, horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconCircle(
            icon = icon,
            tint = MaterialTheme.colorScheme.primary,
            background = LocalAppColors.current.indigoSoft,
            size = 62.dp,
            iconSize = 28.dp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.muted,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text(actionText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    destructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = if (destructive) LocalAppColors.current.expense else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, LocalAppColors.current.line),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        actions()
    }
}

data class AvatarPreset(
    val id: String,
    val icon: ImageVector,
    val startColor: Color,
    val endColor: Color
)

/** Built-in profile photos users can pick instead of initials. */
val avatarPresets = listOf(
    AvatarPreset("violet", Icons.Filled.Person, Color(0xFF8A85FF), Color(0xFF5149E4)),
    AvatarPreset("sunset", Icons.Filled.Face, Color(0xFFFF9A6C), Color(0xFFF0526B)),
    AvatarPreset("pets", Icons.Filled.Pets, Color(0xFF67D49A), Color(0xFF199C6B)),
    AvatarPreset("rocket", Icons.Filled.RocketLaunch, Color(0xFFB47CFF), Color(0xFF6A3DE8)),
    AvatarPreset("star", Icons.Filled.Star, Color(0xFFFFC85A), Color(0xFFF29A16)),
    AvatarPreset("gamer", Icons.Filled.SportsEsports, Color(0xFF7FE0D4), Color(0xFF1E9F9A)),
    AvatarPreset("florist", Icons.Filled.LocalFlorist, Color(0xFFFF9AC8), Color(0xFFE44E96)),
    AvatarPreset("favorite", Icons.Filled.Favorite, Color(0xFFFF8A96), Color(0xFFD93A5B))
)

fun avatarPreset(id: String): AvatarPreset? = avatarPresets.firstOrNull { it.id == id }

@Composable
fun Avatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    avatarId: String = "",
    onClick: (() -> Unit)? = null
) {
    val preset = avatarPreset(avatarId)
    val brush = if (preset != null) {
        Brush.linearGradient(listOf(preset.startColor, preset.endColor))
    } else {
        Brush.linearGradient(listOf(Color(0xFF8A85FF), Color(0xFF5149E4)))
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(brush)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = "Profile" },
        contentAlignment = Alignment.Center
    ) {
        if (preset != null) {
            Icon(
                imageVector = preset.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.52f)
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 2.7f).sp
            )
        }
    }
}

@Composable
fun LabelValue(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalAppColors.current.muted
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    categoryColor: String,
    currencyCode: String,
    hidden: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    val app = LocalAppColors.current
    val isIncome = transaction.type == TxType.INCOME
    val amountText = if (hidden) "••••" else {
        (if (isIncome) "+" else "−") + Money.formatAbs(transaction.amountCents, currencyCode)
    }
    val containerModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 2.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp, horizontal = 2.dp)
    }
    Row(
        modifier = containerModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryAvatar(name = transaction.category, colorHex = categoryColor)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${transaction.category} · ${com.expenso.app.data.DateX.formatRelative(LocalDate.ofEpochDay(transaction.dateEpochDay))}",
                style = MaterialTheme.typography.bodySmall,
                color = app.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleSmall,
                color = when {
                    hidden -> app.muted
                    isIncome -> app.income
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = FontWeight.SemiBold
            )
            if (transaction.account == AccountType.CREDIT) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Credit",
                    style = MaterialTheme.typography.labelSmall,
                    color = app.muted
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(2.dp))
            trailing()
        }
    }
}

@Composable
fun MonthStepper(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    nextEnabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            onClick = onPrevious,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, LocalAppColors.current.line),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("‹", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Surface(
            onClick = { if (nextEnabled) onNext() },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, LocalAppColors.current.line),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "›",
                    fontSize = 24.sp,
                    color = if (nextEnabled) MaterialTheme.colorScheme.onSurface else LocalAppColors.current.muted.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LocalAppColors.current.muted)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
