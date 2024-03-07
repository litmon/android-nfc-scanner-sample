package com.litmon.app.example.nfc_scanner

import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner

@Composable
fun TagScanEffect(
    scanner: TagScanner,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onTagDetected: (Tag) -> Unit,
) {
    val currentOnTagDetected = rememberUpdatedState(onTagDetected)
    NfcScanEffect(
        scanner = scanner,
        lifecycleOwner = lifecycleOwner,
    ) {
        currentOnTagDetected.value(it)
    }
}

@Composable
fun IsoDepScanEffect(
    scanner: IsoDepScanner,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onIsoDepDetected: (IsoDep) -> Unit,
) {
    val currentOnIsoDepDetected = rememberUpdatedState(onIsoDepDetected)
    NfcScanEffect(
        scanner = scanner,
        lifecycleOwner = lifecycleOwner,
    ) {
        currentOnIsoDepDetected.value(it)
    }
}

@Composable
fun <T : Any> NfcScanEffect(
    scanner: NfcScanner<T>,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onScanned: (T) -> Unit,
) {
    DisposableEffect(scanner, lifecycleOwner) {
        scanner.start(lifecycleOwner, onScanned)

        onDispose {
            scanner.stop()
        }
    }
}
