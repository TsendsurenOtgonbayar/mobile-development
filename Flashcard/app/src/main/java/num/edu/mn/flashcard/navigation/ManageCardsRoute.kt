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
import num.edu.mn.flashcard.ui.screens.ManageCardsScreen
import num.edu.mn.flashcard.ui.viewmodel.DeckViewModel

@Composable
fun ManageCardsRoute(
    backStackEntry: NavBackStackEntry,
    onBackClick: () -> Unit,
    onAddCardClick: () -> Unit,
    onEditCardClick: (String) -> Unit
) {

    val deckId = backStackEntry.arguments
        ?.getString("deckId")

    // Картын жагсаалт заавал нэг багцад хамаарна.
    if (deckId.isNullOrBlank()) {
        ManageCardsRouteMessage(
            message = "Багцын ID дамжуулагдсангүй.",
            onBackClick = onBackClick
        )
        return
    }

    val application =
        LocalContext.current.applicationContext as FlashcardApplication

    val container = application.container

    val factory = remember(container, deckId) {
        AppViewModelFactory(
            container = container,
            deckId = deckId
        )
    }

    // Deck Detail-тэй ижил ViewModel CLASS ашиглаж байна.
    //
    // Гэхдээ энэ navigation entry өөрийн INSTANCE-тай.
    // Хоёр дэлгэцийн түр UI төлөв хоорондоо холилдохгүй.
    //
    // Харин ижил Repository, Room ашиглаж байгаа тул
    // хадгалсан өгөгдлийн өөрчлөлт хоёуланд нь харагдана.
    val deckViewModel: DeckViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = factory
    )

    val uiState by deckViewModel.uiState.collectAsStateWithLifecycle(
        lifecycleOwner = backStackEntry
    )

    // Card Form-оос буцаж ирэх эсвэл апп дахин идэвхжихэд
    // одоогийн цагаар суралцах төлөвийг дахин шалгана.
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

    val deck = uiState.deck

    when {

        uiState.isLoading -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFE8FAFF)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF276B91)
                    )
                }
            }
        }

        uiState.errorMessage != null -> {
            ManageCardsRouteMessage(
                message = uiState.errorMessage
                    ?: "Картуудыг ачаалж чадсангүй.",
                onBackClick = onBackClick,
                onRetryClick = {
                    deckViewModel.retry()
                }
            )
        }

        uiState.isDeckMissing || deck == null -> {
            ManageCardsRouteMessage(
                message = "Энэ багц олдсонгүй. Устгагдсан байж болно.",
                onBackClick = onBackClick
            )
        }

        else -> {
            ManageCardsScreen(
                deck = deck,
                cards = uiState.cards,
                imageStorage = container.imageStorage,
                deletingCardId = uiState.deletingCardId,

                onBackClick = onBackClick,

                onAddCardClick = onAddCardClick,

                onEditCardClick = { cardId ->
                    onEditCardClick(cardId)
                },

                // Screen-ийн баталгаажуулах dialog дээр
                // "Устгах" дарсны дараа энэ callback ажиллана.
                onDeleteCardClick = { cardId ->
                    deckViewModel.deleteCard(cardId)
                }
            )
        }
    }

    // Устгах үйлдлийн алдааг жагсаалтыг солихгүйгээр харуулна.
    val actionError = uiState.actionErrorMessage

    if (actionError != null) {
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

@Composable
private fun ManageCardsRouteMessage(
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