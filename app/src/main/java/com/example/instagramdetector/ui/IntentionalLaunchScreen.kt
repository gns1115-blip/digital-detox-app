package com.example.instagramdetector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.instagramdetector.overlay.UsageDuration
import kotlinx.coroutines.delay

@Composable
fun IntentionalLaunchScreen(
    appName: String,
    onCancel: () -> Unit,
    onConfirm: (UsageDuration) -> Unit
) {
    var countdown by remember { mutableIntStateOf(3) }
    var selectedDuration by remember { mutableStateOf<UsageDuration?>(null) }
    
    val prompts = listOf(
        "정말 이 앱을 열어야 하나요?",
        "지금 이 앱이 꼭 필요한가요?",
        "무엇을 위해 이 앱을 사용하나요?",
        "잠시 멈추고 다시 생각해보세요."
    )
    val randomPrompt = remember { prompts.random() }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = appName,
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = randomPrompt,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.LightGray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        if (countdown > 0) {
            Text(
                text = countdown.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "의도적인 선택을 기다리는 중...", color = Color.Gray, fontSize = 14.sp)
        } else {
            Text(text = "얼마나 사용하시겠습니까?", color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                UsageDuration.entries.forEach { duration ->
                    Button(
                        onClick = { onConfirm(duration) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A1A1A),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(duration.label)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        TextButton(onClick = onCancel) {
            Text("그냥 닫기", color = Color.Gray)
        }
    }
}
