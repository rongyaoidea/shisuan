package com.example.shisuan.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.shisuan.ShisuanApplication
import com.example.shisuan.ui.components.BrutalButton
import com.example.shisuan.ui.components.BrutalInput
import com.example.shisuan.ui.theme.*
import com.example.shisuan.ui.viewModel.UnitConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConfigScreen() {
    val app = LocalContext.current.applicationContext as ShisuanApplication
    val vm = viewModel { UnitConfigViewModel(app.repo) }
    val config by vm.config.collectAsState()
    var wpb by remember { mutableStateOf(config?.weightPerBoxGram?.toString() ?: "5000") }
    var ppb by remember { mutableStateOf(config?.packagesPerBox?.toString() ?: "10") }

    Scaffold(topBar = { TopAppBar(title = { Text("换算配置", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF1A1A1A)) }, navigationIcon = { IconButton(onClick = {}) { Icon(Icons.Default.ArrowBack, "返回") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("装箱规格", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B6B6B))
            BrutalInput(wpb, { wpb = it }, "每箱重量", "g", KeyboardType.Decimal)
            BrutalInput(ppb, { ppb = it }, "每箱包数", "包", KeyboardType.Decimal)
            if (config != null) {
                val w = wpb.toDoubleOrNull() ?: 0.0
                val p = ppb.toIntOrNull() ?: 1
                if (w > 0 && p > 0) Text("自动推算：每包 ${"%.0f".format(w / p)}g", fontSize = 13.sp, color = Color(0xFF6B6B6B))
            }
            BrutalButton("保 存", { val w = wpb.toDoubleOrNull() ?: return@BrutalButton ; val p = ppb.toIntOrNull() ?: return@BrutalButton ; if (w > 0 && p > 0) vm.save(w, p) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }
}
