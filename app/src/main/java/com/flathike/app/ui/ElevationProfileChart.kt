package com.flathike.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flathike.app.model.ElevationProfilePoint
import com.flathike.app.model.TrackWaypoint
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * High-performance Canvas-based elevation profile chart for hiking tracks,
 * featuring interactive intermediate waypoints, vertical guideline markers,
 * and elevation range annotations.
 */
@Composable
fun ElevationProfileChart(
    points: List<ElevationProfilePoint>,
    waypoints: List<TrackWaypoint> = emptyList(),
    selectedWaypoint: TrackWaypoint? = null,
    onWaypointSelected: ((TrackWaypoint?) -> Unit)? = null,
    appStrings: AppStrings? = null,
    modifier: Modifier = Modifier
) {
    var activeWaypoint by remember(selectedWaypoint) { mutableStateOf(selectedWaypoint) }

    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appStrings?.noElevationData ?: "Нет данных высотного профиля",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxDist = max(0.01, points.maxOf { it.distanceKm })
    val minEle = points.minOf { it.elevationMeters }
    val maxEle = points.maxOf { it.elevationMeters }
    val eleRange = max(10.0, maxEle - minEle)

    val chartLineColor = MaterialTheme.colorScheme.primary
    val gradientTopColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val gradientBottomColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val waypointMarkerColor = MaterialTheme.colorScheme.tertiary
    val waypointSelectedColor = MaterialTheme.colorScheme.error

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        // Chart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appStrings?.elevationProfile ?: "Высотный профиль",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (waypoints.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "📍 ${waypoints.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "▲ ${maxEle.toInt()} м  ▼ ${minEle.toInt()} м",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Canvas Elevation Graph with Waypoints
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .pointerInput(points, waypoints) {
                    detectTapGestures { tapOffset ->
                        if (waypoints.isEmpty()) return@detectTapGestures
                        val width = size.width
                        val clickedWpt = waypoints.minByOrNull { wpt ->
                            val wptX = ((wpt.distanceKm / maxDist).coerceIn(0.0, 1.0)).toFloat() * width
                            abs(wptX - tapOffset.x)
                        }
                        if (clickedWpt != null) {
                            val wptX = ((clickedWpt.distanceKm / maxDist).coerceIn(0.0, 1.0)).toFloat() * width
                            if (abs(wptX - tapOffset.x) < 48.dp.toPx()) {
                                activeWaypoint = if (activeWaypoint == clickedWpt) null else clickedWpt
                                onWaypointSelected?.invoke(activeWaypoint)
                            }
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val paddingTop = 16f
            val paddingBottom = 16f
            val usableHeight = height - paddingBottom
            val chartRangeHeight = usableHeight - paddingTop

            // Draw horizontal grid lines (min, mid, max)
            val lineYMid = paddingTop + chartRangeHeight * 0.5f
            drawLine(
                color = gridColor,
                start = Offset(0f, paddingTop),
                end = Offset(width, paddingTop),
                strokeWidth = 1f
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, lineYMid),
                end = Offset(width, lineYMid),
                strokeWidth = 1f
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, usableHeight),
                end = Offset(width, usableHeight),
                strokeWidth = 1f
            )

            // Build path for profile line and fill
            val path = Path()
            val fillPath = Path()

            points.forEachIndexed { index, pt ->
                val x = (pt.distanceKm / maxDist).toFloat() * width
                val normalizedY = ((pt.elevationMeters - minEle) / eleRange).toFloat()
                val y = usableHeight - (normalizedY * chartRangeHeight)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, usableHeight)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                if (index == points.size - 1) {
                    fillPath.lineTo(x, usableHeight)
                    fillPath.close()
                }
            }

            // Draw gradient fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(gradientTopColor, gradientBottomColor),
                    startY = 0f,
                    endY = usableHeight
                )
            )

            // Draw elevation line
            drawPath(
                path = path,
                color = chartLineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Waypoints on the graph
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            waypoints.forEachIndexed { idx, wpt ->
                val wptX = ((wpt.distanceKm / maxDist).coerceIn(0.0, 1.0)).toFloat() * width
                val normY = ((wpt.elevation - minEle) / eleRange).coerceIn(0.0, 1.0).toFloat()
                val wptY = usableHeight - (normY * chartRangeHeight)
                val isSelected = (activeWaypoint == wpt)

                val markerColor = if (isSelected) waypointSelectedColor else waypointMarkerColor
                val markerRadius = if (isSelected) 7.dp.toPx() else 4.5.dp.toPx()

                // Vertical indicator dashed line from point to base
                drawLine(
                    color = markerColor.copy(alpha = if (isSelected) 0.9f else 0.5f),
                    start = Offset(wptX, wptY),
                    end = Offset(wptX, usableHeight),
                    strokeWidth = if (isSelected) 2.dp.toPx() else 1.2.dp.toPx(),
                    pathEffect = dashEffect
                )

                // Outer circle pin
                drawCircle(
                    color = markerColor,
                    radius = markerRadius,
                    center = Offset(wptX, wptY)
                )

                // Inner white center
                drawCircle(
                    color = Color.White,
                    radius = markerRadius * 0.45f,
                    center = Offset(wptX, wptY)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Axis Distance Labels
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "0.0 км (${points.first().elevationMeters.toInt()}м)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${String.format(Locale.US, "%.1f", maxDist)} км (${points.last().elevationMeters.toInt()}м)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Horizontal Waypoint Chips
        if (waypoints.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                waypoints.forEachIndexed { idx, wpt ->
                    val isSelected = (activeWaypoint == wpt)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            activeWaypoint = if (isSelected) null else wpt
                            onWaypointSelected?.invoke(activeWaypoint)
                        },
                        label = {
                            Text(
                                text = "${idx + 1}. ${wpt.name} (${String.format(Locale.US, "%.1f", wpt.distanceKm)} км)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.tertiary
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
            }
        }

        // Selected Waypoint Detail Card
        AnimatedVisibility(visible = activeWaypoint != null) {
            activeWaypoint?.let { wpt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = wpt.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.2f", wpt.distanceKm)} км • Высота ${wpt.elevation.toInt()} м" +
                                        (wpt.description?.let { " • $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
