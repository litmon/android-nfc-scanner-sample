package com.litmon.app.example.nfc_scanner

import android.nfc.tech.IsoDep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner

@Composable
fun IsoDepScanCardEffect(
    activity: FragmentActivity = LocalContext.current as FragmentActivity,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onIsoDepDetected: (IsoDep) -> Unit,
) {
    val currentOnIsoDepDetected = rememberUpdatedState(onIsoDepDetected)
    ScanCardEffect {
        IsoDepScanner(activity = activity, lifecycleOwner = lifecycleOwner) {
            currentOnIsoDepDetected.value(it)
        }
    }
}

@Composable
fun ScanCardEffect(
    scanner: () -> NfcScanner,
) {
    val rememberedScanner = remember { scanner() }
    DisposableEffect(rememberedScanner) {
        rememberedScanner.start()

        onDispose {
            rememberedScanner.stop()
        }
    }
}
