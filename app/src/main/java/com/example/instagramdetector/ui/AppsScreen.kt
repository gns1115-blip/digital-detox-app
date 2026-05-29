package com.example.instagramdetector.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.instagramdetector.util.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    onBack: () -> Unit,
    onLaunchRequest: (AppInfo) -> Unit,
    viewModel: AppsViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val monitoredPackages by viewModel.monitoredPackages.collectAsStateWithLifecycle(initialValue = emptySet())

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("애플리케이션", color = Color.White) },
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
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("앱 검색...", color = Color.DarkGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.DarkGray,
                    cursorColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { appInfo ->
                    AppRow(
                        appInfo = appInfo,
                        isMonitored = monitoredPackages.contains(appInfo.packageName),
                        onToggleMonitor = { viewModel.toggleMonitoring(appInfo.packageName) },
                        onLaunchRequest = { onLaunchRequest(appInfo) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    appInfo: AppInfo,
    isMonitored: Boolean,
    onToggleMonitor: () -> Unit,
    onLaunchRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isMonitored) Color(0xFF1A1A1A) else Color.Transparent)
            .clickable { onLaunchRequest() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        appInfo.icon?.let {
            Image(
                bitmap = it.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(42.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appInfo.name, 
                color = if (isMonitored) Color.White else Color.LightGray,
                fontWeight = if (isMonitored) FontWeight.Bold else FontWeight.Normal,
                fontSize = 16.sp
            )
            Text(text = appInfo.packageName, color = Color.DarkGray, fontSize = 11.sp)
        }

        IconButton(onClick = onToggleMonitor) {
            Icon(
                imageVector = if (isMonitored) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                contentDescription = null,
                tint = if (isMonitored) Color.White else Color.DarkGray
            )
        }
    }
}
