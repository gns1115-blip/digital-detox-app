package com.example.instagramdetector.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed interface OverlayStep {
    data object SelectReason : OverlayStep
    data class SelectDuration(val reason: OverlayReason) : OverlayStep
    data class TimeExpiredBlock(val secondsRemaining: Int) : OverlayStep
    data class ShortFormBreak(val secondsRemaining: Int) : OverlayStep
}

@Composable
fun InstagramOverlayContent(
    step: OverlayStep,
    onReasonSelected: (OverlayReason) -> Unit,
    onDurationSelected: (UsageDuration) -> Unit,
) {
    val blockTouches = step is OverlayStep.TimeExpiredBlock || step is OverlayStep.ShortFormBreak

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (blockTouches) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { },
                    )
                } else {
                    Modifier
                },
            )
            .background(Color.Black)
            .padding(horizontal = 28.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (step) {
            OverlayStep.SelectReason -> ReasonSelectionContent(onReasonSelected = onReasonSelected)
            is OverlayStep.SelectDuration -> DurationSelectionContent(
                reason = step.reason,
                onDurationSelected = onDurationSelected,
            )
            is OverlayStep.TimeExpiredBlock -> TimeExpiredBlockContent(
                secondsRemaining = step.secondsRemaining,
            )
            is OverlayStep.ShortFormBreak -> ShortFormBreakContent(
                secondsRemaining = step.secondsRemaining,
            )
        }
    }
}

@Composable
private fun ReasonSelectionContent(onReasonSelected: (OverlayReason) -> Unit) {
    Text(
        text = "왜 이 앱을 열었나요?",
        color = Color.White,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(40.dp))

    OverlayReason.entries.forEach { reason ->
        OverlayActionButton(label = reason.label, onClick = { onReasonSelected(reason) })
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DurationSelectionContent(
    reason: OverlayReason,
    onDurationSelected: (UsageDuration) -> Unit,
) {
    Text(
        text = "사용 시간을 선택하세요",
        color = Color.White,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "선택한 이유: ${reason.label}",
        color = Color.LightGray,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(32.dp))

    UsageDuration.entries.forEach { duration ->
        OverlayActionButton(label = duration.label, onClick = { onDurationSelected(duration) })
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun TimeExpiredBlockContent(secondsRemaining: Int) {
    Text(
        text = "사용 시간이 종료되었습니다",
        color = Color.White,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "앱 실행이 ${secondsRemaining}초 동안 차단됩니다.",
        color = Color.LightGray,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(28.dp))

    Text(
        text = "${secondsRemaining}초",
        color = Color.White,
        fontSize = 42.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ShortFormBreakContent(secondsRemaining: Int) {
    Text(
        text = "잠시 쉬어갈까요?",
        color = Color.White,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "숏폼 콘텐츠를 장시간 시청했습니다.",
        color = Color.LightGray,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "${secondsRemaining}초 동안 스크롤이 차단됩니다",
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "잠시 눈을 쉬어 주세요.",
        color = Color.Gray,
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OverlayActionButton(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
        ),
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
