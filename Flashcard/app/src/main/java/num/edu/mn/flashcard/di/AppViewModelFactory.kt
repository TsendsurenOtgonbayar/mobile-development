package num.edu.mn.flashcard.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import num.edu.mn.flashcard.ui.viewmodel.CardFormViewModel
import num.edu.mn.flashcard.ui.viewmodel.DeckFormViewModel
import num.edu.mn.flashcard.ui.viewmodel.DeckViewModel
import num.edu.mn.flashcard.ui.viewmodel.LibraryViewModel
import num.edu.mn.flashcard.ui.viewmodel.StudyMode
import num.edu.mn.flashcard.ui.viewmodel.StudyViewModel

// Factory нь ViewModel үүсгэхэд шаардлагатай
// Repository болон бусад параметрийг дамжуулж өгнө.
//
// ViewModelProvider нь ViewModel-ийн lifecycle-ийг удирдана.
// Энэ Factory нь зөвхөн шинэ instance хэрэгтэй үед үүсгэнэ.
class AppViewModelFactory(
    private val container: AppContainer,

    // Library-д хэрэггүй.
    // Deck болон Study төрлийн ViewModel-д шаардлагатай.
    // Form дээр null байвал шинэ deck үүсгэнэ.
    private val deckId: String? = null,

    // null байвал шинэ карт үүсгэнэ.
    // Утгатай байвал тухайн картыг засна.
    private val cardId: String? = null,

    // Зөвхөн StudyViewModel үүсгэх үед дамжуулна.
    // FLASHCARD эсвэл LEARN байна.
    private val studyMode: StudyMode? = null,

    // false: Ахиц, давтах хугацааг Room-д хадгална.
    // true: Practice тул ахиц, давтах хугацааг өөрчлөхгүй.
    private val isPractice: Boolean = false
) : ViewModelProvider.Factory {

    // SavedStateHandle шаарддаггүй ViewModel-уудыг үүсгэнэ.
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        // when: Хүссэн ViewModel-ийн төрлөөр сонголт хийнэ.
        val viewModel: ViewModel = when (modelClass) {

            LibraryViewModel::class.java -> {
                LibraryViewModel(
                    repository = container.flashcardRepository
                )
            }

            DeckViewModel::class.java -> {
                DeckViewModel(
                    deckId = requireDeckId(),
                    repository = container.flashcardRepository
                )
            }

            StudyViewModel::class.java -> {
                // Горимыг мартаж дамжуулсан бол
                // шалтгааныг нь тодорхой хэлж зогсооно.
                val selectedStudyMode = requireNotNull(studyMode) {
                    "StudyViewModel үүсгэхэд studyMode шаардлагатай."
                }

                StudyViewModel(
                    deckId = requireDeckId(),
                    studyMode = selectedStudyMode,
                    isPractice = isPractice,
                    repository = container.flashcardRepository
                )
            }

            DeckFormViewModel::class.java,
            CardFormViewModel::class.java -> {
                // Form-ийн бичиж буй утгууд SavedStateHandle ашигладаг.
                // Тиймээс CreationExtras-тай create() хэрэгтэй.
                throw IllegalArgumentException(
                    "${modelClass.simpleName} үүсгэхэд " +
                            "CreationExtras шаардлагатай."
                )
            }

            else -> {
                throw IllegalArgumentException(
                    "Энэ ViewModel Factory-д бүртгэгдээгүй: " +
                            modelClass.name
                )
            }
        }

        // Дээр modelClass-аар яг ямар төрөл хүссэнийг шалгасан.
        // Гэхдээ compiler generic T төрлийг баталж чадахгүй тул
        // cast-ийн warning-ийг зөвхөн энэ хэсэгт дарна.
        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }

    // CreationExtras нь SavedStateHandle үүсгэхэд хэрэгтэй
    // lifecycle, saved-state owner болон default arguments
    // зэрэг мэдээллийг агуулна.
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {

        val viewModel: ViewModel = when (modelClass) {

            DeckFormViewModel::class.java -> {
                DeckFormViewModel(
                    deckId = deckId,
                    repository = container.flashcardRepository,

                    // Android-ийн өгсөн мэдээлэлтэй холбож үүсгэнэ.
                    // SavedStateHandle()-ийг гараар шинээр үүсгэхгүй.
                    savedStateHandle = extras.createSavedStateHandle()
                )
            }

            CardFormViewModel::class.java -> {
                CardFormViewModel(
                    deckId = requireDeckId(),
                    cardId = cardId,
                    repository = container.flashcardRepository,
                    savedStateHandle = extras.createSavedStateHandle()
                )
            }

            else -> {
                // Library, Deck, Study ViewModel-д
                // SavedStateHandle одоогоор шаардлагагүй.
                //
                // return: Нөгөө create() функцийн үр дүнг
                // шууд буцааж, энэ функцийг дуусгана.
                return create(modelClass)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }

    // Давтагдах deckId шалгалтыг нэг газар байрлуулсан.
    // private: Зөвхөн энэ class дотроос дуудна.
    private fun requireDeckId(): String {
        val selectedDeckId = requireNotNull(deckId) {
            "Энэ ViewModel-ийг үүсгэхэд deckId шаардлагатай."
        }

        // null биш боловч "" эсвэл "   " байж болох тул
        // хоосон эсэхийг тусад нь шалгана.
        require(selectedDeckId.isNotBlank()) {
            "deckId хоосон байж болохгүй."
        }

        return selectedDeckId
    }
}