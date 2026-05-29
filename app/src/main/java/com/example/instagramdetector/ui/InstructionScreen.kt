package com.example.instagramdetector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionScreen(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("기능 안내", color = Color.White) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("뒤로", color = Color.White)
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FeatureItem(
                title = "1. 앱 실행기 (Launcher)",
                description = "관리가 필요한 앱을 리스트에서 클릭하세요. 사용할 시간을 미리 선택하고 앱을 실행하면, 해당 시간 동안만 앱을 사용할 수 있습니다."
            )

            FeatureItem(
                title = "2. 자동 감지 설정 (체크박스)",
                description = "앱 옆의 아이콘을 활성화하면, 실행기를 거치지 않고 홈 화면에서 앱을 직접 열어도 시스템이 이를 감지하여 관리를 시작합니다."
            )

            FeatureItem(
                title = "3. 강력한 종료 및 차단",
                description = "설정된 시간이 모두 지나면 앱이 강제로 닫히고 홈 화면으로 이동합니다. 이후 30초 동안은 해당 앱을 다시 열 수 없습니다."
            )

            FeatureItem(
                title = "4. 스크롤 패턴 분석",
                description = "앱 사용 중 5분 이상 과도한 스크롤이나 터치가 반복되면 '무한 콘텐츠 시청'으로 판단하여 휴식을 권고하는 화면을 띄웁니다."
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "※ 본 앱은 접근성 서비스 권한을 사용하여 앱 종료 및 패턴 분석을 수행합니다. 어떠한 데이터도 외부로 전송되지 않습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun FeatureItem(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                lineHeight = 22.sp
            )
        }
    }
}
