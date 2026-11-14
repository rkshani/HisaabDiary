package com.hisaab.diary.ui.feature.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hisaab.diary.R
import com.hisaab.diary.domain.usecase.Report
import com.hisaab.diary.ui.components.formatMoney
import com.hisaab.diary.ui.theme.HisaabTheme

/**
 * Simple grouped bar chart drawn with Canvas (no chart library):
 * for each day or month, a red "gave" bar and a green "got" bar.
 */
@Composable
fun ReportChart(report: Report, modifier: Modifier = Modifier) {
    val gaveColor = HisaabTheme.colors.willGive
    val gotColor = HisaabTheme.colors.willGet
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val description = stringResource(
        R.string.chart_description,
        formatMoney(report.totalGave),
        formatMoney(report.totalGot),
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .semantics { contentDescription = description },
        ) {
            val buckets = report.buckets
            if (buckets.isEmpty()) return@Canvas
            val max = buckets.maxOf { maxOf(it.gave.paisa, it.got.paisa) }.coerceAtLeast(1)
            val groupWidth = size.width / buckets.size
            val barWidth = (groupWidth * BAR_SHARE / 2).coerceAtLeast(1f)
            val radius = CornerRadius(barWidth / 4, barWidth / 4)
            buckets.forEachIndexed { index, bucket ->
                val left = index * groupWidth + (groupWidth - barWidth * 2) / 2
                drawBar(left, barWidth, bucket.gave.paisa.toFloat() / max, gaveColor, radius)
                drawBar(left + barWidth, barWidth, bucket.got.paisa.toFloat() / max, gotColor, radius)
            }
            drawLine(axisColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            Legend(color = gaveColor, label = stringResource(R.string.action_you_gave))
            Legend(color = gotColor, label = stringResource(R.string.action_you_got))
        }
    }
}

private fun DrawScope.drawBar(left: Float, width: Float, fraction: Float, color: Color, radius: CornerRadius) {
    if (fraction <= 0f) return
    val height = size.height * fraction
    drawRoundRect(
        color = color,
        topLeft = Offset(left, size.height - height),
        size = Size(width, height),
        cornerRadius = radius,
    )
}

@Composable
private fun Legend(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

private const val BAR_SHARE = 0.7f
