package com.litmon.app.example.nfc_scanner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.litmon.app.example.nfc_scanner.ui.theme.NfcScannerTheme

class MainActivity : AppCompatActivity() {
    private val scanner by lazy { IsoDepScanner(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NfcScannerTheme {
                var isScanning by remember {
                    mutableStateOf(false)
                }

                if (isScanning) {
                    IsoDepScanEffect(scanner) {
                        Toast.makeText(this, "isoDep detected. $it", Toast.LENGTH_SHORT).show()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                        ) {
                            Text(
                                text = if (isScanning) "Scanning" else "Not Scanning",
                            )
                            Button(onClick = { isScanning = !isScanning }) {
                                Text(text = if (isScanning) "stop scan" else "start scan")
                            }
                        }
                    }
                }
            }
        }
    }
}
