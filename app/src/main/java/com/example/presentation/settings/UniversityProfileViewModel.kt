package com.example.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.GostProfile
import com.example.domain.repository.UniversityProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileEditorState(
    val id: Long = 0L,
    val name: String = "",
    val universityName: String = "",
    val facultyName: String = "",
    val departmentName: String = "",
    val leftMarginMm: String = "30",
    val rightMarginMm: String = "15",
    val topMarginMm: String = "20",
    val bottomMarginMm: String = "20",
    val fontName: String = "Times New Roman",
    val fontSizePt: String = "14",
    val lineSpacing: String = "1.5",
    val paragraphIndentMm: String = "12.5",
    val bibliographyFormat: String = "ГОСТ Р 7.0.100-2018",
    val isDefault: Boolean = false,
    val isSavedSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel для управления и редактирования профилей ВУЗов / ГОСТ.
 */
class UniversityProfileViewModel(
    private val profileRepository: UniversityProfileRepository
) : ViewModel() {

    val profiles: StateFlow<List<GostProfile>> = profileRepository.getAllGostProfiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _editorState = MutableStateFlow(ProfileEditorState())
    val editorState: StateFlow<ProfileEditorState> = _editorState.asStateFlow()

    init {
        // Убедимся, что дефолтные профили инициализированы
        viewModelScope.launch {
            profileRepository.getDefaultProfile()
        }
    }

    fun loadProfileForEditing(profileId: Long) {
        if (profileId <= 0L) {
            // Новый профиль
            _editorState.value = ProfileEditorState(
                name = "Новый профиль ВУЗа",
                universityName = "",
                leftMarginMm = "30",
                rightMarginMm = "15",
                topMarginMm = "20",
                bottomMarginMm = "20",
                fontName = "Times New Roman",
                fontSizePt = "14",
                lineSpacing = "1.5",
                paragraphIndentMm = "12.5"
            )
            return
        }

        viewModelScope.launch {
            val profile = profileRepository.getProfileById(profileId) ?: GostProfile.DEFAULT_GOST
            _editorState.value = ProfileEditorState(
                id = profile.id,
                name = profile.name,
                universityName = profile.universityName,
                facultyName = profile.facultyName,
                departmentName = profile.departmentName,
                leftMarginMm = profile.leftMarginMm.toString(),
                rightMarginMm = profile.rightMarginMm.toString(),
                topMarginMm = profile.topMarginMm.toString(),
                bottomMarginMm = profile.bottomMarginMm.toString(),
                fontName = profile.fontName,
                fontSizePt = profile.fontSizePt.toString(),
                lineSpacing = profile.lineSpacing.toString(),
                paragraphIndentMm = profile.paragraphIndentMm.toString(),
                bibliographyFormat = profile.bibliographyFormat,
                isDefault = profile.isDefault
            )
        }
    }

    fun updateName(v: String) { _editorState.value = _editorState.value.copy(name = v) }
    fun updateUniversity(v: String) { _editorState.value = _editorState.value.copy(universityName = v) }
    fun updateFaculty(v: String) { _editorState.value = _editorState.value.copy(facultyName = v) }
    fun updateDepartment(v: String) { _editorState.value = _editorState.value.copy(departmentName = v) }
    fun updateLeftMargin(v: String) { _editorState.value = _editorState.value.copy(leftMarginMm = v) }
    fun updateRightMargin(v: String) { _editorState.value = _editorState.value.copy(rightMarginMm = v) }
    fun updateTopMargin(v: String) { _editorState.value = _editorState.value.copy(topMarginMm = v) }
    fun updateBottomMargin(v: String) { _editorState.value = _editorState.value.copy(bottomMarginMm = v) }
    fun updateFontName(v: String) { _editorState.value = _editorState.value.copy(fontName = v) }
    fun updateFontSize(v: String) { _editorState.value = _editorState.value.copy(fontSizePt = v) }
    fun updateLineSpacing(v: String) { _editorState.value = _editorState.value.copy(lineSpacing = v) }
    fun updateParagraphIndent(v: String) { _editorState.value = _editorState.value.copy(paragraphIndentMm = v) }
    fun updateBibliography(v: String) { _editorState.value = _editorState.value.copy(bibliographyFormat = v) }
    fun updateIsDefault(v: Boolean) { _editorState.value = _editorState.value.copy(isDefault = v) }

    fun resetToGostDefaults() {
        val current = _editorState.value
        _editorState.value = current.copy(
            leftMarginMm = "30",
            rightMarginMm = "15",
            topMarginMm = "20",
            bottomMarginMm = "20",
            fontName = "Times New Roman",
            fontSizePt = "14",
            lineSpacing = "1.5",
            paragraphIndentMm = "12.5",
            bibliographyFormat = "ГОСТ Р 7.0.100-2018"
        )
    }

    fun saveProfile(onSuccess: () -> Unit) {
        val s = _editorState.value
        if (s.name.isBlank()) {
            _editorState.value = s.copy(errorMessage = "Введите название профиля")
            return
        }

        val profile = GostProfile(
            id = s.id,
            name = s.name.trim(),
            universityName = s.universityName.trim(),
            facultyName = s.facultyName.trim(),
            departmentName = s.departmentName.trim(),
            leftMarginMm = s.leftMarginMm.toFloatOrNull() ?: 30f,
            rightMarginMm = s.rightMarginMm.toFloatOrNull() ?: 15f,
            topMarginMm = s.topMarginMm.toFloatOrNull() ?: 20f,
            bottomMarginMm = s.bottomMarginMm.toFloatOrNull() ?: 20f,
            fontName = s.fontName.trim().ifEmpty { "Times New Roman" },
            fontSizePt = s.fontSizePt.toFloatOrNull() ?: 14f,
            lineSpacing = s.lineSpacing.toFloatOrNull() ?: 1.5f,
            paragraphIndentMm = s.paragraphIndentMm.toFloatOrNull() ?: 12.5f,
            startPageNumbering = 2,
            bibliographyFormat = s.bibliographyFormat.trim().ifEmpty { "ГОСТ Р 7.0.100-2018" },
            isDefault = s.isDefault
        )

        viewModelScope.launch {
            if (s.id == 0L) {
                profileRepository.insertProfile(profile)
            } else {
                profileRepository.updateProfile(profile)
            }
            _editorState.value = _editorState.value.copy(isSavedSuccess = true)
            onSuccess()
        }
    }

    fun deleteProfile(id: Long, onSuccess: () -> Unit) {
        if (id <= 1L) {
            // Защищаем стандартный профиль ГОСТ от случайного удаления
            _editorState.value = _editorState.value.copy(errorMessage = "Стандартный профиль ГОСТ нельзя удалить")
            return
        }
        viewModelScope.launch {
            profileRepository.deleteProfile(id)
            onSuccess()
        }
    }

    fun setAsDefault(profile: GostProfile) {
        viewModelScope.launch {
            profileRepository.updateProfile(profile.copy(isDefault = true))
        }
    }

    class Factory(
        private val repository: UniversityProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UniversityProfileViewModel(repository) as T
        }
    }
}
