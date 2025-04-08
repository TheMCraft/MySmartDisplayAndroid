package com.htlklu.mysmartdisplay

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun CommandPicker(bleManager: BLEManager) {
    var selCommand by remember { mutableStateOf("Befehl Auswählen") }
    var expanded by remember { mutableStateOf(false) }
    val commands = listOf("delay:300", "delay:150", "d", "blink:off", "blink:on", "led:off", "run:off", "run:on", "led:on", "light:off", "light:on")

    Column(modifier = Modifier.padding(16.dp)) {
        Box {
            OutlinedTextField(
                value = selCommand,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                readOnly = true,
                label = { Text("Befehl wählen") }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                commands.forEach { command ->
                    DropdownMenuItem(
                        text = { Text(command) },
                        onClick = {
                            selCommand = command
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (selCommand != "Befehl Auswählen") {
                    bleManager.writeToCharacteristic(selCommand)
                    selCommand = "Befehl Auswählen"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Befehl senden")
        }
    }
}