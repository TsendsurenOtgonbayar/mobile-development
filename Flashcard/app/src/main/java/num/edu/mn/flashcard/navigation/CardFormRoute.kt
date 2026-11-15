package num.edu.mn.flashcard.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import num.edu.mn.flashcard.FlashcardApplication
import num.edu.mn.flashcard.di.AppViewModelFactory
import num.edu.mn.flashcard.ui.screens.CardFormScreen
import num.edu.mn.flashcard.ui.viewmodel.CardFormViewModel

@Composable
fun CardFormRoute(
    backStackEntry: NavBackStackEntry,
    onBackClick: () -> Unit,
    onSaved: (String) -> Unit
) {

    // Карт аль багцад хамаарахыг заана.
    val deckId = backStackEntry.arguments
        ?.getString("deckId")

    // null бол шинэ карт, утгатай бол засах картын ID.
    val cardId = backStackEntry.arguments
        ?.getString("cardId")

    // Буруу аргументтай бол ViewModel үүсгэхгүй.
    if (
        deckId.isNullOrBlank() ||
        (cardId != null && cardId.isBlank())
    ) {
        CardFormArgumentError(
            onBackClick = onBackClick
        )
        return
    }

    val application =
        LocalContext.current.applicationContext as FlashcardApplication

    val container = application.container
    val contentResolver = application.contentResolver

    // Сонгосон багц, картын ID-уудыг Factory-д өгнө.
    val factory = remember(container, deckId, cardId) {
        AppViewModelFactory(
            container = container,
            deckId = deckId,
            cardId = cardId
        )
    }

    // Factory-ийн create(modelClass, extras) функц
    // CardFormViewModel болон SavedStateHandle үүсгэнэ.
    val formViewModel: CardFormViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = factory
    )

    val uiState by formViewModel.uiState.collectAsStateWithLifecycle(
        lifecycleOwner = backStackEntry
    )

    var pickerErrorMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // Launcher-ийг Composable-ийн үндсэн хэсэгт бүртгэнэ.
    // Товч дарах үед зөвхөн launch() дуудна.
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->

        // Хэрэглэгч зураг сонголгүй хаасан бол
        // form-ийн өмнөх сонголтыг хэвээр үлдээнэ.
        if (uri != null) {
            try {
                // Form сэргэсэн үед Uri-г дахин унших эрх авна.
                // Энэ нь зургийг дотоод санд хуулж байгаа үйлдэл биш.
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                pickerErrorMessage = null

                formViewModel.onImageSelected(uri)

            } catch (exception: SecurityException) {
                Log.e(
                    "CardFormRoute",
                    "Зургийн эрхийг хадгалж чадсангүй.",
                    exception
                )

                pickerErrorMessage =
                    "Энэ зургийг дараа унших эрх авч чадсангүй. " +
                            "Зургаа дахин сонгоно уу."
            }
        }
    }

    // Хадгалалт дуустал системийн Back үйлдлийг түр барина.
    // Screen-ийн Back товч мөн isSaving үед идэвхгүй.
    BackHandler(enabled = uiState.isSaving) {
        // Хадгалалт дуусахыг хүлээнэ.
    }

    // Effect дотор callback-ийн хамгийн сүүлийн хувилбарыг ашиглана.
    val currentOnSaved by rememberUpdatedState(onSaved)

    // Хадгалалт амжилттай болсон үед л буцах үйлдэл хийнэ.
    LaunchedEffect(uiState.savedCardId) {
        val savedCardId = uiState.savedCardId

        if (savedCardId != null) {
            currentOnSaved(savedCardId)
        }
    }

    CardFormScreen(
        uiState = uiState,
        imageStorage = container.imageStorage,

        onQuestionChange = { newQuestion ->
            formViewModel.onQuestionChange(newQuestion)
        },

        onAnswerChange = { newAnswer ->
            formViewModel.onAnswerChange(newAnswer)
        },

        onPickImageClick = {
            pickerErrorMessage = null

            try {
                photoPicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )

            } catch (exception: ActivityNotFoundException) {
                Log.e(
                    "CardFormRoute",
                    "Зураг сонгох дэлгэц нээгдсэнгүй.",
                    exception
                )

                pickerErrorMessage =
                    "Зураг сонгох дэлгэцийг нээж чадсангүй."
            }
        },

        onRemoveImageClick = {
            formViewModel.onRemoveImage()
        },

        onSaveClick = {
            formViewModel.saveCard()
        },

        onBackClick = onBackClick,

        onRetryLoadingClick = {
            formViewModel.retryLoading()
        },

        onDismissSaveError = {
            formViewModel.dismissSaveError()
        },

        // Ахицын dialog-ийн хоёр үндсэн сонголтыг холбоно.
        onProgressChoice = { resetProgress ->
            formViewModel.confirmProgressChoice(
                resetProgress = resetProgress
            )
        },

        // "Болих" → form дээр үлдэнэ, хадгалахгүй.
        onCancelProgressChoice = {
            formViewModel.cancelProgressChoice()
        }
    )

    val pickerError = pickerErrorMessage

    if (pickerError != null) {
        AlertDialog(
            onDismissRequest = {
                pickerErrorMessage = null
            },

            title = {
                Text("Зураг сонгож чадсангүй")
            },

            text = {
                Text(pickerError)
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        pickerErrorMessage = null
                    }
                ) {
                    Text("Ойлголоо")
                }
            }
        )
    }
}

@Composable
private fun CardFormArgumentError(
    onBackClick: () -> Unit
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
            Text("Багц эсвэл картын ID буруу байна.")

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onBackClick) {
                Text("Буцах")
            }
        }
    }
}