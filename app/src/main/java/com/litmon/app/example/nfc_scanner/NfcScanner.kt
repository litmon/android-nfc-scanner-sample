package com.litmon.app.example.nfc_scanner

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcBarcode
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import androidx.core.app.PendingIntentCompat
import androidx.core.content.IntentCompat
import androidx.core.util.Consumer
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

interface NfcScanner<T : Any> {
    fun start(onScanned: (T) -> Unit)
    fun stop()
}

internal class NfcScannerImpl<T : Any>(
    private val activity: FragmentActivity,
    private val lifecycleOwner: LifecycleOwner,
    private val filters: Array<IntentFilter>,
    private val techLists: Array<Array<String>>,
    private val tagConverter: (Tag) -> T?,
) : NfcScanner<T> {
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

    private var onScanned: (T) -> Unit = {}

    private val onNewIntentListener = Consumer<Intent> {
        Timber.d("newIntent received: $it")
        val tag =
            IntentCompat.getParcelableExtra(it, NfcAdapter.EXTRA_TAG, android.nfc.Tag::class.java)
        if (tag != null) {
            Timber.d("tag.techList = (${tag.techList.joinToString(", ")})")
        } else {
            Timber.w(":warn: tag is null")
        }

        val converted = tag?.let(tagConverter)
        if (converted != null) {
            onScanned(converted)
        }
    }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
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

        override fun onStop(owner: LifecycleOwner) {
            Timber.d("removeOnNewIntentListener")
            activity.removeOnNewIntentListener(onNewIntentListener)
        }
    }

    override fun start(onScanned: (T) -> Unit) {
        this.onScanned = onScanned
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }

    override fun stop() {
        this.onScanned = {}
        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)

        lifecycleObserver.onPause(lifecycleOwner)
        lifecycleObserver.onDestroy(lifecycleOwner)
    }

    companion object {
        private const val REQUEST_CODE_PENDING_INTENT = 940625
    }
}

class TagScanner(
    activity: FragmentActivity,
    lifecycleOwner: LifecycleOwner,
) : NfcScanner<Tag> by NfcScannerImpl(
    activity = activity,
    lifecycleOwner = lifecycleOwner,
    filters = filters,
    techLists = techLists,
    tagConverter = { it },
) {
    companion object {
        private val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
        )
        private val techLists = arrayOf(
            arrayOf(IsoDep::class.java.name),
            arrayOf(NfcA::class.java.name),
            arrayOf(NfcB::class.java.name),
            arrayOf(NfcF::class.java.name),
            arrayOf(NfcV::class.java.name),
            arrayOf(NfcBarcode::class.java.name),
            arrayOf(Ndef::class.java.name),
            arrayOf(MifareClassic::class.java.name),
            arrayOf(MifareUltralight::class.java.name),
        )
    }
}

class IsoDepScanner(
    activity: FragmentActivity,
    lifecycleOwner: LifecycleOwner,
) : NfcScanner<IsoDep> by NfcScannerImpl(
    activity = activity,
    lifecycleOwner = lifecycleOwner,
    filters = filters,
    techLists = techLists,
    tagConverter = { IsoDep.get(it) },
) {
    companion object {
        private val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
        private val techLists = arrayOf(arrayOf(IsoDep::class.java.name))
    }
}
