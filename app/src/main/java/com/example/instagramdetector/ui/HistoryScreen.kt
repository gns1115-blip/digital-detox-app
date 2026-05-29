package com.example.instagramdetector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.instagramdetector.datastore.OverlaySelectionRecord
import com.example.instagramdetector.detection.MonitoredApps
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    records: List<OverlaySelectionRecord>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("최근 기록", color = Color.White) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("뒤로", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
    ) { innerPadding ->
        if (records.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "저장된 기록이 없습니다.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.LightGray,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(records) { index, record ->
                    HistoryRecordCard(
                        index = index + 1,
                        record = record,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    index: Int,
    record: OverlaySelectionRecord,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "#$index  ${record.selectedReason.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = formatTimestamp(record.timestamp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )
            Text(
                text = formatPackageLabel(record.packageName),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
            )
        }
    }
}

private fun formatTimestamp(timestampMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
        .format(Date(timestampMillis))

private fun formatPackageLabel(packageName: String): String =
    when (packageName) {
        MonitoredApps.INSTAGRAM -> "Instagram"
        MonitoredApps.INSTAGRAM_LITE -> "Instagram Lite"
        MonitoredApps.YOUTUBE -> "YouTube"
        else -> packageName
    }
