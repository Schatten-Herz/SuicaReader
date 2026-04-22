package com.example.suicareader.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.NfcF
import android.os.Bundle
import kotlinx.coroutines.delay

class NfcReaderManager(
    private val activity: Activity,
    private val onCardRead: (Tag) -> Unit,
    private val onError: (Exception) -> Unit
) : NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    fun startReaderMode() {
        if (nfcAdapter == null) return

        val flags = NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        val options = Bundle().apply {
            // 根据用户建议，增加 PRESENCE_CHECK_DELAY，允许更稳定的读取，防止过快抛出 TagLostException
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 1000)
        }
        nfcAdapter?.enableReaderMode(activity, this, flags, options)
    }

    fun stopReaderMode() {
        nfcAdapter?.disableReaderMode(activity)
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag != null) {
            val nfcF = NfcF.get(tag)
            if (nfcF != null) {
                try {
                    // 交给 Parser 或直接回调处理
                    onCardRead(tag)
                } catch (e: TagLostException) {
                    // TagLostException 重试逻辑或回调
                    onError(e)
                } catch (e: Exception) {
                    onError(e)
                }
            } else {
                onError(IllegalArgumentException("Not a FeliCa (NfcF) tag"))
            }
        }
    }
}
