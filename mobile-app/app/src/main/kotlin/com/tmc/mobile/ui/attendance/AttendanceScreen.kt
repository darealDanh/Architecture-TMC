package com.tmc.mobile.ui.attendance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tmc.mobile.domain.attendance.AttendanceDay
import com.tmc.mobile.domain.attendance.EntrySource
import com.tmc.mobile.domain.attendance.MonthlyAttendance
import com.tmc.mobile.domain.attendance.Severity
import com.tmc.mobile.ui.components.ChipTone
import com.tmc.mobile.ui.components.StatusChip
import com.tmc.mobile.ui.components.TmcCard
import com.tmc.mobile.ui.theme.TmcColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val dayFormat = DateTimeFormatter.ofPattern("EEE dd", Locale.ENGLISH)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun AttendanceRoute(viewModel: AttendanceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AttendanceScreen(state, onSelect = viewModel::select)
}

@Composable
fun AttendanceScreen(state: AttendanceUiState, onSelect: (YearMonth) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp)) {
            Text("Attendance", style = MaterialTheme.typography.headlineSmall)
            MonthPicker(state.months, state.selected, onSelect)
        }
        when (val content = state.content) {
            AttendanceContent.Loading -> Centered { CircularProgressIndicator() }
            AttendanceContent.NoData -> Centered { Message("No data offline for this month", "Connect to the internet to load it.") }
            is AttendanceContent.Error -> Centered { Message(content.message, null) }
            is AttendanceContent.Loaded -> MonthContent(content.attendance)
        }
    }
}

@Composable
private fun MonthPicker(months: List<YearMonth>, selected: YearMonth, onSelect: (YearMonth) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            "${selected.format(monthFormat)} ▾",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { expanded = true }.padding(vertical = 2.dp),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            months.forEach { month ->
                DropdownMenuItem(
                    text = { Text(month.format(monthFormat)) },
                    onClick = {
                        expanded = false
                        onSelect(month)
                    },
                )
            }
        }
    }
}

@Composable
private fun MonthContent(attendance: MonthlyAttendance) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(attendance.daysWorked.toString(), "Days", Modifier.weight(1f))
                Stat(String.format(Locale.ENGLISH, "%.1f", attendance.totalWorked.toMinutes() / 60.0), "Hours", Modifier.weight(1f))
                Stat(
                    attendance.toReview.toString(),
                    "To review",
                    Modifier.weight(1f),
                    if (attendance.toReview > 0) TmcColors.Error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (attendance.fromCache) {
            item {
                Text(
                    "Showing saved data. Connect to refresh.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        item {
            if (attendance.days.isEmpty()) {
                TmcCard { Message("No attendance yet this month", null, Modifier.padding(24.dp)) }
            } else {
                TmcCard {
                    attendance.days.forEachIndexed { index, day ->
                        if (index > 0) HorizontalDivider(color = TmcColors.Divider)
                        DayRow(day)
                    }
                }
            }
        }
        item { Box(Modifier.height(4.dp)) }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    TmcCard(modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(value, fontSize = 20.sp, lineHeight = 24.sp, color = valueColor, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun DayRow(day: AttendanceDay) {
    val record = day.record
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(record.date.format(dayFormat), style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                detail(day),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (day.flags.isEmpty()) {
            Text(
                "${record.entry?.format(timeFormat)} – ${record.exit?.format(timeFormat)}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                day.flags.forEach { flag ->
                    StatusChip(flag.label, if (flag.severity == Severity.NEEDS_REVIEW) ChipTone.Error else ChipTone.Warning)
                }
            }
        }
    }
}

private fun detail(day: AttendanceDay): String = when {
    day.record.source == EntrySource.MANUAL -> "Entered by guard"
    day.needsReview -> "No exit recorded"
    else -> "Gate ${day.record.gateId}"
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun Message(title: String, body: String?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        body?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
