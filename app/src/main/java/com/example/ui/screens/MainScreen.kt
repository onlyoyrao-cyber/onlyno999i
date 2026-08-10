package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AnalyzerViewModel

enum class NavigationTab(val title: String, val icon: ImageVector) {
    PREDICTION("预测杀号", Icons.Default.Analytics),
    TRIGGER("触发推演", Icons.Default.AltRoute),
    BACKTEST("自动回测", Icons.Default.Speed),
    FREQUENCY("冷热矩阵", Icons.Default.BarChart),
    SETTINGS("数据管理", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AnalyzerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(NavigationTab.PREDICTION) }

    val allDraws by viewModel.allDraws.collectAsStateWithLifecycle()
    val predictionResult by viewModel.predictionResult.collectAsStateWithLifecycle()
    val backtestRecords by viewModel.backtestRecords.collectAsStateWithLifecycle()
    val backtestSummary by viewModel.backtestSummary.collectAsStateWithLifecycle()
    val bufferedPredictions by viewModel.bufferedPredictions.collectAsStateWithLifecycle()
    val bufferSummary by viewModel.bufferSummary.collectAsStateWithLifecycle()
    val frequencyStats by viewModel.frequencyStats.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val poolSize by viewModel.targetPoolSize.collectAsStateWithLifecycle()
    val remoteUrl by viewModel.remoteUrl.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "澳门杀神",
                        color = Color(0xFF1D1B20),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF3EDF7),
                contentColor = Color(0xFF1D1B20)
            ) {
                NavigationTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 10.sp, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6750A4),
                            selectedTextColor = Color(0xFF6750A4),
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F)
                        )
                    )
                }
            }
        },
        containerColor = Color(0xFFFEF7FF),
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFEF7FF))
        ) {
            when (selectedTab) {
                NavigationTab.PREDICTION -> PredictionScreen(
                    predictionResult = predictionResult,
                    bufferSummary = bufferSummary,
                    bufferedPredictions = bufferedPredictions,
                    recentDraws = allDraws,
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshRemoteData() }
                )
                NavigationTab.TRIGGER -> TriggerDetailScreen(
                    triggerInfo = predictionResult?.triggerInfo,
                    recentDraws = allDraws
                )
                NavigationTab.BACKTEST -> BacktestScreen(
                    summary = backtestSummary,
                    records = backtestRecords,
                    bufferSummary = bufferSummary,
                    bufferedPredictions = bufferedPredictions,
                    onRunForcedBacktest = { viewModel.runForcedSinglePeriodBacktest() }
                )
                NavigationTab.FREQUENCY -> FrequencyScreen(
                    frequencyStats = frequencyStats,
                    maxPool = poolSize
                )
                NavigationTab.SETTINGS -> SettingsScreen(
                    remoteUrl = remoteUrl,
                    onUpdateRemoteUrl = { viewModel.updateRemoteUrl(it) },
                    onSyncRemoteData = { viewModel.refreshRemoteData() },
                    onAddDraw = { period, nums -> viewModel.addDrawRecord(period, nums) },
                    onResetData = { viewModel.resetToDefaultHistory() },
                    onRunForcedBacktest = { viewModel.runForcedSinglePeriodBacktest() }
                )
            }
        }
    }
}

