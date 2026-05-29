package com.example.instagramdetector.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.instagramdetector.detox.DetoxPrefs
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    onNavigateToApps: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val time by viewModel.currentTime.collectAsStateWithLifecycle()
    val battery by viewModel.batteryLevel.collectAsStateWithLifecycle()
    val stats by viewModel.dailyStats.collectAsStateWithLifecycle(initialValue = null)
    
    val isDetoxEnabled = remember(time) { DetoxPrefs.isProtectionEnabled(context) }

    val utilities = listOf(
        UtilityItem("전화", Icons.Default.Phone) { launchIntent(context, Intent(Intent.ACTION_DIAL)) },
        UtilityItem("메시지", Icons.Default.Email) { launchIntent(context, context.packageManager.getLaunchIntentForPackage("com.google.android.apps.messaging") ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)) },
        UtilityItem("카메라", Icons.Default.CameraAlt) { launchIntent(context, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) },
        UtilityItem("갤러리", Icons.Default.PhotoLibrary) { launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse("content://media/internal/images/media"))) },
        UtilityItem("달력", Icons.Default.DateRange) { launchIntent(context, Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI)) },
        UtilityItem("시계", Icons.Default.Alarm) { launchIntent(context, Intent(AlarmClock.ACTION_SHOW_ALARMS)) },
        UtilityItem("계산기", Icons.Default.Calculate) { launchIntent(context, context.packageManager.getLaunchIntentForPackage("com.google.android.calculator") ?: context.packageManager.getLaunchIntentForPackage("com.android.calculator2") ?: Intent()) },
        UtilityItem("설정", Icons.Default.Settings) { onNavigateToSettings() }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Clock
        Text(
            text = viewModel.getFormattedTime(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 80.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )
        )
        Text(
            text = viewModel.getFormattedDate(),
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )
        
        Text(
            text = if (isDetoxEnabled) "● 디톡스 보호 중" else "○ 보호 중단됨",
            color = if (isDetoxEnabled) Color(0xFF81C784) else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        // Battery and Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoChip(label = "배터리", value = "$battery%", icon = Icons.Default.BatteryFull, modifier = Modifier.weight(1f))
            stats?.let {
                InfoChip(label = "집중 스트릭", value = "${it.focusStreak}일", icon = Icons.Default.LocalFireDepartment, modifier = Modifier.weight(1f))
                val mins = TimeUnit.MILLISECONDS.toMinutes(it.todayUsageTimeMs)
                InfoChip(label = "오늘 사용", value = "${mins}분", icon = Icons.Default.Timer, modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Utilities Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(utilities) { item ->
                UtilityIcon(item)
            }
        }

        // Apps Button (Friction)
        Button(
            onClick = onNavigateToApps,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1A1A1A),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Apps, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("애플리케이션 목록", fontWeight = FontWeight.Medium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun UtilityIcon(item: UtilityItem) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { item.onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = Color.LightGray,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = item.label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun InfoChip(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(Color(0xFF111111), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(label, color = Color.DarkGray, fontSize = 10.sp)
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

data class UtilityItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

private fun launchIntent(context: Context, intent: Intent) {
    try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback or Toast
    }
}
