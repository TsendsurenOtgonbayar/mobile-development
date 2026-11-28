package num.edu.mn.flashcard.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import num.edu.mn.flashcard.data.models.FlashCard
import num.edu.mn.flashcard.data.repository.FlashcardRepository

// enum class: Урьдчилан тодорхойлсон сонголтуудыг илэрхийлнэ.
// Ингэснээр горимын нэрийг String-ээр бичиж алдахгүй.
enum class StudyMode {
    FLASHCARD,
    LEARN
}

// Дэлгэц зурахад хэрэгтэй мэдээллүүд.
// Room-д энэ объектыг хадгалахгүй.
// Энэ нь зөвхөн одоогийн суралцах үйлдлийн төлөв юм.
data class StudyUiState(
    val studyMode: StudyMode,
    val isPractice: Boolean,

    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,

    val cards: List<FlashCard> = emptyList(),
    val currentCardIndex: Int = 0,

    val typedAnswer: String = "",
    val answerInputError: String? = null,

    val isAnswerVisible: Boolean = false,
    val isSubmitting: Boolean = false,

    // null: Одоогийн картын хариултыг хараахан үнэлээгүй.
    // true: Зөв / санаж байна.
    // false: Буруу / санахгүй байна.
    val answerResult: Boolean? = null,

    val correctCount: Int = 0,
    val wrongCount: Int = 0,

    val isFinished: Boolean = false,
    val actionErrorMessage: String? = null
) {
    // get(): Утгыг бусад талбараас тооцоолж авна.
    // currentCard гэсэн тусдаа хадгалсан хувьсагчтай байхгүй.
    val currentCard: FlashCard?
        get() = cards.getOrNull(currentCardIndex)

    val totalCardCount: Int
        get() = cards.size

    val answeredCount: Int
        get() = correctCount + wrongCount

    // 0f–1f утга нь progress indicator-д тохиромжтой.
    val progress: Float
        get() = if (totalCardCount == 0) {
            0f
        } else {
            answeredCount.toFloat() / totalCardCount
        }
}

