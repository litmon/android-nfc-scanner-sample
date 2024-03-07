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
import androidx.activity.ComponentActivity
import androidx.core.app.PendingIntentCompat
import androidx.core.content.IntentCompat
import androidx.core.util.Consumer
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

interface NfcScanner<T> {
    fun start(lifecycleOwner: LifecycleOwner, onScanned: (T) -> Unit)
    fun stop()
}

internal class NfcScannerImpl<T>(
    private val activity: ComponentActivity,
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

    private var lifecycleOwner: LifecycleOwner? = null

    private var onScanned: ((T) -> Unit)? = null

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
            onScanned?.invoke(converted)
        }
    }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            Timber.d("onCreate")
            addOnNewIntentListener()
        }

        override fun onStart(owner: LifecycleOwner) {
            Timber.d("onStart")
        }

        override fun onResume(owner: LifecycleOwner) {
            Timber.d("onResume")
            enableForegroundDispatch()
        }

        override fun onPause(owner: LifecycleOwner) {
            Timber.d("onPause")
            disableForegroundDispatch()
        }

        override fun onStop(owner: LifecycleOwner) {
            Timber.d("onStop")
        }

        override fun onDestroy(owner: LifecycleOwner) {
            Timber.d("onDestroy")
            removeOnNewIntentListener()
        }
    }

    override fun start(lifecycleOwner: LifecycleOwner, onScanned: (T) -> Unit) {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        this.onScanned = onScanned
        this.lifecycleOwner = lifecycleOwner
    }

    override fun stop() {
        lifecycleOwner?.let { owner ->
            owner.lifecycle.removeObserver(lifecycleObserver)
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                disableForegroundDispatch()
                removeOnNewIntentListener()
            }
        }

        this.onScanned = null
        this.lifecycleOwner = null
    }

    private fun addOnNewIntentListener() {
        Timber.d("addOnNewIntentListener")
        activity.addOnNewIntentListener(onNewIntentListener)
    }

    private fun removeOnNewIntentListener() {
        Timber.d("removeOnNewIntentListener")
        activity.removeOnNewIntentListener(onNewIntentListener)
    }

    private fun enableForegroundDispatch() {
        Timber.d("startScanning")
        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
    }

    private fun disableForegroundDispatch() {
        Timber.d("stopScanning")
        nfcAdapter.disableForegroundDispatch(activity)
    }

    companion object {
        private const val REQUEST_CODE_PENDING_INTENT = 940625
    }
}

class TagScanner(
    activity: FragmentActivity,
) : NfcScanner<Tag> by NfcScannerImpl(
    activity = activity,
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
) : NfcScanner<IsoDep> by NfcScannerImpl(
    activity = activity,
    filters = filters,
    techLists = techLists,
    tagConverter = { IsoDep.get(it) },
) {
    companion object {
        private val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
        private val techLists = arrayOf(arrayOf(IsoDep::class.java.name))
    }
}
