package num.edu.mn.flashcard.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import num.edu.mn.flashcard.FlashcardApplication
import num.edu.mn.flashcard.di.AppViewModelFactory
import num.edu.mn.flashcard.ui.screens.LibraryScreen
import num.edu.mn.flashcard.ui.viewmodel.LibraryViewModel

// Энэ Composable нь ViewModel болон Screen-ийг холбоно.
// UI-ийн Card, TextField зэрэг харагдах хэсгийг энд бичихгүй.
@Composable
fun LibraryRoute(
    backStackEntry: NavBackStackEntry,
    onDeckClick: (String) -> Unit,
    onAddDeckClick: () -> Unit
) {

    // Одоогийн Android орчноос Application объектыг авна.
    //
    // Manifest дээр android:name=".FlashcardApplication"
    // гэж бүртгэсэн учраас манай Application class байх ёстой.
    val application =
        LocalContext.current.applicationContext as FlashcardApplication

    // Application-ийн эзэмшиж байгаа нэг container.
    // Шинэ AppContainer үүсгэхгүй.
    val container = application.container

    // Recomposition бүрд Factory-г дахин үүсгэхгүй.
    //
    // remember нь зөвхөн Factory объектыг энд дахин ашиглуулна.
    // ViewModel-ийн амьдралын хугацааг remember удирдахгүй.
    val factory = remember(container) {
        AppViewModelFactory(container)
    }

    // Энэ navigation entry-д хамаарах ViewModel авна.
    //
    // Өмнө үүссэн бол Android тэр объектыг буцаана.
    // Үүсээгүй бол factory.create()-ийг ашиглана.
    //
    // backStackEntry back stack-аас бүрэн хасагдахад
    // түүнд хамаарах ViewModel цэвэрлэгдэнэ.
    val libraryViewModel: LibraryViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = factory
    )

    // StateFlow-г Compose-ийн ажигладаг State болгон авна.
    //
    // lifecycle нь STARTED эсвэл түүнээс идэвхтэй үед ажиглана.
    // UI идэвхгүй үед ажиглалтыг түр зогсооно.
    //
    // by ашигласнаар uiState.value гэж бичихгүй.
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle(
        lifecycleOwner = backStackEntry
    )

    // Дэлгэц дахин RESUMED болох үед одоогийн цагаар шинэчилнэ.
    //
    // Жишээ:
    // - Өөр дэлгэцээс Library руу буцах.
    // - Аппыг background-оос дахин нээх.
    DisposableEffect(backStackEntry, libraryViewModel) {

        val lifecycle = backStackEntry.lifecycle

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {
                libraryViewModel.refresh()
            }
        }

        lifecycle.addObserver(observer)

        // Observer нэмэх үед entry аль хэдийн RESUMED байсан бол
        // шинэчилж байгаа эсэхийг баталгаажуулна.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            libraryViewModel.refresh()
        }

        // Энэ effect composition-оос гарах үед observer-ийг салгана.
        // Ингэснээр хуучин observer давхар үлдэхгүй.
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // Бэлдсэн төлөв болон үйлдлүүдийг UI-д дамжуулна.
    LibraryScreen(
        uiState = uiState,
        imageStorage = container.imageStorage,
        deckStatistics = uiState.deckStatistics,

        onDeckClick = onDeckClick,
        onAddDeckClick = onAddDeckClick,

        onRetryClick = {
            libraryViewModel.retry()
        }
    )
}