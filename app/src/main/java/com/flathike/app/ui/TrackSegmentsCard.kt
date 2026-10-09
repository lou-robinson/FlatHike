package com.flathike.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flathike.app.model.SlopeCategory
import com.flathike.app.model.SlopeSpeedSummary
import com.flathike.app.model.TrackCalculationResult
import com.flathike.app.model.TrackSegment
import com.flathike.app.model.TrackSplitMode
import com.flathike.app.model.TrackWaypoint
import java.util.Locale
import kotlin.math.abs

/**
 * Rich interactive card for analyzing speed across terrain slopes and
 * partitioning the GPS track into logical parts (auto by slope, splits, or waypoints).
 */
@Composable
fun TrackSegmentsCard(
    calculationResult: TrackCalculationResult,
    currentSplitMode: TrackSplitMode,
    onSplitModeChange: (TrackSplitMode) -> Unit,
    onAddWaypointClick: () -> Unit,
    appStrings: AppStrings,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val segments = calculationResult.getSegmentsForMode(currentSplitMode)
    val slopeSummaries = calculationResult.slopeSpeedAnalysis

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Title Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appStrings.speedAnalysisTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (calculationResult.hasRecordedTime)
                            "${appStrings.speedVsSlopeDesc} ${appStrings.gpsSource}"
                        else
                            "${appStrings.speedVsSlopeDesc} ${appStrings.toblerSource}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // SECTION 1: Speed vs Slope Summary Grid
                    Text(
                        text = appStrings.speedVsSlopeTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    slopeSummaries.forEach { summary ->
                        SlopeSpeedRow(summary = summary, appStrings = appStrings)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // SECTION 2: Track Partitioning / Splits
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = appStrings.partitionMode,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = onAddWaypointClick,
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(appStrings.addWaypoint, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mode Selection Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = currentSplitMode == TrackSplitMode.BY_SLOPE,
                            onClick = { onSplitModeChange(TrackSplitMode.BY_SLOPE) },
                            label = { Text(appStrings.splitBySlope, style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = currentSplitMode == TrackSplitMode.BY_DISTANCE_1KM,
                            onClick = { onSplitModeChange(TrackSplitMode.BY_DISTANCE_1KM) },
                            label = { Text(appStrings.splitBy1Km, style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = currentSplitMode == TrackSplitMode.BY_DISTANCE_5KM,
                            onClick = { onSplitModeChange(TrackSplitMode.BY_DISTANCE_5KM) },
                            label = { Text(appStrings.splitBy5Km, style = MaterialTheme.typography.labelSmall) }
                        )
                        if (calculationResult.hasWaypoints) {
                            FilterChip(
                                selected = currentSplitMode == TrackSplitMode.BY_WAYPOINTS,
                                onClick = { onSplitModeChange(TrackSplitMode.BY_WAYPOINTS) },
                                label = { Text(appStrings.splitByWaypoints, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // List of Segments
                    if (segments.isEmpty()) {
                        Text(
                            text = if (currentSplitMode == TrackSplitMode.BY_WAYPOINTS)
                                appStrings.noWaypoints
                            else
                                appStrings.notEnoughData,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        segments.forEach { seg ->
                            TrackSegmentRow(segment = seg, appStrings = appStrings)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlopeSpeedRow(
    summary: SlopeSpeedSummary,
    appStrings: AppStrings
) {
    val (label, icon, color) = getSlopeCategoryVisuals(summary.category, appStrings)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
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
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val ascentStr = if (summary.totalAscentMeters > 0) " • ▲+${summary.totalAscentMeters.toInt()}${appStrings.meters}" else ""
                val descentStr = if (summary.totalDescentMeters > 0) " • ▼-${summary.totalDescentMeters.toInt()}${appStrings.meters}" else ""
                Text(
                    text = "${String.format(Locale.US, "%.1f", summary.totalDistanceKm)} ${appStrings.km} (${String.format(Locale.US, "%.0f", summary.percentageOfTrack)}%)$ascentStr$descentStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", summary.avgSpeedKmH)} ${appStrings.kmh}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (summary.isRealGpsSpeed) appStrings.gps else appStrings.tobler,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (summary.isRealGpsSpeed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun TrackSegmentRow(
    segment: TrackSegment,
    appStrings: AppStrings
) {
    val (_, icon, color) = getSlopeCategoryVisuals(segment.slopeCategory, appStrings)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${segment.index}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = segment.getDisplayName(appStrings),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val elevStr = if (segment.elevationChangeM >= 0) "▲+${segment.elevationChangeM.toInt()}${appStrings.meters}" else "▼${segment.elevationChangeM.toInt()}${appStrings.meters}"
                Text(
                    text = "${String.format(Locale.US, "%.2f", segment.distanceKm)} ${appStrings.km} • ${appStrings.slope} ${String.format(Locale.US, "%+.1f", segment.avgSlopePercent)}% $elevStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", segment.avgSpeedKmH)} ${appStrings.kmh}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                val minutes = (segment.durationSeconds / 60.0).toInt()
                Text(
                    text = "$minutes ${appStrings.min}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getSlopeCategoryVisuals(
    category: SlopeCategory,
    appStrings: AppStrings
): Triple<String, ImageVector, Color> {
    return when (category) {
        SlopeCategory.STEEP_ASCENT -> Triple(
            appStrings.steepAscent,
            Icons.Default.ArrowUpward,
            Color(0xFFD32F2F) // Red
        )
        SlopeCategory.MODERATE_ASCENT -> Triple(
            appStrings.moderateAscent,
            Icons.Default.ArrowUpward,
            Color(0xFFE65100) // Deep Orange
        )
        SlopeCategory.FLAT -> Triple(
            appStrings.flatTerrain,
            Icons.Default.TrendingFlat,
            Color(0xFF2E7D32) // Forest Green
        )
        SlopeCategory.MODERATE_DESCENT -> Triple(
            appStrings.moderateDescent,
            Icons.Default.ArrowDownward,
            Color(0xFF0277BD) // Blue
        )
        SlopeCategory.STEEP_DESCENT -> Triple(
            appStrings.steepDescent,
            Icons.Default.ArrowDownward,
            Color(0xFF6A1B9A) // Purple
        )
    }
}
