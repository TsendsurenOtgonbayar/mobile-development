package num.edu.mn.flashcard.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import num.edu.mn.flashcard.data.repository.FlashcardRepository

data class DeckFormUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,

    val title: String = "",
    val description: String = "",

    // Өмнө нь Room-д хадгалсан нүүр зургийн файлын нэр.
    val existingImageFileName: String? = null,

    // Form дээр шинээр сонгосон зургийн Uri-г String болгон барина.
    // Зургийг өөрийг нь SavedStateHandle-д хадгалахгүй.
    val selectedImageUri: String? = null,

    // Хэрэглэгч зургийг арилгахаар сонгосон эсэх.
    val removeImage: Boolean = false,

    val titleError: String? = null,

    // Хуучин багцын мэдээлэл унших үеийн алдаа.
    val loadErrorMessage: String? = null,

    // Хадгалах үйлдлийн алдаа.
    val saveErrorMessage: String? = null,

    // null биш болсон бол хадгалалт амжилттай.
    // Route энэ төлөвийг хараад өмнөх дэлгэц рүү буцна.
    val savedDeckId: String? = null
)

class DeckFormViewModel(
    private val deckId: String?,
    private val repository: FlashcardRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // SavedStateHandle-ийн түлхүүрүүд.
    // Зөвхөн энэ class дотор ашиглана.
    private companion object {
        const val TITLE_KEY = "deckFormTitle"
        const val DESCRIPTION_KEY = "deckFormDescription"
        const val EXISTING_IMAGE_KEY = "deckFormExistingImage"
        const val SELECTED_IMAGE_KEY = "deckFormSelectedImage"
        const val REMOVE_IMAGE_KEY = "deckFormRemoveImage"
        const val INITIALIZED_KEY = "deckFormInitialized"
    }

    // Хуучин мэдээлэл унших ажлыг давхар эхлүүлэхгүй байхад ашиглана.
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(
        DeckFormUiState(
            isEditMode = deckId != null,

            // Өмнөх form-ийн төлөв сэргэсэн бол
            // database-аас дахин уншаад бичвэрийг нь дарж болохгүй.
            isLoading = deckId != null &&
                    savedStateHandle.get<Boolean>(INITIALIZED_KEY) != true,

            title = savedStateHandle.get<String>(TITLE_KEY) ?: "",

            description =
                savedStateHandle.get<String>(DESCRIPTION_KEY) ?: "",

            existingImageFileName =
                savedStateHandle.get<String>(EXISTING_IMAGE_KEY),

            selectedImageUri =
                savedStateHandle.get<String>(SELECTED_IMAGE_KEY),

            removeImage =
                savedStateHandle.get<Boolean>(REMOVE_IMAGE_KEY) ?: false
        )
    )

    // UI зөвхөн уншина.
    // Өөрчлөх үйлдлүүдийг доорх функцүүдээр дамжуулна.
    val uiState: StateFlow<DeckFormUiState> = _uiState.asStateFlow()

    init {
        require(deckId == null || deckId.isNotBlank()) {
            "Багцын ID хоосон String байж болохгүй."
        }

        val isInitialized =
            savedStateHandle.get<Boolean>(INITIALIZED_KEY) == true

        if (!isInitialized) {
            if (deckId == null) {

                // Шинэ багцын form хоосон утгуудаар бэлэн байна.
                savedStateHandle[INITIALIZED_KEY] = true

            } else {
                loadExistingDeck()
            }
        }
    }

    // UI loading, saving үед талбаруудыг мөн disabled болгоно.
    // Энд давхар шалгаж, логикийн түвшинд хамгаалж байна.
    private fun canEdit(): Boolean {
        val state = _uiState.value

        return !state.isLoading &&
                !state.isSaving &&
                state.loadErrorMessage == null &&
                state.savedDeckId == null
    }

    // Засах багцын мэдээллийг нэг удаа уншина.
    private fun loadExistingDeck() {

        val selectedDeckId = deckId ?: return

        if (loadJob?.isActive == true) {
            return
        }

        _uiState.update { state ->
            state.copy(
                isLoading = true,
                loadErrorMessage = null
            )
        }

        loadJob = viewModelScope.launch {
            try {
                val deck = requireNotNull(
                    repository.getDeckById(selectedDeckId)
                ) {
                    "Засах багц олдсонгүй."
                }

                // Form-ийн анхны утгуудыг saved state-д бүртгэнэ.
                savedStateHandle[TITLE_KEY] = deck.title
                savedStateHandle[DESCRIPTION_KEY] = deck.description
                savedStateHandle[EXISTING_IMAGE_KEY] =
                    deck.coverImageFileName
                savedStateHandle[SELECTED_IMAGE_KEY] = null
                savedStateHandle[REMOVE_IMAGE_KEY] = false
                savedStateHandle[INITIALIZED_KEY] = true

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        title = deck.title,
                        description = deck.description,
                        existingImageFileName = deck.coverImageFileName,
                        selectedImageUri = null,
                        removeImage = false,
                        loadErrorMessage = null
                    )
                }

            } catch (exception: CancellationException) {
                throw exception

            } catch (exception: Exception) {
                Log.e(
                    "DeckFormViewModel",
                    "Засах багцыг уншиж чадсангүй.",
                    exception
                )

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        loadErrorMessage =
                            "Багцын мэдээллийг уншиж чадсангүй. " +
                                    "Багц устгагдсан байж болно."
                    )
                }
            }
        }
    }

    fun retryLoading() {
        if (_uiState.value.loadErrorMessage != null) {
            loadExistingDeck()
        }
    }

    fun onTitleChange(newTitle: String) {
        if (!canEdit()) return

        savedStateHandle[TITLE_KEY] = newTitle

        _uiState.update { state ->
            state.copy(
                title = newTitle,
                titleError = null,
                saveErrorMessage = null
            )
        }
    }

    fun onDescriptionChange(newDescription: String) {
        if (!canEdit()) return

        savedStateHandle[DESCRIPTION_KEY] = newDescription

        _uiState.update { state ->
            state.copy(
                description = newDescription,
                saveErrorMessage = null
            )
        }
    }

    // Photo Picker-ийн үр дүнг Route энэ функцэд дамжуулна.
    fun onImageSelected(uri: Uri?) {

        // null бол хэрэглэгч picker-ийг зураг сонголгүй хаасан.
        // Байгаа зургийн сонголтыг өөрчлөхгүй.
        if (uri == null || !canEdit()) return

        val uriString = uri.toString()

        savedStateHandle[SELECTED_IMAGE_KEY] = uriString
        savedStateHandle[REMOVE_IMAGE_KEY] = false

        _uiState.update { state ->
            state.copy(
                selectedImageUri = uriString,
                removeImage = false,
                saveErrorMessage = null
            )
        }
    }

    fun onRemoveImage() {
        if (!canEdit()) return

        savedStateHandle[SELECTED_IMAGE_KEY] = null
        savedStateHandle[REMOVE_IMAGE_KEY] = true

        _uiState.update { state ->
            state.copy(
                selectedImageUri = null,
                removeImage = true,
                saveErrorMessage = null
            )
        }

        // Энд хуучин зургийн файлыг шууд устгахгүй.
        // Save дарахгүйгээр буцвал хуучин зураг хэвээр байх ёстой.
    }

    fun saveDeck() {
        if (!canEdit()) return

        val formState = _uiState.value

        if (formState.title.isBlank()) {
            _uiState.update { state ->
                state.copy(
                    titleError = "Багцын нэрийг оруулна уу."
                )
            }
            return
        }

        // Дахин Save дарахаас өмнө saving төлөвийг идэвхжүүлнэ.
        _uiState.update { state ->
            state.copy(
                isSaving = true,
                titleError = null,
                saveErrorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val imageUri = formState.selectedImageUri?.let {
                    Uri.parse(it)
                }

                val savedId = repository.saveDeck(
                    title = formState.title,
                    description = formState.description,
                    deckId = deckId,
                    selectedImageUri = imageUri,
                    removeImage = formState.removeImage
                )

                _uiState.update { state ->
                    state.copy(
                        savedDeckId = savedId
                    )
                }

            } catch (exception: CancellationException) {
                throw exception

            } catch (exception: Exception) {
                Log.e(
                    "DeckFormViewModel",
                    "Багцыг хадгалж чадсангүй.",
                    exception
                )

                _uiState.update { state ->
                    state.copy(
                        saveErrorMessage =
                            "Багцыг хадгалж чадсангүй. " +
                                    "Дахин оролдох эсвэл зургаа дахин сонгоно уу."
                    )
                }

            } finally {
                _uiState.update { state ->
                    state.copy(
                        isSaving = false
                    )
                }
            }
        }
    }

    fun dismissSaveError() {
        _uiState.update { state ->
            state.copy(
                saveErrorMessage = null
            )
        }
    }
}