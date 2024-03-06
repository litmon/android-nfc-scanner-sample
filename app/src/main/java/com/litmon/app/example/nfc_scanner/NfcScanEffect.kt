package com.litmon.app.example.nfc_scanner

import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity

@Composable
fun TagScanEffect(
    activity: FragmentActivity,
    onTagDetected: (Tag) -> Unit,
) {
    val currentOnTagDetected = rememberUpdatedState(onTagDetected)
    val lifecycleOwner = LocalLifecycleOwner.current
    NfcScanEffect(
        scanner = { TagScanner(activity = activity, lifecycleOwner = lifecycleOwner) }
    ) {
        currentOnTagDetected.value(it)
    }
}

@Composable
fun IsoDepScanEffect(
    activity: FragmentActivity,
    onIsoDepDetected: (IsoDep) -> Unit,
) {
    val currentOnIsoDepDetected = rememberUpdatedState(onIsoDepDetected)
    val lifecycleOwner = LocalLifecycleOwner.current
    NfcScanEffect(
        scanner = { IsoDepScanner(activity = activity, lifecycleOwner = lifecycleOwner) }
    ) {
        currentOnIsoDepDetected.value(it)
    }
}

@Composable
fun <T : Any> NfcScanEffect(
    scanner: () -> NfcScanner<T>,
    onScanned: (T) -> Unit,
) {
    val rememberedScanner = remember { scanner() }
    DisposableEffect(rememberedScanner) {
        rememberedScanner.start(onScanned)

        onDispose {
            rememberedScanner.stop()
        }
    }
}
