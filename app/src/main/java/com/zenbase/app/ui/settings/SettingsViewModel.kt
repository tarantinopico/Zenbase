package com.zenbase.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zenbase.app.ZenbaseApp
import com.zenbase.app.domain.usecase.BackupRestoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val backupRestoreUseCase: BackupRestoreUseCase
) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ZenbaseApp
                SettingsViewModel(app.container.backupRestoreUseCase)
            }
        }
    }
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun createBackup(onSuccess: (Uri) -> Unit) {
        viewModelScope.launch {
            try {
                val uri = backupRestoreUseCase.createBackup()
                onSuccess(uri)
                _message.value = "Záloha vytvořena a uložena."
            } catch (e: Exception) {
                _message.value = "Chyba při zálohování: ${e.message}"
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                backupRestoreUseCase.restoreBackup(uri)
                _message.value = "Záloha byla úspěšně obnovena."
            } catch (e: Exception) {
                _message.value = "Chyba při obnově: ${e.message}"
            }
        }
    }
    
    fun clearMessage() {
        _message.value = null
    }
}
