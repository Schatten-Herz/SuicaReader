package com.example.suicareader

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.example.suicareader.data.db.AppDatabase
import com.example.suicareader.nfc.FeliCaParser
import com.example.suicareader.nfc.NfcReaderManager
import com.example.suicareader.nfc.StationResolver
import com.example.suicareader.ui.MainViewModel
import com.example.suicareader.ui.MainViewModelFactory
import com.example.suicareader.ui.map.TransitMapCatalog
import com.example.suicareader.ui.navigation.AppNavigation
import com.example.suicareader.ui.theme.SuicaReaderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private enum class HapticCue {
        Detected, Success, Error
    }

    private lateinit var nfcReaderManager: NfcReaderManager

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(AppDatabase.getDatabase(this).cardDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize heavy station & coordinate datasets off the main thread
        lifecycleScope.launch(Dispatchers.IO) {
            TransitMapCatalog.init(applicationContext)
            StationResolver.init(applicationContext)
            StationResolver.warmSearchIndex()
        }

        nfcReaderManager = NfcReaderManager(
            activity = this,
            onTagDetected = {
                performHapticCue(HapticCue.Detected)
                viewModel.notifyNfcReading()
            },
            onCardRead = { tag ->
                lifecycleScope.launch(Dispatchers.IO) {
                    StationResolver.awaitReady()
                    when (val result = FeliCaParser.readCardDetailed(tag)) {
                        is FeliCaParser.ReadResult.Success -> {
                            val idmHex = result.data.idm.joinToString("") { "%02X".format(it) }
                            Log.d("NFC", "Card read successfully: IDm=$idmHex, Balance=${result.data.balance}")
                            performHapticCue(HapticCue.Success)
                            viewModel.saveCard(result.data)
                        }
                        is FeliCaParser.ReadResult.TagLost -> {
                            Log.w("NFC", "Tag lost during read")
                            performHapticCue(HapticCue.Error)
                            viewModel.notifyNfcTagLost()
                        }
                        is FeliCaParser.ReadResult.Failed -> {
                            Log.e("NFC", "Failed to parse FeliCa data", result.error)
                            performHapticCue(HapticCue.Error)
                            viewModel.notifyNfcFailed()
                        }
                    }
                }
            },
            onUnsupportedTag = {
                Log.w("NFC", "Unsupported NFC tag discovered")
                performHapticCue(HapticCue.Error)
                viewModel.notifyNfcUnsupported()
            },
            onError = { e ->
                Log.e("NFC", "NFC Error: ${e.message}")
                performHapticCue(HapticCue.Error)
                viewModel.notifyNfcFailed()
            }
        )

        setContent {
            SuicaReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcReaderManager.startReaderMode()
    }

    override fun onPause() {
        super.onPause()
        nfcReaderManager.stopReaderMode()
    }

    @Suppress("DEPRECATION")
    private fun performHapticCue(cue: HapticCue) {
        runOnUiThread {
            val handledByVibrator = runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (vibrator != null && vibrator.hasVibrator()) {
                    val effect = when (cue) {
                        HapticCue.Detected -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                        HapticCue.Success -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                        HapticCue.Error -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                    }
                    vibrator.vibrate(effect)
                    true
                } else {
                    false
                }
            }.getOrDefault(false)

            if (!handledByVibrator) {
                val flag = when (cue) {
                    HapticCue.Detected -> HapticFeedbackConstants.CLOCK_TICK
                    HapticCue.Success -> HapticFeedbackConstants.CONFIRM
                    HapticCue.Error -> HapticFeedbackConstants.REJECT
                }
                window.decorView.performHapticFeedback(flag)
            }
        }
    }
}
