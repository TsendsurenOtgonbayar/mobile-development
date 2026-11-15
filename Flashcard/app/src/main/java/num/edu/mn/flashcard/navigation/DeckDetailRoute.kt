package num.edu.mn.flashcard.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import num.edu.mn.flashcard.FlashcardApplication
import num.edu.mn.flashcard.di.AppViewModelFactory
import num.edu.mn.flashcard.ui.screens.DeckDetailScreen
import num.edu.mn.flashcard.ui.viewmodel.DeckViewModel

@Composable
fun DeckDetailRoute(
    backStackEntry: NavBackStackEntry,
    onBackClick: () -> Unit,
    onFlashcardClick: (Boolean) -> Unit,
    onLearnClick: (Boolean) -> Unit,
    onManageCardsClick: () -> Unit,
    onEditDeckClick: () -> Unit
) {

    // "deck/{deckId}" route-ийн параметрийг авна.
    val deckId = backStackEntry.arguments
        ?.getString("deckId")

    // Буруу route эсвэл дутуу аргумент байвал
    // ViewModel үүсгэхээс өмнө мэдээлэл харуулна.
    if (deckId.isNullOrBlank()) {
        DeckRouteMessage(
            message = "Багцын ID дамжуулагдсангүй.",
            onBackClick = onBackClick
        )

        // Энэ Composable-ийн үлдсэн хэсгийг ажиллуулахгүй.
        return
    }

    val application =
        LocalContext.current.applicationContext as FlashcardApplication

    val container = application.container

    // Тухайн deckId-д тохирох Factory.
    // Өөр багцын ID ирвэл шинэ Factory үүснэ.
    val factory = remember(container, deckId) {
        AppViewModelFactory(
            container = container,
            deckId = deckId
        )
    }

    // Энэ navigation entry өөрийн DeckViewModel-той байна.
    val deckViewModel: DeckViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = factory
    )

    val uiState by deckViewModel.uiState.collectAsStateWithLifecycle(
        lifecycleOwner = backStackEntry
    )

    // Дэлгэц дахин идэвхжих үед шинэ цагаар ахиц шалгана.
    DisposableEffect(backStackEntry, deckViewModel) {

        val lifecycle = backStackEntry.lifecycle

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                deckViewModel.refresh()
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // Navigation бол UI-г зурахаас тусдаа side effect.
    //
    // isDeleted өөрчлөгдөхөд энэ блок ажиллана.
    // Энгийн recomposition бүрд popBackStack дуудахгүй.
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBackClick()
        }
    }

    // Local val ашигласнаар доор null шалгалтын дараа
    // compiler deck-ийг null биш гэж танина.
    val deck = uiState.deck

    when {

        // Устгасны дараа navigation буцах зуур
        // "Багц олдсонгүй" гэсэн төлөв үзүүлэхгүй.
        uiState.isDeleted -> {
            DeckRouteProgress(message = "Багц устгагдлаа…")
        }

        // Database-аас багц арилсан ч зураг цэвэрлэх зэрэг
        // Repository-ийн үлдсэн ажил үргэлжилж байж болно.
        uiState.isDeleting && deck == null -> {
            DeckRouteProgress(message = "Багцыг устгаж байна…")
        }

        uiState.isLoading -> {
            DeckRouteProgress(message = "Багцыг ачаалж байна…")
        }

        uiState.errorMessage != null -> {
            DeckRouteMessage(
                message = uiState.errorMessage
                    ?: "Багцын мэдээллийг ачаалж чадсангүй.",
                onBackClick = onBackClick,
                onRetryClick = {
                    deckViewModel.retry()
                }
            )
        }

        uiState.isDeckMissing || deck == null -> {
            DeckRouteMessage(
                message = "Энэ багц олдсонгүй. Устгагдсан байж болно.",
                onBackClick = onBackClick
            )
        }

        else -> {
            DeckDetailScreen(
                deck = deck,
                statistics = uiState.statistics,
                imageStorage = container.imageStorage,
                isDeleting = uiState.isDeleting,

                onBackClick = onBackClick,
                onFlashcardClick = onFlashcardClick,
                onLearnClick = onLearnClick,
                onManageCardsClick = onManageCardsClick,
                onEditDeckClick = onEditDeckClick,

                // Screen өөрийн баталгаажуулах dialog-ийн
                // "Устгах" товч дарагдсаны дараа үүнийг дуудна.
                onDeleteDeckClick = {
                    deckViewModel.deleteDeck()
                }
            )
        }
    }

    // Унших алдаа болон устгах алдаа өөр.
    // Устгах алдаа гарвал дэлгэцийг бүхэлд нь солихгүй,
    // мэдээллийн dialog харуулна.
    val actionError = uiState.actionErrorMessage

    if (actionError != null && !uiState.isDeleted) {
        AlertDialog(
            onDismissRequest = {
                deckViewModel.dismissActionError()
            },

            title = {
                Text("Үйлдэл амжилтгүй")
            },

            text = {
                Text(actionError)
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        deckViewModel.dismissActionError()
                    }
                ) {
                    Text("Ойлголоо")
                }
            }
        )
    }
}

// Loading болон устгаж байгаа үеийн жижиг UI.
@Composable
private fun DeckRouteProgress(message: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFE8FAFF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF276B91)
            )

            Spacer(Modifier.height(16.dp))

            Text(message)
        }
    }
}

// Алдаа эсвэл багц олдоогүй үеийн UI.
// onRetryClick null бол дахин оролдох товч харуулахгүй.
@Composable
private fun DeckRouteMessage(
    message: String,
    onBackClick: () -> Unit,
    onRetryClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFE8FAFF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(message)

            Spacer(Modifier.height(16.dp))

            if (onRetryClick != null) {
                Button(onClick = onRetryClick) {
                    Text("Дахин оролдох")
                }
            }

            TextButton(onClick = onBackClick) {
                Text("Буцах")
            }
        }
    }
}