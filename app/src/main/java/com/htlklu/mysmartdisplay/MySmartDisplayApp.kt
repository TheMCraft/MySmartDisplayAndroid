package com.htlklu.mysmartdisplay

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
@RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
fun MySmartDisplayApp(bleManager: BLEManager) {
    val connectedDevice = bleManager.connectedDevice.collectAsState()
    val red = bleManager.red.collectAsState()
    val green = bleManager.green.collectAsState()
    val blue = bleManager.blue.collectAsState()
    val log = bleManager.log.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            "My Smart Display",
            fontSize = 24.sp,
            color = Color.White,
            modifier = Modifier
                .background(Color(0xFF9E092E))
                .fillMaxWidth()
                .padding(16.dp)
        )

        if (connectedDevice.value == null) {
            Text("Verbinde dich mit deinem My Smart Display...", color = Color.Red)
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(bleManager.discoveredDevices.value) { device ->
                    Text(
                        text = device.name ?: "Unknown",
                        modifier = Modifier
                            .clickable { bleManager.connectToDevice(device) }
                            .padding(8.dp)
                    )
                }
            }
        } else {
            Text("Verbunden mit: ${connectedDevice.value?.name ?: "Unknown"}", modifier = Modifier.padding(vertical = 8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = red.value.toFloat(),
                    onValueChange = { bleManager.setColor('r', it) },
                    valueRange = 0f..255f,
                    colors = SliderDefaults.colors(thumbColor = Color.Red)
                )
                Slider(
                    value = green.value.toFloat(),
                    onValueChange = { bleManager.setColor('g', it) },
                    valueRange = 0f..255f,
                    colors = SliderDefaults.colors(thumbColor = Color.Green)
                )
                Slider(
                    value = blue.value.toFloat(),
                    onValueChange = { bleManager.setColor('b', it) },
                    valueRange = 0f..255f,
                    colors = SliderDefaults.colors(thumbColor = Color.Blue)
                )
            }

            Spacer(Modifier.height(8.dp))
            CommandPicker(bleManager)

            Spacer(Modifier.height(16.dp))
            Text("Logs", fontSize = 18.sp)
            Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(log.value)
            }
        }
    }
}