class StudyViewModel(
    private val deckId: String,
    private val studyMode: StudyMode,
    private val isPractice: Boolean,
    private val repository: FlashcardRepository
) : ViewModel() {

    // MutableStateFlow: ViewModel дотроос өөрчилж болох төлөв.
    private val mutableUiState = MutableStateFlow(
        StudyUiState(
            studyMode = studyMode,
            isPractice = isPractice
        )
    )

    // asStateFlow(): Дэлгэц төлөвийг уншина.
    // Харин шууд өөрчлөх эрхгүй.
    val uiState = mutableUiState.asStateFlow()

    // Давхар ачаалах хүсэлтээс хамгаална.
    private var isLoadingCards = false

    init {
        require(deckId.isNotBlank()) {
            "StudyViewModel үүсгэхэд deckId шаардлагатай."
        }

        loadCards()
    }

    private fun loadCards() {
        if (isLoadingCards) return

        isLoadingCards = true

        mutableUiState.update {
            it.copy(
                isLoading = true,
                loadErrorMessage = null
            )
        }

        // viewModelScope: ViewModel-ийн lifecycle-тэй холбоотой coroutine.
        // Өгөгдөл ачаалах suspend функцийг энд дуудна.
        viewModelScope.launch {
            try {
                val deck = repository.getDeckById(deckId)

                requireNotNull(deck) {
                    "Суралцах багц олдсонгүй."
                }

                val cards = if (isPractice) {
                    // Practice: Багцын бүх картыг авна.
                    //
                    // first(): Flow-ийн эхний утгыг аваад
                    // ажиглалтыг зогсооно.
                    //
                    // Суралцах явцад Room өөрчлөгдөх бүрд
                    // картын жагсаалтыг дахин байгуулахгүй.
                    repository.observeCardsByDeckId(deckId).first()
                } else {
                    // Энгийн давталт: Шинэ болон хугацаа нь
                    // болсон картуудыг авна.
                    //
                    // Repository дотор 7 хоногийн дүрмийг
                    // мөн шалган шинэчилдэг.
                    repository.getStudyCards(
                        deckId = deckId,
                        currentTime = System.currentTimeMillis()
                    )
                }

                mutableUiState.value = StudyUiState(
                    studyMode = studyMode,
                    isPractice = isPractice,
                    isLoading = false,
                    cards = cards
                )
            } catch (exception: CancellationException) {
                // Coroutine цуцлагдсаныг энгийн алдаа гэж үзэхгүй.
                throw exception
            } catch (exception: Exception) {
                Log.e(
                    "StudyViewModel",
                    "Суралцах картуудыг ачаалж чадсангүй.",
                    exception
                )

                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        loadErrorMessage =
                            "Картуудыг ачаалж чадсангүй. Дахин оролдоно уу."
                    )
                }
            } finally {
                // Амжилттай эсвэл алдаатай дууссан аль ч үед ажиллана.
                isLoadingCards = false
            }
        }
    }

    fun retryLoading() {
        // Зөвхөн анхны ачаалалт алдаатай үед дахин оролдоно.
        // Үргэлжилж буй суралцах үйлдлийг санамсаргүй эхлүүлэхгүй.
        if (mutableUiState.value.loadErrorMessage == null) return

        loadCards()
    }

    private fun canAnswer(state: StudyUiState): Boolean {
        return !state.isLoading &&
                state.loadErrorMessage == null &&
                !state.isSubmitting &&
                !state.isFinished &&
                state.currentCard != null &&
                state.answerResult == null
    }

    fun flipCard() {
        val state = mutableUiState.value

        if (studyMode != StudyMode.FLASHCARD) return
        if (!canAnswer(state)) return

        // Хариултыг харуулна.
        // Зөвхөн flip хийснээр ахиц хадгалагдахгүй.
        mutableUiState.update {
            it.copy(isAnswerVisible = true)
        }
    }

    fun rateFlashcard(isRemembered: Boolean) {
        val state = mutableUiState.value

        if (studyMode != StudyMode.FLASHCARD) return
        if (!canAnswer(state)) return

        // Хариултаа харсны дараа л үнэлгээ өгнө.
        if (!state.isAnswerVisible) return

        submitAnswer(isCorrect = isRemembered)
    }

    fun onAnswerChange(answer: String) {
        val state = mutableUiState.value

        if (studyMode != StudyMode.LEARN) return
        if (!canAnswer(state)) return

        mutableUiState.update {
            it.copy(
                typedAnswer = answer,
                answerInputError = null,
                actionErrorMessage = null,

                // Хадгалалт алдаатай бол дахин бичих боломжтой.
                // Бичиж эхлэх үед өмнөх feedback-ийг хаана.
                isAnswerVisible = false
            )
        }
    }

    fun checkAnswer() {
        val state = mutableUiState.value

        if (studyMode != StudyMode.LEARN) return
        if (!canAnswer(state)) return

        val card = state.currentCard ?: return
        val enteredAnswer = state.typedAnswer.trim()

        if (enteredAnswer.isEmpty()) {
            mutableUiState.update {
                it.copy(
                    answerInputError = "Хариултаа бичнэ үү."
                )
            }
            return
        }

        // trim(): Зөвхөн эхлэл, төгсгөлийн хоосон зайг арилгана.
        // ignoreCase = true: Том, жижиг үсгийн ялгааг үл тооцно.
        //
        // Үгийн дундах зай, тэмдэгт, томьёог өөрчлөхгүй.
        val isCorrect = enteredAnswer.equals(
            other = card.answer.trim(),
            ignoreCase = true
        )

        submitAnswer(isCorrect)
    }

    private fun submitAnswer(isCorrect: Boolean) {
        val state = mutableUiState.value

        if (!canAnswer(state)) return

        val card = state.currentCard ?: return

        // Coroutine эхлэхээс ӨМНӨ true болгоно.
        // Товчийг хурдан давхар дарахад хоёр удаа хадгалахгүй.
        //
        // Learn-ийн зөв хариултыг мөн шууд дэлгэнэ.
        // Хадгалалт дуусах хүртэл дараагийн карт руу орохгүй.
        mutableUiState.update {
            it.copy(
                isSubmitting = true,
                answerInputError = null,
                actionErrorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                if (!isPractice) {
                    // Repository нь transaction дотор:
                    // 1. Картыг Room-оос дахин уншина.
                    // 2. Leitner дүрмээр ахицыг тооцоолно.
                    // 3. Картын ахицыг хадгална.
                    // 4. Deck-ийн lastStudiedAt-ийг шинэчилнэ.
                    repository.submitReview(
                        cardId = card.id,
                        isCorrect = isCorrect,
                        currentTime = System.currentTimeMillis()
                    )
                }

                // Practice үед дээрх Room хадгалалтыг алгасана.
                // Харин session-ийн зөв, буруу тоолуур ажиллана.
                //
                // Энгийн давталтад хадгалалт амжилттай
                // болсны дараа л тоолуурыг нэмнэ.
                mutableUiState.update {
                    it.copy(
                        isSubmitting = false,
                        answerResult = isCorrect,
                        isAnswerVisible = true,
                        correctCount = it.correctCount +
                                if (isCorrect) 1 else 0,
                        wrongCount = it.wrongCount +
                                if (isCorrect) 0 else 1
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(
                    "StudyViewModel",
                    "Хариултын үр дүнг хадгалж чадсангүй.",
                    exception
                )

                mutableUiState.update {
                    it.copy(
                        isSubmitting = false,
                        actionErrorMessage =
                            "Үр дүнг хадгалж чадсангүй. " +
                                    "Хариултаа дахин илгээнэ үү."
                    )
                }
            }
        }
    }

    fun nextCard() {
        val state = mutableUiState.value

        if (state.isLoading || state.isSubmitting) return
        if (state.isFinished) return

        // Хариултыг үнэлж дуусаагүй бол дараагийн карт руу орохгүй.
        if (state.answerResult == null) return

        val nextIndex = state.currentCardIndex + 1

        if (nextIndex >= state.totalCardCount) {
            mutableUiState.update {
                it.copy(isFinished = true)
            }
            return
        }

        mutableUiState.update {
            it.copy(
                currentCardIndex = nextIndex,
                typedAnswer = "",
                answerInputError = null,
                isAnswerVisible = false,
                answerResult = null,
                actionErrorMessage = null
            )
        }
    }

    fun dismissActionError() {
        mutableUiState.update {
            it.copy(actionErrorMessage = null)
        }
    }
}