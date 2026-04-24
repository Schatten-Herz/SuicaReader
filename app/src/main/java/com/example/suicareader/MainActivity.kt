package com.example.suicareader

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.suicareader.nfc.FeliCaParser
import com.example.suicareader.nfc.NfcReaderManager
import com.example.suicareader.ui.navigation.AppNavigation
import com.example.suicareader.ui.theme.SuicaReaderTheme

import androidx.activity.viewModels
import com.example.suicareader.data.db.AppDatabase
import com.example.suicareader.ui.MainViewModel
import com.example.suicareader.ui.MainViewModelFactory
import com.example.suicareader.ui.map.TransitMapCatalog

class MainActivity : ComponentActivity() {

    private lateinit var nfcReaderManager: NfcReaderManager
    
    private val viewModel: MainViewModel by viewModels { 
        MainViewModelFactory(AppDatabase.getDatabase(this).cardDao()) 
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        com.example.suicareader.nfc.StationResolver.init(this)
        TransitMapCatalog.init(this)
        
        nfcReaderManager = NfcReaderManager(
            activity = this,
            onCardRead = { tag ->
                val suicaData = FeliCaParser.readCard(tag)
                if (suicaData != null) {
                    val idmHex = suicaData.idm.joinToString("") { "%02X".format(it) }
                    Log.d("NFC", "Card read successfully: IDm=$idmHex, Balance=${suicaData.balance}")
                    // 将 suicaData 保存到 Room 数据库
                    viewModel.saveCard(suicaData)
                } else {
                    Log.e("NFC", "Failed to parse FeliCa data")
                }
            },
            onError = { e ->
                Log.e("NFC", "NFC Error: ${e.message}")
            }
        )

        setContent {
            SuicaReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black // 配合液态玻璃的深色背景
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
}