package com.litmon.app.example.nfc_scanner

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.core.app.PendingIntentCompat
import androidx.core.content.IntentCompat
import androidx.core.util.Consumer
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

interface NfcScanner {
    fun start()
    fun stop()
}

internal class NfcScannerImpl(
    private val activity: FragmentActivity,
    private val lifecycleOwner: LifecycleOwner,
    private val filters: Array<IntentFilter>,
    private val techLists: Array<Array<String>>,
    private val onCardScanned: (Tag) -> Unit,
) : NfcScanner, DefaultLifecycleObserver {
    private val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)

    private val intent = Intent(activity, activity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    private val pendingIntent = PendingIntentCompat.getActivity(
        activity,
        REQUEST_CODE_PENDING_INTENT,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT,
        true
    )

    private val onNewIntentListener = Consumer<Intent> {
        Timber.d("newIntent received: $it")
        val tag = IntentCompat.getParcelableExtra(it, NfcAdapter.EXTRA_TAG, android.nfc.Tag::class.java)
        if (tag != null) {
            Timber.d("tag.techList = (${tag.techList.joinToString(", ")})")
            onCardScanned(tag)
        } else {
            Timber.w(":warn: tag is null")
        }
    }

    override fun start() {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun stop() {
        lifecycleOwner.lifecycle.removeObserver(this)

        onPause(lifecycleOwner)
        onDestroy(lifecycleOwner)
    }

    override fun onCreate(owner: LifecycleOwner) {
        Timber.d("addOnNewIntentListener")
        activity.addOnNewIntentListener(onNewIntentListener)
    }

    override fun onResume(owner: LifecycleOwner) {
        Timber.d("startScanning")
        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
    }

    override fun onPause(owner: LifecycleOwner) {
        Timber.d("stopScanning")
        nfcAdapter.disableForegroundDispatch(activity)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Timber.d("removeOnNewIntentListener")
        activity.removeOnNewIntentListener(onNewIntentListener)
    }

    companion object {
        private const val REQUEST_CODE_PENDING_INTENT = 128457391
    }
}

class IsoDepScanner(
    activity: FragmentActivity,
    lifecycleOwner: LifecycleOwner,
    private val onIsoDepDetected: (IsoDep) -> Unit,
) : NfcScanner by NfcScannerImpl(
    activity = activity,
    lifecycleOwner = lifecycleOwner,
    filters = filters,
    techLists = techLists,
    onCardScanned = {
        val isoDep = IsoDep.get(it)
        if (isoDep != null) {
            onIsoDepDetected(isoDep)
        } else {
            Timber.w(":warn: isoDep is null")
        }
    }
) {
    companion object {
        private val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
        private val techLists = arrayOf(arrayOf(IsoDep::class.java.name))
    }
}
