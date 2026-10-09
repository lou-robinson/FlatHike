package com.flathike.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.flathike.app.model.TrackWaypoint
import java.util.Locale

/**
 * Dialog for adding a new intermediate waypoint along the track.
 */
@Composable
fun AddWaypointDialog(
    maxDistanceKm: Double,
    onDismiss: () -> Unit,
    onSave: (name: String, distanceKm: Double, description: String?) -> Unit,
    appStrings: AppStrings
) {
    var name by remember { mutableStateOf("") }
    var distanceStr by remember { mutableStateOf(String.format(Locale.US, "%.1f", maxDistanceKm / 2.0)) }
    var description by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = appStrings.addWaypoint,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = { Text(appStrings.waypointName) },
                    placeholder = { Text("Привал / Родник / Перевал") },
                    isError = isError && name.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = distanceStr,
                    onValueChange = { distanceStr = it },
                    label = { Text("${appStrings.waypointDistanceKm} [0 .. ${String.format(Locale.US, "%.1f", maxDistanceKm)}]") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(appStrings.waypointDesc) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isBlank()) {
                        isError = true
                        return@TextButton
                    }
                    val distKm = distanceStr.trim().replace(',', '.').toDoubleOrNull() ?: (maxDistanceKm / 2.0)
                    val clampedDist = distKm.coerceIn(0.0, maxDistanceKm)
                    onSave(trimmedName, clampedDist, description.trim().ifBlank { null })
                }
            ) {
                Text(appStrings.save, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(appStrings.cancel)
            }
        }
    )
}
