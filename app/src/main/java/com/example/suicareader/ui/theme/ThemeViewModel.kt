package com.example.suicareader.ui.theme

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("suica_prefs", Context.MODE_PRIVATE)
    private val savedLangString = prefs.getString("app_language", null)
    private val _currentLanguage = MutableStateFlow(
        savedLangString
            ?.let {
                try {
                    AppLanguage.valueOf(it)
                } catch (e: Exception) {
                    systemDefaultLanguage()
                }
            }
            ?: systemDefaultLanguage()
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        prefs.edit().putString("app_language", lang.name).apply()
    }

    private fun systemDefaultLanguage(): AppLanguage {
        val lang = Locale.getDefault().language.lowercase(Locale.ROOT)
        return when (lang) {
            "zh" -> AppLanguage.ZH
            "ja" -> AppLanguage.JA
            else -> AppLanguage.EN
        }
    }
}
