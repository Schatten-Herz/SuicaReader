package com.example.suicareader.ui.theme

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("suica_prefs", Context.MODE_PRIVATE)
    
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    private val savedLangString = prefs.getString("app_language", "EN") ?: "EN"
    private val _currentLanguage = MutableStateFlow(
        try { AppLanguage.valueOf(savedLangString) } catch (e: Exception) { AppLanguage.EN }
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage

    fun toggleTheme() {
        val newValue = !_isDarkTheme.value
        _isDarkTheme.value = newValue
        prefs.edit().putBoolean("is_dark_theme", newValue).apply()
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        prefs.edit().putString("app_language", lang.name).apply()
    }
}
