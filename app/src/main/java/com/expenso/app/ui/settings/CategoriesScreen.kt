package com.expenso.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenso.app.ui.components.CategoryAvatar
import com.expenso.app.ui.components.ConfirmDialog
import com.expenso.app.ui.components.DetailTopBar
import com.expenso.app.ui.components.EmptyState
import com.expenso.app.ui.components.FormField
import com.expenso.app.ui.components.SegmentedControl
import com.expenso.app.ui.components.SurfaceCard
import com.expenso.app.ui.components.categoryIcon
import com.expenso.app.ui.components.parseHexColor
import com.expenso.app.ui.theme.LocalAppColors

private val iconChoices = listOf(
    "category", "restaurant", "groceries", "dining", "transport", "shopping",
    "entertainment", "bills", "health", "education", "travel", "loans",
    "credit_card", "salary", "freelance", "investments", "gifts", "income"
)

private val colorChoices = listOf(
    "#625BFF", "#8B5CF6", "#EC4899", "#EF4444", "#F97316", "#F59E0B",
    "#84CC16", "#16A36A", "#14B8A6", "#06B6D4", "#3B82F6", "#94A3B8"
)

@Composable
fun CategoriesScreen(
    vm: CategoriesViewModel,
    onBack: () -> Unit
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val app = LocalAppColors.current
    val context = LocalContext.current

    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CategoryRow?>(null) }
    var selectedType by remember { mutableStateOf("EXPENSE") }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.clearMessage()
        }
    }

    val visibleRows = state.rows
        .filter { it.category.type == selectedType }
        .sortedWith(compareByDescending<CategoryRow> { it.transactionCount }.thenBy { it.category.name })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        DetailTopBar(
            title = "Categories",
            subtitle = "Organise where your money goes",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showAdd = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add category",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 30.dp)
        ) {
            item {
                SurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(app.indigoSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Your categories",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${state.expenseCount} expense · ${state.incomeCount} income",
                                style = MaterialTheme.typography.bodySmall,
                                color = app.muted
                            )
                        }
                        Text(
                            text = "${state.rows.size}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                SegmentedControl(
                    options = listOf("EXPENSE", "INCOME"),
                    selected = selectedType,
                    label = { if (it == "INCOME") "Income" else "Expense" },
                    onSelect = { selectedType = it }
                )
                Spacer(Modifier.height(16.dp))
            }

            if (visibleRows.isEmpty()) {
                item {
                    SurfaceCard {
                        EmptyState(
                            icon = Icons.Filled.Category,
                            title = "No categories yet",
                            message = "Add a category to keep your transactions organised.",
                            actionText = "Add category",
                            onAction = { showAdd = true }
                        )
                    }
                }
            } else {
                items(visibleRows, key = { it.category.id }) { row ->
                    CategoryRowItem(
                        row = row,
                        onDelete = { pendingDelete = row }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Deleting a category removes its budget. Existing transactions keep their historical label.",
                    style = MaterialTheme.typography.bodySmall,
                    color = app.muted
                )
            }
        }
    }

    if (showAdd) {
        AddCategoryDialog(
            initialType = selectedType,
            onDismiss = { showAdd = false },
            onSave = { name, type, iconKey, colorHex ->
                vm.createCategory(name, type, iconKey, colorHex)
                showAdd = false
            }
        )
    }

    pendingDelete?.let { row ->
        ConfirmDialog(
            title = "Delete ${row.category.name}?",
            message = buildString {
                append("This category will be removed. ")
                if (row.transactionCount > 0) {
                    append("${row.transactionCount} existing transaction")
                    if (row.transactionCount != 1) append("s")
                    append(" will keep the label. ")
                }
                if (row.hasBudget) append("Its monthly budget will also be deleted.")
            }.trim(),
            onConfirm = {
                vm.deleteCategory(row.category)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun CategoryRowItem(
    row: CategoryRow,
    onDelete: () -> Unit
) {
    val app = LocalAppColors.current
    SurfaceCard(padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(
                name = row.category.name,
                colorHex = row.category.colorHex,
                size = 44.dp,
                iconKey = row.category.iconKey
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.category.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (row.category.isDefault) {
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "Built-in",
                            style = MaterialTheme.typography.labelSmall,
                            color = app.muted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(app.surfaceRaised)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(
                            if (row.transactionCount == 0) "No transactions yet"
                            else "${row.transactionCount} transaction" + if (row.transactionCount == 1) "" else "s"
                        )
                        if (row.hasBudget) append(" · Budget set")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = app.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete ${row.category.name}",
                    tint = app.muted,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    initialType: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    val app = LocalAppColors.current
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(initialType) }
    var iconKey by remember { mutableStateOf("category") }
    var colorHex by remember { mutableStateOf(colorChoices.first()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New category") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Choose a name, an icon and a colour.",
                    style = MaterialTheme.typography.bodySmall,
                    color = app.muted
                )
                Spacer(Modifier.height(12.dp))
                FormField(
                    label = "Category name",
                    value = name,
                    onValueChange = {
                        name = it.take(28)
                        error = null
                    },
                    placeholder = "e.g. Subscriptions",
                    supporting = error
                )
                Spacer(Modifier.height(12.dp))
                SegmentedControl(
                    options = listOf("EXPENSE", "INCOME"),
                    selected = type,
                    label = { if (it == "INCOME") "Income" else "Expense" },
                    onSelect = { type = it }
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = app.muted
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(iconChoices, key = { it }) { key ->
                        val selected = key == iconKey
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (selected) parseHexColor(colorHex).copy(alpha = 0.16f) else app.surfaceRaised)
                                .border(
                                    width = if (selected) 1.5.dp else 0.dp,
                                    color = if (selected) parseHexColor(colorHex) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { iconKey = key }
                                .padding(9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = categoryIcon(key),
                                contentDescription = key.replace('_', ' '),
                                tint = if (selected) parseHexColor(colorHex) else app.muted
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Colour",
                    style = MaterialTheme.typography.labelMedium,
                    color = app.muted
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(colorChoices, key = { it }) { hex ->
                        val selected = hex == colorHex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(
                                    width = if (selected) 2.5.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable(role = Role.RadioButton) { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = when {
                        name.isBlank() -> "Enter a category name"
                        name.trim().length < 2 -> "Use at least 2 characters"
                        else -> null
                    }
                    if (error == null) onSave(name.trim(), type, iconKey, colorHex)
                }
            ) { Text("Add category") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(24.dp)
    )
}
