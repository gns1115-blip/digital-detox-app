package com.example.instagramdetector.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.instagramdetector.detox.DetoxPrefs
import com.example.instagramdetector.util.canDrawOverlays
import com.example.instagramdetector.util.createManageOverlayPermissionIntent
import com.example.instagramdetector.util.isAppAccessibilityServiceEnabled
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToInfo: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isAccessibilityEnabled by remember { mutableStateOf(context.isAppAccessibilityServiceEnabled()) }
    var canDrawOverlay by remember { mutableStateOf(context.canDrawOverlays()) }
    var detoxToggleEnabled by rememberSaveable { mutableStateOf(DetoxPrefs.getUserToggleEnabled(context)) }
    
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        canDrawOverlay = context.canDrawOverlays()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("설정", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SectionHeader("보호 모드 설정")
            
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Digital Detox 보호", color = Color.White, fontWeight = FontWeight.Medium)
                        Text(if (detoxToggleEnabled) "작동 중" else "중단됨", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = detoxToggleEnabled,
                        onCheckedChange = {
                            detoxToggleEnabled = it
                            DetoxPrefs.setUserToggleEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.Gray
                        )
                    )
                }
            }

            SectionHeader("권한 관리")
            
            PermissionRow(
                title = "접근성 서비스",
                isEnabled = isAccessibilityEnabled,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            )

            PermissionRow(
                title = "다른 앱 위에 표시",
                isEnabled = canDrawOverlay,
                onClick = {
                    overlayPermissionLauncher.launch(context.createManageOverlayPermissionIntent())
                }
            )

            SectionHeader("도움말 및 기록")
            
            OutlinedButton(
                onClick = onNavigateToHistory,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("최근 사용 기록 보기")
            }

            OutlinedButton(
                onClick = onNavigateToInfo,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("기능 안내 및 도움말")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        content = { content() }
    )
}

@Composable
fun PermissionRow(title: String, isEnabled: Boolean, onClick: () -> Unit) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color.White)
            if (isEnabled) {
                Text("활성화됨", color = Color.Gray, fontSize = 14.sp)
            } else {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("설정하기", fontSize = 12.sp)
                }
            }
        }
    }
}
