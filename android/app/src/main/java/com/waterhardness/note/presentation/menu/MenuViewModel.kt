package com.waterhardness.note.presentation.menu

import androidx.lifecycle.viewModelScope
import com.waterhardness.note.domain.model.*
import com.waterhardness.note.domain.repository.CareRepository
import com.waterhardness.note.presentation.common.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class MenuViewModel(repository: CareRepository) : CareViewModel(repository) {
    private val _state = MutableStateFlow(MenuUiState())
    override val state = _state.asStateFlow()
    init { viewModelScope.launch { repository.state.collect { store -> _state.update { it.copy(store = store, selectedId = it.selectedId?.takeIf { id -> store.data.activeAppliances.any { a -> a.id == id } } ?: store.data.activeAppliances.firstOrNull()?.id) } } } }
    override fun dateChanged(today: LocalDate) { _state.update { it.copy(today = today, examples = repository.examples(today)) } }
    fun select(id: String) { _state.update { it.copy(selectedId = id) } }
    fun toggleExample() { _state.update { it.copy(exampleExpanded = !it.exampleExpanded) } }
    fun play(defaultName: String) {
        val id = _state.value.selectedId
        if (id == null) add(defaultName, true) else emit(UiEvent.StartTest(id))
    }
    fun add(defaultName: String, launchTest: Boolean = false) { _state.update { it.copy(editor = ApplianceEditor(name = defaultName, launchTest = launchTest)) } }
    fun edit(appliance: Appliance) { _state.update { it.copy(editor = ApplianceEditor(appliance.id, appliance.name, appliance.type)) } }
    fun name(value: String) { _state.update { it.copy(editor = it.editor?.copy(name = value.take(40), error = false)) } }
    fun type(type: ApplianceType, defaultName: String) { _state.update { it.copy(editor = it.editor?.copy(type = type, name = defaultName, error = false)) } }
    fun dismissEditor() { if (_state.value.editor?.saving != true) _state.update { it.copy(editor = null) } }
    fun archiveConfirmation(value: Boolean) { _state.update { it.copy(editor = it.editor?.copy(confirmArchive = value)) } }
    fun save() {
        val editor = _state.value.editor ?: return
        if (editor.saving) return
        if (editor.name.trim().isEmpty()) { _state.update { it.copy(editor = editor.copy(error = true)) }; return }
        _state.update { it.copy(editor = editor.copy(saving = true), writeError = false) }
        viewModelScope.launch {
            val appliance = Appliance(id = editor.id ?: java.util.UUID.randomUUID().toString(), name = editor.name.trim(), type = editor.type)
            runCatching { repository.update { data -> data.copy(appliances = if (editor.id == null) data.appliances + appliance else data.appliances.map { if (it.id == editor.id) it.copy(name = appliance.name, type = appliance.type) else it }) } }
                .onSuccess { _state.update { it.copy(editor = null, selectedId = appliance.id) }; if (editor.launchTest && active) emit(UiEvent.StartTest(appliance.id)) }
                .onFailure { _state.update { it.copy(editor = editor.copy(storageError = true), writeError = true) } }
        }
    }
    fun archive() {
        val editor = _state.value.editor ?: return
        if (editor.saving) return
        _state.update { it.copy(editor = editor.copy(saving = true)) }
        viewModelScope.launch {
            runCatching { repository.update { data -> data.copy(appliances = data.appliances.map { if (it.id == editor.id) it.copy(archived = true) else it }) } }
                .onSuccess { _state.update { it.copy(editor = null) } }
                .onFailure { _state.update { it.copy(editor = editor.copy(confirmArchive = false, storageError = true), writeError = true) } }
        }
    }
}
