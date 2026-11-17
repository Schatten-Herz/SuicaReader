package com.example.suicareader.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Bundle
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

class NfcReaderManager(
    private val activity: Activity,
    private val onTagDetected: () -> Unit = {},
    private val onCardRead: (Tag) -> Unit,
    private val onUnsupportedTag: () -> Unit = {},
    private val onError: (Exception) -> Unit
) : NfcAdapter.ReaderCallback {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val isReading = AtomicBoolean(false)
    @Volatile
    private var lastHandledAtMs: Long = 0L

    fun startReaderMode() {
        if (nfcAdapter == null) return

        val flags = NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 1000)
        }
        nfcAdapter.enableReaderMode(activity, this, flags, options)
    }

    fun stopReaderMode() {
        nfcAdapter?.disableReaderMode(activity)
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag == null) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastHandledAtMs < 900L) return
        if (!isReading.compareAndSet(false, true)) return

        try {
            val nfcF = NfcF.get(tag)
            if (nfcF != null) {
                onTagDetected()
                onCardRead(tag)
                lastHandledAtMs = SystemClock.elapsedRealtime()
            } else {
                lastHandledAtMs = SystemClock.elapsedRealtime()
                onUnsupportedTag()
            }
        } catch (e: Exception) {
            onError(e)
        } finally {
            isReading.set(false)
        }
    }
}
