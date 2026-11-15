package num.edu.mn.flashcard.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import num.edu.mn.flashcard.FlashcardApplication
import num.edu.mn.flashcard.di.AppViewModelFactory
import num.edu.mn.flashcard.ui.screens.DeckFormScreen
import num.edu.mn.flashcard.ui.viewmodel.DeckFormViewModel

@Composable
fun DeckFormRoute(
    backStackEntry: NavBackStackEntry,
    onBackClick: () -> Unit,
    onSaved: (String) -> Unit
) {

    // null → шинэ багц.
    // Утгатай → байгаа багцыг засах.
    val deckId = backStackEntry.arguments
        ?.getString("deckId")

    val application =
        LocalContext.current.applicationContext as FlashcardApplication

    val container = application.container
    val contentResolver = application.contentResolver

    val factory = remember(container, deckId) {
        AppViewModelFactory(
            container = container,
            deckId = deckId
        )
    }

    // Navigation entry нь SavedStateHandle үүсгэхэд хэрэгтэй
    // CreationExtras мэдээллийг ViewModelProvider-д өгнө.
    //
    // Өмнө Factory дээр бичсэн create(modelClass, extras)
    // overload энд ашиглагдана.
    val formViewModel: DeckFormViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = factory
    )

    val uiState by formViewModel.uiState.collectAsStateWithLifecycle(
        lifecycleOwner = backStackEntry
    )

    // Picker нээх/эрх авах алдаа нь Android UI-тэй холбоотой.
    // Тиймээс энэ жижиг төлөвийг Route дээр хадгална.
    var pickerErrorMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // Launcher-ийг товч дарах үед биш,
    // Composable-ийн үндсэн хэсэгт бүртгэнэ.
    val photoPicker = rememberLauncherForActivityResult(

        // Нэг зураг эсвэл видео сонгох contract.
        // launch хийхдээ ImageOnly гэж хязгаарлана.
        contract = ActivityResultContracts.PickVisualMedia()

    ) { uri ->

        // null бол хэрэглэгч зураг сонголгүй picker-ийг хаасан.
        // Өмнөх сонголт болон хадгалсан зургийг өөрчлөхгүй.
        if (uri != null) {

            try {
                // Сонгосон Uri-г дараа дахин унших эрхийг хадгална.
                //
                // Энэ нь зургийг дотоод санд хуулж байгаа биш.
                // Хуулах ажил Save дарахад Repository дээр хийгдэнэ.
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                pickerErrorMessage = null

                // Зөвшөөрөл амжилттай авсны дараа
                // шинэ сонголтыг ViewModel-д өгнө.
                formViewModel.onImageSelected(uri)

            } catch (exception: SecurityException) {

                Log.e(
                    "DeckFormRoute",
                    "Сонгосон зургийн эрхийг хадгалж чадсангүй.",
                    exception
                )

                // Шинэ сонголтыг ViewModel-д дамжуулаагүй тул
                // өмнөх зураг хэвээр үлдэнэ.
                pickerErrorMessage =
                    "Энэ зургийг дараа унших эрх авч чадсангүй. " +
                            "Зургаа дахин сонгоно уу."
            }
        }
    }

    // Хадгалж байх үед Android-ийн системийн Back үйлдлийг түр барина.
    // Screen-ийн Back товч мөн isSaving үед идэвхгүй байгаа.
    //
    // Хоосон блок нь зориуд: хадгалалт дуустал буцахгүй.
    BackHandler(enabled = uiState.isSaving) {
        // Хадгалалт дуусахыг хүлээнэ.
    }

    // Coroutine ажиллаж байх хооронд callback өөрчлөгдсөн ч
    // хамгийн сүүлийн callback-ийг ашиглана.
    val currentOnSaved by rememberUpdatedState(onSaved)

    // Амжилттай хадгалсны дараа navigation үйлдэл хийнэ.
    // Үүнийг Composable-ийн үндсэн хэсгээс шууд дуудахгүй.
    LaunchedEffect(uiState.savedDeckId) {

        val savedDeckId = uiState.savedDeckId

        if (savedDeckId != null) {
            currentOnSaved(savedDeckId)
        }
    }

    DeckFormScreen(
        uiState = uiState,
        imageStorage = container.imageStorage,

        onTitleChange = { newTitle ->
            formViewModel.onTitleChange(newTitle)
        },

        onDescriptionChange = { newDescription ->
            formViewModel.onDescriptionChange(newDescription)
        },

        onPickImageClick = {

            pickerErrorMessage = null

            try {
                // Зөвхөн зураг сонгоно.
                // Видео сонгох боломж өгөхгүй.
                photoPicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )

            } catch (exception: ActivityNotFoundException) {

                Log.e(
                    "DeckFormRoute",
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
            formViewModel.saveDeck()
        },

        onBackClick = onBackClick,

        onRetryLoadingClick = {
            formViewModel.retryLoading()
        },

        onDismissSaveError = {
            formViewModel.dismissSaveError()
        }
    )

    // Photo Picker-той холбоотой алдаа.
    // Room хадгалалтын алдааны dialog-ийг Screen өөрөө харуулдаг.
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