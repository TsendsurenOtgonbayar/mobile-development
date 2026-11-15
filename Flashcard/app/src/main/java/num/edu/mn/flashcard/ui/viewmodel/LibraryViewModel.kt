package num.edu.mn.flashcard.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import num.edu.mn.flashcard.data.models.Deck
import num.edu.mn.flashcard.data.models.DeckStatistics
import num.edu.mn.flashcard.data.repository.FlashcardRepository

// Library дэлгэцэд харуулах бүх мэдээлэл.
data class LibraryUiState(
    val isLoading: Boolean = true,

    val decks: List<Deck> = emptyList(),

    // Багцын ID-аар статистикийг нь олно.
    //
    // Жишээ:
    // deckStatistics["deck-1"] → deck-1 багцын статистик.
    val deckStatistics: Map<String, DeckStatistics> = emptyMap(),

    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: FlashcardRepository
) : ViewModel() {

    // Гараар дахин ачаалах хүсэлтийг илэрхийлнэ.
    private val reloadTrigger = MutableStateFlow(0)

    val uiState: StateFlow<LibraryUiState> = reloadTrigger

        // refresh() дуудагдвал өмнөх ажиглалтыг цуцлаад
        // шинэ хугацаагаар дахин эхлүүлнэ.
        .flatMapLatest {

            observeCurrentTime()

                // Шинэ цаг ирэхэд хугацаанаас хамаарах
                // статистикийн ажиглалтыг дахин байгуулна.
                .flatMapLatest { currentTime ->
                    observeLibrary(currentTime)
                }

                // Анх эсвэл гараар refresh хийхэд loading харуулна.
                // Минут тутмын шинэчлэлт бүрд loading харуулахгүй.
                .onStart {
                    emit(LibraryUiState())
                }

                .catch { exception ->
                    Log.e(
                        "LibraryViewModel",
                        "Багц болон статистикийг уншиж чадсангүй.",
                        exception
                    )

                    emit(
                        LibraryUiState(
                            isLoading = false,
                            errorMessage =
                                "Багцуудыг ачаалж чадсангүй. Дахин оролдоно уу."
                        )
                    )
                }
        }

        .stateIn(
            scope = viewModelScope,

            started = SharingStarted.WhileSubscribed(

                // UI ажиглахаа больсноос 5 секундийн дараа
                // database болон цагийн урсгалуудыг зогсооно.
                stopTimeoutMillis = 5_000,

                // Урсгал зогссоны дараа хуучин UI төлөвийг
                // initialValue буюу loading төлөвт буцаана.
                replayExpirationMillis = 0
            ),

            initialValue = LibraryUiState()
        )

    // Одоогийн цагийг анх шууд, дараа нь минут тутам өгнө.
    //
    // Энэ нь background notification эсвэл alarm биш.
    // UI-ийн ажиглалт идэвхтэй байх үед л ажиллах урсгал.
    private fun observeCurrentTime(): Flow<Long> = flow {

        while (true) {
            emit(System.currentTimeMillis())

            // delay нь thread-ийг блоклохгүй.
            // Coroutine цуцлагдвал хүлээлт мөн цуцлагдана.
            delay(60_000L)
        }
    }

    // Багцууд болон тэдгээрийн статистикийг нэг UI төлөв болгоно.
    private fun observeLibrary(
        currentTime: Long
    ): Flow<LibraryUiState> {

        return repository.observeDecks()
            .flatMapLatest { decks ->

                // Багцгүй үед combine хийх урсгал байхгүй.
                // Тиймээс хоосон жагсаалтын төлөв шууд буцаана.
                if (decks.isEmpty()) {

                    flowOf(
                        LibraryUiState(
                            isLoading = false
                        )
                    )

                } else {

                    // Багц бүрт статистикийн Flow үүсгэнэ.
                    //
                    // Repository эхлээд 7 хоногийн дүрмийг
                    // хэрэгжүүлээд статистикийг ажигладаг.
                    val statisticsFlows:
                            List<Flow<Pair<String, DeckStatistics>>> =
                        decks.map { deck ->

                            repository.observeDeckStatistics(
                                deckId = deck.id,
                                currentTime = currentTime
                            ).map { statistics ->

                                // Аль багцын статистик болохыг
                                // ID-тай нь хамт хадгална.
                                Pair(deck.id, statistics)
                            }
                        }

                    // combine:
                    // Flow бүрээс анхны утга ирсний дараа
                    // тэдгээрийг нэг үр дүн болгон нэгтгэнэ.
                    //
                    // Аль нэг багцын статистик өөрчлөгдвөл
                    // бусдын хамгийн сүүлийн утгатай нэгтгээд
                    // шинэ LibraryUiState гаргана.
                    combine(statisticsFlows) { statisticsPairs ->

                        LibraryUiState(
                            isLoading = false,
                            decks = decks,

                            // Pair-уудыг Map болгон хувиргана.
                            // key = deckId, value = статистик.
                            deckStatistics = statisticsPairs.toMap()
                        )
                    }
                }
            }
    }

    // Дэлгэц дахин идэвхжихэд шинэ цаг ашиглан
    // өгөгдлөө дахин шалгахад ашиглана.
    //
    // Дараа нь Navigation дээр lifecycle-тэй холбоно.
    fun refresh() {
        reloadTrigger.update { currentValue ->
            currentValue + 1
        }
    }

    // Алдааны дэлгэцийн "Дахин оролдох" товч.
    fun retry() {
        refresh()
    }
}