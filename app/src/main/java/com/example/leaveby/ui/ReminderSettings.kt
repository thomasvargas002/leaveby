@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.leaveby.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderSettingsSection(
    enabled: Boolean,
    days: Set<Int>,                 // 1..7 (Mon..Sun)
    time: LocalTime,
    toggleEnabled: (Boolean) -> Unit,
    toggleDay: (Int) -> Unit,
    onPickTime: (LocalTime) -> Unit
) {
    val ctx = LocalContext.current
    val labels = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")

    Column(Modifier.padding(16.dp)) {

        // Enable row
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = toggleEnabled)
        }

        Spacer(Modifier.height(12.dp))
        Text("Days", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))

        // Wrap chips using FlowRow
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 1..7) {
                FilterChip(
                    selected = i in days,
                    onClick = { toggleDay(i) },
                    label = { Text(labels[i - 1]) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Time", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))

        Button(onClick = {
            TimePickerDialog(
                ctx,
                { _, hh, mm -> onPickTime(LocalTime.of(hh, mm)) },
                time.hour,
                time.minute,
                false
            ).show()
        }) {
            val h12 = (time.hour % 12).let { if (it == 0) 12 else it }
            val ampm = if (time.hour < 12) "AM" else "PM"
            val display = String.format(Locale.getDefault(), "%02d:%02d %s", h12, time.minute, ampm)
            Text(display)
        }
    }
}
