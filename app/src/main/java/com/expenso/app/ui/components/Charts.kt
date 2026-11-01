package com.expenso.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expenso.app.data.Money
import com.expenso.app.ui.theme.LocalAppColors
import kotlin.math.roundToInt

data class DonutSlice(
    val label: String,
    val value: Long,
    val color: Color
)

@Composable
fun SpendingBarChart(
    values: List<Long>,
    labels: List<String>,
    currencyCode: String,
    modifier: Modifier = Modifier,
    highlightLast: Boolean = true,
    dateLabels: List<String> = emptyList()
) {
    val app = LocalAppColors.current
    val primary = MaterialTheme.colorScheme.primary
    val animate by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "barGrow"
    )
    val maxValue = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val description = values.indices.joinToString(", ") { "${labels.getOrElse(it) { "" }} ${Money.format(values[it], currencyCode)}" }
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .semantics { contentDescription = "Spending chart: $description" }
        ) {
            val count = values.size.coerceAtLeast(1)
            // One equal-width slot per bar so each bar sits exactly above its own label.
            val slot = size.width / count
            val barWidth = (slot * 0.38f)
                .coerceAtMost(22.dp.toPx())
                .coerceAtLeast(6.dp.toPx())
            val baseline = size.height - 1.dp.toPx()
            // Subtle horizontal guides, same treatment as the analytics trend chart.
            for (i in 1..3) {
                val y = baseline - (size.height * 0.26f * i)
                drawLine(
                    color = app.line.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            values.forEachIndexed { index, value ->
                val ratio = (value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
                val height = ((size.height * 0.84f) * ratio * animate).coerceAtLeast(4.dp.toPx())
                val centerX = slot * index + slot / 2f
                val isHighlighted = highlightLast && index == values.lastIndex
                val color = if (isHighlighted) primary else primary.copy(alpha = if (app.isDark) 0.34f else 0.22f)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(centerX - barWidth / 2f, baseline - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            values.indices.forEach { index ->
                val isHighlighted = highlightLast && index == values.lastIndex
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = labels.getOrElse(index) { "" },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isHighlighted) primary else app.muted,
                        fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    if (dateLabels.isNotEmpty()) {
                        Text(
                            text = dateLabels.getOrElse(index) { "" },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (isHighlighted) primary else app.muted.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetTrendChart(
    cumulative: List<Long>,
    monthDays: Int,
    budgetCents: Long,
    color: Color,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val app = LocalAppColors.current
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "budgetReveal"
    )
    val days = monthDays.coerceAtLeast(2)
    val maxValue = maxOf(budgetCents, cumulative.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val axisDays = remember(days) {
        listOf(1, (days + 3) / 4, (days + 1) / 2, (3 * days + 2) / 4, days)
            .distinct()
            .filter { it in 1..days }
    }
    val spent = cumulative.lastOrNull() ?: 0L
    val description = "Budget trend: ${Money.format(spent, currencyCode)} spent of ${Money.format(budgetCents, currencyCode)}"

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .semantics { contentDescription = description }
        ) {
            val rightInset = 12.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = size.height - 2.dp.toPx()
            val chartWidth = size.width - rightInset
            if (chartWidth <= 0f) return@Canvas
            fun xFor(day: Int) = chartWidth * ((day - 1).toFloat() / (days - 1).toFloat())
            fun yFor(value: Long) = bottom - (bottom - top) * (value.toFloat() / maxValue.toFloat())

            // Dashed target line for the monthly budget.
            if (budgetCents > 0) {
                val y = yFor(budgetCents)
                drawLine(
                    color = app.muted.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()), 0f)
                )
            }
            if (cumulative.isEmpty()) return@Canvas

            val points = cumulative.mapIndexed { index, value -> Offset(xFor(index + 1), yFor(value)) }
            clipRect(left = 0f, top = 0f, right = chartWidth * reveal, bottom = size.height) {
                if (points.size == 1) {
                    drawCircle(color = color, radius = 3.dp.toPx(), center = points.first())
                } else {
                    val line = Path()
                    line.moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p = points[i]
                        val q = points[i + 1]
                        val midX = (p.x + q.x) / 2f
                        line.cubicTo(midX, p.y, midX, q.y, q.x, q.y)
                    }
                    val area = Path().apply {
                        addPath(line)
                        lineTo(points.last().x, bottom)
                        lineTo(points.first().x, bottom)
                        close()
                    }
                    drawPath(
                        path = area,
                        brush = Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.02f)),
                            startY = top,
                            endY = bottom
                        )
                    )
                    drawPath(
                        path = line,
                        color = color,
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            // Marker on the latest day.
            val last = points.last()
            drawCircle(color = Color.White, radius = 4.6.dp.toPx(), center = last)
            drawCircle(
                color = color,
                radius = 4.6.dp.toPx(),
                center = last,
                style = Stroke(width = 2.2.dp.toPx())
            )
        }
        Spacer(Modifier.height(6.dp))
        val density = LocalDensity.current
        Layout(
            content = {
                axisDays.forEach { day ->
                    Text(
                        text = day.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = app.muted,
                        maxLines = 1
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(Constraints()) }
            val width = constraints.maxWidth
            val labelHeight = placeables.maxOfOrNull { it.height } ?: 0
            val rightInset = with(density) { 12.dp.toPx() }
            layout(width, labelHeight) {
                placeables.forEachIndexed { index, placeable ->
                    val day = axisDays[index]
                    val fraction = (day - 1).toFloat() / (days - 1).toFloat()
                    val centerX = (width - rightInset) * fraction
                    val x = (centerX - placeable.width / 2f).roundToInt()
                        .coerceIn(0, (width - placeable.width).coerceAtLeast(0))
                    placeable.place(x, 0)
                }
            }
        }
    }
}

@Composable
fun MonthlyTrendChart(
    months: List<String>,
    income: List<Long>,
    expense: List<Long>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val app = LocalAppColors.current
    val incomeColor = app.income
    val expenseColor = MaterialTheme.colorScheme.primary
    val animate by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "trendGrow"
    )
    val maxValue = ((income + expense).maxOrNull() ?: 0L).coerceAtLeast(1L)
    val description = months.indices.joinToString(", ") {
        "${months[it]}: income ${Money.format(income.getOrElse(it) { 0 }, currencyCode)}, expenses ${Money.format(expense.getOrElse(it) { 0 }, currencyCode)}"
    }
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
                .semantics { contentDescription = "Monthly trend chart: $description" }
        ) {
            val count = months.size.coerceAtLeast(1)
            val groupWidth = size.width / count
            val barWidth = (groupWidth * 0.22f).coerceAtMost(18.dp.toPx())
            val gap = 4.dp.toPx()
            val baseline = size.height - 1.dp.toPx()
            // Subtle horizontal guides.
            for (i in 1..3) {
                val y = baseline - (size.height * 0.25f * i)
                drawLine(
                    color = app.line.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            months.indices.forEach { index ->
                val centerX = groupWidth * index + groupWidth / 2f
                val incomeHeight = (size.height * 0.86f) * (income.getOrElse(index) { 0 }.toFloat() / maxValue) * animate
                val expenseHeight = (size.height * 0.86f) * (expense.getOrElse(index) { 0 }.toFloat() / maxValue) * animate
                drawRoundRect(
                    color = incomeColor.copy(alpha = if (app.isDark) 0.75f else 0.85f),
                    topLeft = Offset(centerX - barWidth - gap / 2, baseline - incomeHeight.coerceAtLeast(2f)),
                    size = Size(barWidth, incomeHeight.coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(centerX + gap / 2, baseline - expenseHeight.coerceAtLeast(2f)),
                    size = Size(barWidth, expenseHeight.coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            months.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = app.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendDot(color = incomeColor, label = "Income")
            LegendDot(color = expenseColor, label = "Expenses")
        }
    }
}

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 22.dp,
    center: @Composable () -> Unit
) {
    val app = LocalAppColors.current
    val animate by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = "donutSweep"
    )
    val total = slices.sumOf { it.value }.coerceAtLeast(1L)
    val description = slices.joinToString(", ") { "${it.label} ${it.value * 100 / total}%" }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = "Category breakdown: $description" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = ringWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = app.line.copy(alpha = if (app.isDark) 0.8f else 0.55f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            var start = -90f
            slices.forEach { slice ->
                val rawSweep = 360f * (slice.value.toFloat() / total.toFloat())
                val sweep = (rawSweep - 3f).coerceAtLeast(0.8f) * animate
                if (slice.value > 0) {
                    drawArc(
                        color = slice.color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                start += rawSweep
            }
        }
        center()
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color? = null,
    strokeWidth: Dp = 9.dp,
    center: @Composable () -> Unit = {}
) {
    val app = LocalAppColors.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "ringProgress"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            drawArc(
                color = trackColor ?: app.line.copy(alpha = 0.75f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        center()
    }
}

@Composable
fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalAppColors.current.muted
        )
    }
}

@Composable
fun MiniProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp
) {
    val app = LocalAppColors.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "miniProgress"
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        drawRoundRect(
            color = app.line.copy(alpha = 0.8f),
            size = size,
            cornerRadius = CornerRadius(size.height / 2, size.height / 2)
        )
        drawRoundRect(
            color = color,
            size = Size(size.width * animated, size.height),
            cornerRadius = CornerRadius(size.height / 2, size.height / 2)
        )
    }
}
