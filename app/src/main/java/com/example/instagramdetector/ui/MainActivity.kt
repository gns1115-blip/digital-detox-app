package com.example.instagramdetector.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.instagramdetector.InstagramDetectionState
import com.example.instagramdetector.InstagramDetectorApplication
import com.example.instagramdetector.detox.DetoxPrefs
import com.example.instagramdetector.util.canDrawOverlays
import com.example.instagramdetector.util.createManageOverlayPermissionIntent
import com.example.instagramdetector.util.isAppAccessibilityServiceEnabled
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

private enum class AppScreen {
    Home,
    History,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    val context = LocalContext.current
    val app = context.applicationContext as InstagramDetectorApplication
    val recentRecords by app.overlaySelectionRepository.recentRecords
        .collectAsStateWithLifecycle(initialValue = emptyList())

    when (currentScreen) {
        AppScreen.Home -> InstagramDetectorScreen(
            onOpenHistory = { currentScreen = AppScreen.History },
        )
        AppScreen.History -> HistoryScreen(
            records = recentRecords,
            onBack = { currentScreen = AppScreen.Home },
        )
    }
}

@Composable
private fun InstagramDetectorScreen(
    onOpenHistory: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val app = context.applicationContext as InstagramDetectorApplication

    var isAccessibilityEnabled by remember {
        mutableStateOf(context.isAppAccessibilityServiceEnabled())
    }
    var canDrawOverlay by remember { mutableStateOf(context.canDrawOverlays()) }
    var hasNotificationPermission by remember {
        mutableStateOf(isNotificationPermissionGranted(context))
    }

    val dailyStats by app.appStatsRepository.dailyStats
        .collectAsStateWithLifecycle(
            initialValue = com.example.instagramdetector.datastore.DailyAppStats(
                todayDate = "",
                todayUsageTimeMs = 0L,
                blockCount = 0,
                lastBlockAtMillis = null,
            ),
        )

    val detectionCount by InstagramDetectionState.detectionCount.collectAsStateWithLifecycle()
    val lastDetectedAtMillis by InstagramDetectionState.lastDetectedAtMillis.collectAsStateWithLifecycle()
    val lastDetectedPackage by InstagramDetectionState.lastDetectedPackage.collectAsStateWithLifecycle()
    val lastSelectedReason by InstagramDetectionState.lastSelectedReason.collectAsStateWithLifecycle()
    val lastReasonSelectedAtMillis by InstagramDetectionState.lastReasonSelectedAtMillis.collectAsStateWithLifecycle()

    var detoxToggleEnabled by rememberSaveable {
        mutableStateOf(DetoxPrefs.getUserToggleEnabled(context))
    }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        canDrawOverlay = context.canDrawOverlays()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        hasNotificationPermission = isNotificationPermissionGranted(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = context.isAppAccessibilityServiceEnabled()
                canDrawOverlay = context.canDrawOverlays()
                hasNotificationPermission = isNotificationPermissionGranted(context)
                detoxToggleEnabled = DetoxPrefs.getUserToggleEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val protectionEnabled = DetoxPrefs.isProtectionEnabled(context, now = nowMs)
    val remainingDisableMs = DetoxPrefs.remainingTempDisableMs(context, now = nowMs)
    val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingDisableMs)
    val remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingDisableMs) % 60

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "앱 사용 관리",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Digital Detox Enabled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Switch(
                            checked = detoxToggleEnabled,
                            onCheckedChange = { checked ->
                                detoxToggleEnabled = checked
                                DetoxPrefs.setUserToggleEnabled(context, checked)
                                if (!checked) {
                                    DetoxPrefs.clearTemporaryDisable(context)
                                }
                            },
                        )
                    }

                    Text(
                        text = if (protectionEnabled) "Protection Enabled" else "Protection Disabled",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (protectionEnabled) Color(0xFF2E7D32) else Color(0xFFC62828),
                    )

                    if (detoxToggleEnabled) {
                        Button(
                            onClick = {
                                DetoxPrefs.disableForMinutes(context, minutes = 10, now = System.currentTimeMillis())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = protectionEnabled,
                        ) {
                            Text("Disable for 10 minutes")
                        }

                        if (!protectionEnabled && remainingDisableMs > 0L) {
                            Text(
                                text = "Re-enables in ${remainingMinutes}m ${remainingSeconds}s",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            ServiceStatusCard(title = "접근성 서비스", isEnabled = isAccessibilityEnabled)
            ServiceStatusCard(title = "다른 앱 위에 표시", isEnabled = canDrawOverlay)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ServiceStatusCard(title = "알림 권한", isEnabled = hasNotificationPermission)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "오늘 통계 (${dailyStats.todayDate})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = "오늘 사용 시간: ${formatDuration(dailyStats.todayUsageTimeMs)}")
                    Text(text = "차단 횟수: ${dailyStats.blockCount}회")
                    Text(
                        text = "마지막 차단: ${formatDetectedTime(dailyStats.lastBlockAtMillis)}",
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "감지 횟수: $detectionCount", style = MaterialTheme.typography.titleMedium)
                    Text(text = "마지막 감지: ${formatDetectedTime(lastDetectedAtMillis)}")
                    Text(text = "마지막 패키지: ${lastDetectedPackage ?: "없음"}")
                    Text(text = "마지막 선택: ${lastSelectedReason?.label ?: "없음"}")
                    Text(text = "선택 시각: ${formatDetectedTime(lastReasonSelectedAtMillis)}")
                }
            }

            OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                Text("최근 기록 보기 (10개)")
            }

            if (!isAccessibilityEnabled) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("접근성 설정 열기")
                }
            }

            if (!canDrawOverlay) {
                Button(
                    onClick = {
                        overlayPermissionLauncher.launch(context.createManageOverlayPermissionIntent())
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("다른 앱 위에 표시 권한 허용")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                OutlinedButton(
                    onClick = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("알림 권한 허용")
                }
            }

            Text(
                text = "Instagram: 실행 이유·사용 시간 선택\n" +
                    "YouTube/Instagram: Reels·Shorts 10분 시청 시 휴식\n" +
                    "사용 시간 종료 시 홈 이동 + 30초 재실행 차단",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ServiceStatusCard(title: String, isEnabled: Boolean) {
    val statusText = if (isEnabled) "$title: 활성화됨" else "$title: 비활성화"
    val statusColor = if (isEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = statusColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun isNotificationPermissionGranted(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun formatDetectedTime(timestampMillis: Long?): String {
    if (timestampMillis == null) return "없음"
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
        .format(Date(timestampMillis))
}

private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return "${minutes}분 ${seconds}초"
}
