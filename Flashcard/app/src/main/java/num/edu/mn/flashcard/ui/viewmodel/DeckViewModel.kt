package num.edu.mn.flashcard.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import num.edu.mn.flashcard.data.models.Deck
import num.edu.mn.flashcard.data.models.DeckStatistics
import num.edu.mn.flashcard.data.models.FlashCard
import num.edu.mn.flashcard.data.repository.FlashcardRepository

// Нэг багцын дэлгэцэд шаардлагатай мэдээлэл.
data class DeckUiState(
    val isLoading: Boolean = true,

    // Ачаалж байх үед эсвэл багц олдохгүй бол null.
    val deck: Deck? = null,

    val cards: List<FlashCard> = emptyList(),

    val statistics: DeckStatistics = DeckStatistics(),

    // Database-д энэ ID-тай багц байхгүй.
    val isDeckMissing: Boolean = false,

    // Өгөгдөл унших үед гарсан алдаа.
    val errorMessage: String? = null,

    // Устгах үйлдлийн төлөвүүд.
    val isDeleting: Boolean = false,
    val deletingCardId: String? = null,
    val isDeleted: Boolean = false,
    val actionErrorMessage: String? = null
)

// Database-аас ирэх мэдээллээс тусдаа,
// хэрэглэгчийн үйлдлийн төлөвийг хадгална.
//
// private: зөвхөн энэ файл дотор ашиглана.
// Database-аас ирэх өгөгдлөөс тусдаа үйлдлийн төлөв.
private data class DeckActionState(

    // Багцыг бүхэлд нь устгах үйлдэл.
    val isDeleting: Boolean = false,

    // Нэг карт устгах үйлдэл.
    val deletingCardId: String? = null,

    // Багц амжилттай устсан эсэх.
    val isDeleted: Boolean = false,

    // Багц эсвэл карт устгах үед гарсан алдаа.
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class DeckViewModel(
    private val deckId: String,
    private val repository: FlashcardRepository
) : ViewModel() {

    // Нэг ViewModel нэг багцын ID-тай ажиллана.
    init {
        require(deckId.isNotBlank()) {
            "Багцын ID хоосон байж болохгүй."
        }
    }

    private val reloadTrigger = MutableStateFlow(0)

    private val actionState = MutableStateFlow(DeckActionState())

    // Database-аас ирэх мэдээллийн урсгал.
    private val contentFlow: Flow<DeckUiState> = reloadTrigger
        .flatMapLatest {

            observeCurrentTime()
                .flatMapLatest { currentTime ->
                    observeDeckContent(currentTime)
                }

                // Анх болон гараар refresh хийхэд loading гарна.
                // Минут тутмын шинэчлэлт бүрд loading гаргахгүй.
                .onStart {
                    emit(DeckUiState())
                }

                .catch { exception ->
                    Log.e(
                        "DeckViewModel",
                        "Багцын мэдээллийг уншиж чадсангүй.",
                        exception
                    )

                    emit(
                        DeckUiState(
                            isLoading = false,
                            errorMessage =
                                "Багцын мэдээллийг ачаалж чадсангүй."
                        )
                    )
                }
        }

    // Database-ийн мэдээлэл болон үйлдлийн төлөвийг нэгтгэнэ.
    //
    // Жишээлбэл статистик өөрчлөгдсөн ч isDeleting төлөв
    // санамсаргүй false болж алдагдахгүй.
    val uiState: StateFlow<DeckUiState> = combine(
        contentFlow,
        actionState
    ) { content, action ->

        content.copy(
            isDeleting = action.isDeleting,

            // Үйлдлийн төлөвийг UI-д дамжуулна.
            deletingCardId = action.deletingCardId,

            isDeleted = action.isDeleted,
            actionErrorMessage = action.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = 5_000,
            replayExpirationMillis = 0
        ),
        initialValue = DeckUiState()
    )

    // Хугацаанаас хамаарах мэдээллийг минут тутам шинэчилнэ.
    // UI-ийн ажиглалт зогсоход энэ урсгал мөн зогсоно.
    private fun observeCurrentTime(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(60_000L)
        }
    }

    private fun observeDeckContent(
        currentTime: Long
    ): Flow<DeckUiState> {

        return repository.observeDeckById(deckId)
            .flatMapLatest { deck ->

                if (deck == null) {

                    // Багц олдоогүй гэдгийг loading эсвэл
                    // database алдаанаас тусад нь илэрхийлнэ.
                    flowOf(
                        DeckUiState(
                            isLoading = false,
                            isDeckMissing = true
                        )
                    )

                } else {

                    // Нэг багцын картууд болон статистикийг ажиглана.
                    combine(
                        repository.observeCardsByDeckId(deckId),

                        repository.observeDeckStatistics(
                            deckId = deckId,
                            currentTime = currentTime
                        )
                    ) { cards, statistics ->

                        DeckUiState(
                            isLoading = false,
                            deck = deck,
                            cards = cards,
                            statistics = statistics
                        )
                    }
                }
            }
    }

    // Дэлгэц дахин идэвхжихэд шинэ цаг ашиглан дахин шалгана.
    fun refresh() {
        reloadTrigger.update { currentValue ->
            currentValue + 1
        }
    }

    fun retry() {
        refresh()
    }

    // UI дээр хэрэглэгч устгахыг баталгаажуулсны дараа дуудна.
    // Баталгаажуулах dialog-ийг ViewModel өөрөө харуулахгүй.
    fun deleteDeck() {

        val currentAction = actionState.value

        // Товчийг давхар дарахад хоёр устгал зэрэг эхлэхгүй.
        // Аль хэдийн устсан бол мөн дахин ажиллуулахгүй.
        // Карт устгаж байх үед багцыг давхар устгаж эхлэхгүй.
        if (
            currentAction.isDeleting ||
            currentAction.deletingCardId != null ||
            currentAction.isDeleted
        ) {
            return
        }
        // Coroutine эхлэхээс өмнө үйлдлийг эхэлсэн гэж тэмдэглэнэ.
        actionState.update { state ->
            state.copy(
                isDeleting = true,
                errorMessage = null
            )
        }

        // launch: ViewModel-ийн coroutine scope-д ажил эхлүүлнэ.
        // Ингэснээр suspend Repository функцийг дуудаж болно.
        viewModelScope.launch {

            try {
                repository.deleteDeck(deckId)

                // Navigation-ийг энд шууд дуудахгүй.
                // Route энэ төлөвийг хараад өмнөх дэлгэц рүү буцна.
                actionState.update { state ->
                    state.copy(
                        isDeleted = true
                    )
                }

            } catch (exception: CancellationException) {

                // Coroutine цуцлагдсаныг хэрэглэгчийн алдаа гэж үзэхгүй.
                // Цуцлалтын дохиог цааш дамжуулна.
                throw exception

            } catch (exception: Exception) {

                Log.e(
                    "DeckViewModel",
                    "Багцыг устгаж чадсангүй.",
                    exception
                )

                actionState.update { state ->
                    state.copy(
                        errorMessage =
                            "Багцыг устгаж чадсангүй. Дахин оролдоно уу."
                    )
                }

            } finally {

                // Амжилттай эсвэл алдаатай дууссан аль ч үед
                // устгаж байгаа төлөвийг унтраана.
                actionState.update { state ->
                    state.copy(
                        isDeleting = false
                    )
                }
            }
        }
    }
    // Manage Cards дэлгэцийн баталгаажуулах dialog-оос дуудна.
    fun deleteCard(cardId: String) {

        val currentAction = actionState.value

        // Нэг устгал үргэлжилж байвал дахин эхлүүлэхгүй.
        // Мөн багц аль хэдийн устсан бол карт устгахгүй.
        if (
            currentAction.isDeleting ||
            currentAction.deletingCardId != null ||
            currentAction.isDeleted
        ) {
            return
        }

        if (cardId.isBlank()) {
            actionState.update { state ->
                state.copy(
                    errorMessage = "Устгах картын ID буруу байна."
                )
            }
            return
        }

        // Coroutine эхлэхээс өмнө үйлдэл эхэлснийг тэмдэглэнэ.
        // Давхар товшилтоор хоёр устгал зэрэг эхлэхээс хамгаална.
        actionState.update { state ->
            state.copy(
                deletingCardId = cardId,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                // Database дахь одоогийн картыг шалгана.
                val card = repository.getCardById(cardId)

                // Аль хэдийн устсан бол дахин устгах шаардлагагүй.
                // return@launch хийсэн ч finally ажиллана.
                if (card == null) {
                    return@launch
                }

                // Энэ ViewModel зөвхөн өөрийн багцын картыг устгана.
                require(card.deckId == deckId) {
                    "Карт сонгосон багцад хамаарахгүй байна."
                }

                // Repository:
                // 1. Room-оос картыг устгана.
                // 2. Түүний зургийн файлыг цэвэрлэнэ.
                repository.deleteCard(cardId)

                // Энд cards жагсаалтаас гараар хасахгүй.
                // Room-ийн Flow шинэ жагсаалтыг дамжуулна.
                //
                // Mastered тоо болон Mastery ч
                // статистикийн query-гаар дахин тооцогдоно.

            } catch (exception: CancellationException) {

                // Coroutine цуцлагдсаныг ердийн алдаа гэж үзэхгүй.
                throw exception

            } catch (exception: Exception) {

                Log.e(
                    "DeckViewModel",
                    "Картыг устгаж чадсангүй.",
                    exception
                )

                actionState.update { state ->
                    state.copy(
                        errorMessage =
                            "Картыг устгаж чадсангүй. Дахин оролдоно уу."
                    )
                }

            } finally {

                // Амжилттай, алдаатай эсвэл карт олдоогүй
                // аль ч тохиолдолд progress төлөвийг цэвэрлэнэ.
                actionState.update { state ->
                    state.copy(
                        deletingCardId = null
                    )
                }
            }
        }
    }
    // Snackbar эсвэл dialog-ийн алдааг хаасны дараа дуудна.
    fun dismissActionError() {
        actionState.update { state ->
            state.copy(
                errorMessage = null
            )
        }
    }
}