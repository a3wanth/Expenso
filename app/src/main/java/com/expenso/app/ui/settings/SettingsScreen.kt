package com.expenso.app.ui.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.BuildConfig
import com.expenso.app.ui.components.Avatar
import com.expenso.app.ui.components.ConfirmDialog
import com.expenso.app.ui.components.ScreenHeader
import com.expenso.app.ui.components.SurfaceCard
import com.expenso.app.ui.components.avatarPresets
import com.expenso.app.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

private val currencyOptions = listOf(
    "USD" to "US Dollar",
    "EUR" to "Euro",
    "GBP" to "British Pound",
    "INR" to "Indian Rupee",
    "JPY" to "Japanese Yen",
    "CAD" to "Canadian Dollar",
    "AUD" to "Australian Dollar",
    "SGD" to "Singapore Dollar"
)

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onOpenBudgets: () -> Unit,
    onOpenCategories: () -> Unit,
    onLockNow: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    val app = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showProfile by remember { mutableStateOf(false) }
    var showCurrency by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var showDisableLock by remember { mutableStateOf(false) }
    var showDemoConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showRemoveDemoConfirm by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp)
    ) {
        item {
            ScreenHeader(
                title = "Settings",
                subtitle = "Personalise Expenso and manage your data.",
                eyebrow = "Preferences"
            )
            Spacer(Modifier.height(18.dp))
        }

        item {
            SurfaceCard(onClick = { showProfile = true }, padding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(initials = prefs.initials, size = 56.dp, avatarId = prefs.avatarId)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = prefs.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (prefs.demoMode) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = "Demo data is active",
                                style = MaterialTheme.typography.labelSmall,
                                color = app.warning
                            )
                        }
                    }
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            SectionLabel("Appearance & preferences")
            Spacer(Modifier.height(8.dp))
        }

        item {
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Filled.AttachMoney,
                    title = "Currency",
                    subtitle = currencyOptions.firstOrNull { it.first == prefs.currencyCode }?.second
                        ?: prefs.currencyCode,
                    onClick = { showCurrency = true }
                )
                GroupDivider()
                SettingsRow(
                    icon = Icons.Filled.DarkMode,
                    title = "Theme",
                    subtitle = when (prefs.themeMode) {
                        "LIGHT" -> "Light"
                        "DARK" -> "Dark"
                        else -> "Follow system"
                    },
                    onClick = { showTheme = true }
                )
                GroupDivider()
                SwitchRow(
                    icon = Icons.Filled.Visibility,
                    title = "Hide balances",
                    subtitle = "Mask amounts across the app",
                    checked = prefs.hideBalance,
                    onCheckedChange = vm::setHideBalance
                )
                GroupDivider()
                SwitchRow(
                    icon = Icons.Filled.NotificationsNone,
                    title = "Notifications",
                    subtitle = "Show due-date reminders in app",
                    checked = prefs.notificationsEnabled,
                    onCheckedChange = vm::setNotifications
                )
                GroupDivider()
                SwitchRow(
                    icon = Icons.Filled.Payments,
                    title = "Due reminders",
                    subtitle = "Highlight overdue bills and EMIs",
                    checked = prefs.dueReminders,
                    onCheckedChange = vm::setDueReminders
                )
            }
            Spacer(Modifier.height(22.dp))
            SectionLabel("Security")
            Spacer(Modifier.height(8.dp))
        }

        item {
            SettingsGroup {
                SwitchRow(
                    icon = Icons.Filled.Security,
                    title = "App lock",
                    subtitle = if (prefs.lockActive) "PIN required on launch" else "Protect Expenso with a 4-digit PIN",
                    checked = prefs.lockActive,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showPin = true
                        } else {
                            showDisableLock = true
                        }
                    }
                )
                if (prefs.lockActive) {
                    GroupDivider()
                    SettingsRow(
                        icon = Icons.Filled.Lock,
                        title = "Change PIN",
                        subtitle = "Set a new 4-digit code",
                        onClick = { showPin = true }
                    )
                    GroupDivider()
                    SettingsRow(
                        icon = Icons.Filled.Security,
                        title = "Lock now",
                        subtitle = "Require your PIN immediately",
                        onClick = onLockNow,
                        showChevron = false
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            SectionLabel("Data & storage")
            Spacer(Modifier.height(8.dp))
        }

        item {
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Filled.Payments,
                    title = "Budgets",
                    subtitle = "${state.budgetCount} monthly ${if (state.budgetCount == 1) "budget" else "budgets"}",
                    onClick = onOpenBudgets
                )
                GroupDivider()
                SettingsRow(
                    icon = Icons.Filled.Category,
                    title = "Categories",
                    subtitle = "${state.categoryCount} categories",
                    onClick = onOpenCategories
                )
                GroupDivider()
                SettingsRow(
                    icon = Icons.Filled.FileDownload,
                    title = if (exporting) "Preparing export…" else "Export data",
                    subtitle = "Share a CSV of your transactions, loans and bills",
                    onClick = {
                        if (exporting) return@SettingsRow
                        exporting = true
                        scope.launch {
                            val csv = runCatching { vm.buildExportCsv() }.getOrNull()
                            val result = if (csv == null) {
                                Result.failure(IllegalStateException("Could not read data"))
                            } else {
                                runCatching { shareCsv(context, csv) }
                            }
                            exporting = false
                            Toast.makeText(
                                context,
                                if (result.isSuccess) "Export ready to share" else "Could not create export",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                GroupDivider()
                SettingsRow(
                    icon = Icons.Filled.Storage,
                    title = "Stored records",
                    subtitle = "${state.transactionCount} transactions · ${state.loanCount} loans · ${state.billCount} bills · ${state.cardCount} cards",
                    showChevron = false
                )
            }
            Spacer(Modifier.height(22.dp))
            SectionLabel("Sample data")
            Spacer(Modifier.height(8.dp))
        }

        item {
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Filled.Storage,
                    title = if (prefs.demoMode) "Remove demo data" else "Load demo data",
                    subtitle = if (prefs.demoMode) {
                        "Clear the sample set and start fresh"
                    } else {
                        "Replace financial data with a sample month"
                    },
                    onClick = {
                        if (prefs.demoMode) showRemoveDemoConfirm = true else showDemoConfirm = true
                    }
                )
                GroupDivider()
                SettingsRow(
                    icon = Icons.Filled.DeleteSweep,
                    title = "Clear all financial data",
                    subtitle = "Delete transactions, budgets, loans and bills",
                    onClick = { showClearConfirm = true },
                    tint = app.expense,
                    showChevron = false
                )
            }
            Spacer(Modifier.height(22.dp))
        }

        item {
            SurfaceCard(padding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(app.indigoSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Expenso ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Your financial data never leaves this device. Room + DataStore keep everything local.",
                            style = MaterialTheme.typography.bodySmall,
                            color = app.muted
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Built with Kotlin, Jetpack Compose and Material 3.",
                style = MaterialTheme.typography.labelSmall,
                color = app.muted.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showProfile) {
        ProfileDialog(
            name = prefs.name,
            avatarId = prefs.avatarId,
            onDismiss = { showProfile = false },
            onSave = { name, avatarId ->
                vm.saveProfile(name, avatarId)
                showProfile = false
            }
        )
    }

    if (showCurrency) {
        ChoiceDialog(
            title = "Currency",
            options = currencyOptions,
            selected = prefs.currencyCode,
            onSelect = {
                vm.setCurrency(it)
                showCurrency = false
            },
            onDismiss = { showCurrency = false }
        )
    }

    if (showTheme) {
        ChoiceDialog(
            title = "Theme",
            options = listOf("SYSTEM" to "Follow system", "LIGHT" to "Light", "DARK" to "Dark"),
            selected = prefs.themeMode,
            onSelect = {
                vm.setTheme(it)
                showTheme = false
            },
            onDismiss = { showTheme = false }
        )
    }

    if (showPin) {
        PinDialog(
            title = if (prefs.lockActive) "Change your PIN" else "Create a PIN",
            onDismiss = { showPin = false },
            onConfirm = { pin ->
                if (prefs.lockActive) vm.changePin(pin) else vm.enableAppLock(pin)
                showPin = false
            }
        )
    }

    if (showDisableLock) {
        ConfirmDialog(
            title = "Turn off app lock?",
            message = "Expenso will open without asking for a PIN.",
            confirmLabel = "Turn off",
            onConfirm = {
                vm.disableAppLock()
                showDisableLock = false
            },
            onDismiss = { showDisableLock = false }
        )
    }

    if (showDemoConfirm) {
        ConfirmDialog(
            title = "Load demo data?",
            message = "This replaces all current transactions, budgets, loans, bills and cards with a realistic sample set. Categories reset to defaults.",
            confirmLabel = "Load demo",
            destructive = false,
            onConfirm = {
                vm.loadDemoData()
                showDemoConfirm = false
                Toast.makeText(context, "Demo data loaded", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showDemoConfirm = false }
        )
    }

    if (showRemoveDemoConfirm) {
        ConfirmDialog(
            title = "Remove demo data?",
            message = "All sample transactions, budgets, loans, bills and cards will be deleted. Your categories stay in place.",
            confirmLabel = "Remove",
            onConfirm = {
                vm.removeDemoData()
                showRemoveDemoConfirm = false
                Toast.makeText(context, "Demo data removed", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showRemoveDemoConfirm = false }
        )
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "Clear all financial data?",
            message = "Every transaction, budget, loan, bill and card will be permanently deleted. Categories and your preferences stay. This cannot be undone.",
            confirmLabel = "Clear everything",
            onConfirm = {
                vm.clearAllData()
                showClearConfirm = false
                Toast.makeText(context, "Financial data cleared", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalAppColors.current.muted,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    SurfaceCard(padding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
        content()
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        color = LocalAppColors.current.line,
        modifier = Modifier.padding(start = 52.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    showChevron: Boolean = true
) {
    val app = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = if (app.isDark) 0.22f else 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = app.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onClick != null && showChevron) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = app.muted,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val app = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (app.isDark) 0.22f else 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = app.muted
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(value) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (value == selected) {
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (value != options.last().first) {
                        HorizontalDivider(color = LocalAppColors.current.line)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ProfileDialog(
    name: String,
    avatarId: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var nameValue by remember { mutableStateOf(name) }
    var avatarValue by remember { mutableStateOf(avatarId.ifBlank { avatarPresets.first().id }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your profile") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(
                    initials = initialsOf(nameValue),
                    avatarId = avatarValue,
                    size = 72.dp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Choose a profile photo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                avatarPresets.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { preset ->
                            val selected = preset.id == avatarValue
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .clickable { avatarValue = preset.id }
                                    .then(
                                        if (selected) {
                                            Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                        } else {
                                            Modifier.border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                CircleShape
                                            )
                                        }
                                    )
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Avatar(initials = "", avatarId = preset.id, size = 48.dp)
                            }
                        }
                        if (row.size < 4) {
                            repeat(4 - row.size) { Spacer(Modifier.size(56.dp)) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = nameValue,
                    onValueChange = { nameValue = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(nameValue, avatarValue) },
                enabled = nameValue.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

@Composable
private fun PinDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = "Use four digits. The PIN is hashed and stored only on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAppColors.current.muted
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        pin = value.filter { it.isDigit() }.take(4)
                        error = null
                    },
                    label = { Text("New PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { value ->
                        confirmPin = value.filter { it.isDigit() }.take(4)
                        error = null
                    },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = when {
                        pin.length != 4 -> "PIN must be exactly 4 digits"
                        pin != confirmPin -> "PINs don't match"
                        else -> null
                    }
                    if (error == null) onConfirm(pin)
                }
            ) { Text("Save PIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private suspend fun shareCsv(context: Context, csv: String) {
    withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "expenso-export-${LocalDate.now()}.csv")
        file.writeText(csv)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Expenso export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Expenso export"))
        }
    }
}
