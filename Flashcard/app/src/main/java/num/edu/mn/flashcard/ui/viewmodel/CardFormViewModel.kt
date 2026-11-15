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

data class CardFormUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,

    val question: String = "",
    val answer: String = "",

    val existingImageFileName: String? = null,
    val selectedImageUri: String? = null,
    val removeImage: Boolean = false,

    val questionError: String? = null,
    val answerError: String? = null,

    val loadErrorMessage: String? = null,
    val saveErrorMessage: String? = null,

    // true үед UI ахицын сонголтын dialog харуулна.
    val showProgressDialog: Boolean = false,

    // Утгатай бол хадгалалт амжилттай болсон.
    val savedCardId: String? = null
)

class CardFormViewModel(
    private val deckId: String,
    private val cardId: String?,
    private val repository: FlashcardRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private companion object {
        const val QUESTION_KEY = "cardFormQuestion"
        const val ANSWER_KEY = "cardFormAnswer"
        const val EXISTING_IMAGE_KEY = "cardFormExistingImage"
        const val SELECTED_IMAGE_KEY = "cardFormSelectedImage"
        const val REMOVE_IMAGE_KEY = "cardFormRemoveImage"
        const val INITIALIZED_KEY = "cardFormInitialized"
    }

    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(
        CardFormUiState(
            isEditMode = cardId != null,

            isLoading = cardId != null &&
                    savedStateHandle.get<Boolean>(INITIALIZED_KEY) != true,

            question =
                savedStateHandle.get<String>(QUESTION_KEY) ?: "",

            answer =
                savedStateHandle.get<String>(ANSWER_KEY) ?: "",

            existingImageFileName =
                savedStateHandle.get<String>(EXISTING_IMAGE_KEY),

            selectedImageUri =
                savedStateHandle.get<String>(SELECTED_IMAGE_KEY),

            removeImage =
                savedStateHandle.get<Boolean>(REMOVE_IMAGE_KEY) ?: false
        )
    )

    val uiState: StateFlow<CardFormUiState> = _uiState.asStateFlow()

    init {
        require(deckId.isNotBlank()) {
            "Картын багцын ID хоосон байж болохгүй."
        }

        require(cardId == null || cardId.isNotBlank()) {
            "Картын ID хоосон String байж болохгүй."
        }

        val isInitialized =
            savedStateHandle.get<Boolean>(INITIALIZED_KEY) == true

        if (!isInitialized) {
            if (cardId == null) {
                savedStateHandle[INITIALIZED_KEY] = true
            } else {
                loadExistingCard()
            }
        }
    }

    private fun canEdit(): Boolean {
        val state = _uiState.value

        return !state.isLoading &&
                !state.isSaving &&
                !state.showProgressDialog &&
                state.loadErrorMessage == null &&
                state.savedCardId == null
    }

    // Form-ийн өөрчлөлтийг UI төлөв болон SavedStateHandle-д
    // хамтад нь бүртгэнэ.
    //
    // transform нь:
    // хуучин төлөв авна → шинэ төлөв буцаана гэсэн функц.
    private fun updateDraft(
        transform: (CardFormUiState) -> CardFormUiState
    ) {
        val updatedState = transform(_uiState.value)

        savedStateHandle[QUESTION_KEY] = updatedState.question
        savedStateHandle[ANSWER_KEY] = updatedState.answer
        savedStateHandle[EXISTING_IMAGE_KEY] =
            updatedState.existingImageFileName
        savedStateHandle[SELECTED_IMAGE_KEY] =
            updatedState.selectedImageUri
        savedStateHandle[REMOVE_IMAGE_KEY] =
            updatedState.removeImage

        _uiState.value = updatedState
    }

    private fun loadExistingCard() {
        val selectedCardId = cardId ?: return

        if (loadJob?.isActive == true) return

        _uiState.update { state ->
            state.copy(
                isLoading = true,
                loadErrorMessage = null
            )
        }

        loadJob = viewModelScope.launch {
            try {
                val card = requireNotNull(
                    repository.getCardById(selectedCardId)
                ) {
                    "Засах карт олдсонгүй."
                }

                require(card.deckId == deckId) {
                    "Карт сонгосон багцад хамаарахгүй байна."
                }

                updateDraft { state ->
                    state.copy(
                        isLoading = false,
                        question = card.question,
                        answer = card.answer,
                        existingImageFileName = card.imageFileName,
                        selectedImageUri = null,
                        removeImage = false,
                        loadErrorMessage = null
                    )
                }

                savedStateHandle[INITIALIZED_KEY] = true

            } catch (exception: CancellationException) {
                throw exception

            } catch (exception: Exception) {
                Log.e(
                    "CardFormViewModel",
                    "Картыг уншиж чадсангүй.",
                    exception
                )

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        loadErrorMessage =
                            "Картыг уншиж чадсангүй. " +
                                    "Карт устгагдсан байж болно."
                    )
                }
            }
        }
    }

    fun retryLoading() {
        if (_uiState.value.loadErrorMessage != null) {
            loadExistingCard()
        }
    }

    fun onQuestionChange(newQuestion: String) {
        if (!canEdit()) return

        updateDraft { state ->
            state.copy(
                question = newQuestion,
                questionError = null,
                saveErrorMessage = null
            )
        }
    }

    fun onAnswerChange(newAnswer: String) {
        if (!canEdit()) return

        updateDraft { state ->
            state.copy(
                answer = newAnswer,
                answerError = null,
                saveErrorMessage = null
            )
        }
    }

    fun onImageSelected(uri: Uri?) {
        if (uri == null || !canEdit()) return

        updateDraft { state ->
            state.copy(
                selectedImageUri = uri.toString(),
                removeImage = false,
                saveErrorMessage = null
            )
        }
    }

    fun onRemoveImage() {
        if (!canEdit()) return

        updateDraft { state ->
            state.copy(
                selectedImageUri = null,
                removeImage = true,
                saveErrorMessage = null
            )
        }

        // Save амжилттай болохоос өмнө файлыг устгахгүй.
    }

    // Form-ийн Save товч.
    fun saveCard() {
        if (!canEdit()) return

        // Хэрэглэгч ахицын сонголт хараахан хийгээгүй.
        performSave(resetProgress = null)
    }

    // Ахицын dialog дээр хэрэглэгч сонголт хийсэн үед дуудна.
    fun confirmProgressChoice(resetProgress: Boolean) {

        if (!_uiState.value.showProgressDialog) return

        // true → New болгоно.
        // false → ахицыг хадгална.
        performSave(resetProgress)
    }

    // Dialog-ийн "Болих" товч.
    fun cancelProgressChoice() {
        _uiState.update { state ->
            state.copy(
                showProgressDialog = false
            )
        }

        // Form дээр бичсэн зүйлс хэвээр.
        // Database-д өөрчлөлт ороогүй.
    }

    private fun performSave(resetProgress: Boolean?) {
        val formState = _uiState.value

        if (
            formState.isLoading ||
            formState.isSaving ||
            formState.loadErrorMessage != null ||
            formState.savedCardId != null
        ) {
            return
        }

        val questionError = if (formState.question.isBlank()) {
            "Асуултаа оруулна уу."
        } else {
            null
        }

        val answerError = if (formState.answer.isBlank()) {
            "Богино хариултаа оруулна уу."
        } else {
            null
        }

        if (questionError != null || answerError != null) {
            _uiState.update { state ->
                state.copy(
                    questionError = questionError,
                    answerError = answerError
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                isSaving = true,
                showProgressDialog = false,
                questionError = null,
                answerError = null,
                saveErrorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                // Засах үед картын одоогийн төлөвийг дахин уншина.
                // Form анх нээгдсэн үеийн хайрцагт шууд найдахгүй.
                val existingCard = if (cardId == null) {
                    null
                } else {
                    requireNotNull(repository.getCardById(cardId)) {
                        "Засах карт олдсонгүй."
                    }
                }

                require(
                    existingCard == null ||
                            existingCard.deckId == deckId
                ) {
                    "Карт сонгосон багцад хамаарахгүй байна."
                }

                val contentChanged = existingCard != null &&
                        (
                                existingCard.question != formState.question.trim() ||
                                        existingCard.answer != formState.answer.trim()
                                )

                val needsProgressDecision =
                    existingCard != null &&
                            existingCard.boxLevel > 0 &&
                            contentChanged

                // Хэрэглэгчээс асуух шаардлагатай боловч
                // сонголт хараахан аваагүй бол хадгалахгүй.
                if (needsProgressDecision && resetProgress == null) {

                    _uiState.update { state ->
                        state.copy(
                            showProgressDialog = true
                        )
                    }

                    // Coroutine-ийн энэ гүйцэтгэлийг дуусгана.
                    // Доорх finally ажиллаж isSaving=false болно.
                    return@launch
                }

                val imageUri = formState.selectedImageUri?.let {
                    Uri.parse(it)
                }

                val savedId = repository.saveCard(
                    deckId = deckId,
                    question = formState.question,
                    answer = formState.answer,
                    cardId = cardId,
                    selectedImageUri = imageUri,
                    removeImage = formState.removeImage,
                    resetProgress = resetProgress
                )

                _uiState.update { state ->
                    state.copy(
                        savedCardId = savedId
                    )
                }

            } catch (exception: CancellationException) {
                throw exception

            } catch (exception: Exception) {
                Log.e(
                    "CardFormViewModel",
                    "Картыг хадгалж чадсангүй.",
                    exception
                )

                _uiState.update { state ->
                    state.copy(
                        saveErrorMessage =
                            "Картыг хадгалж чадсангүй. " +
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