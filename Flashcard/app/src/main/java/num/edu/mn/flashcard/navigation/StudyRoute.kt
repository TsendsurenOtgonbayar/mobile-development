package num.edu.mn.flashcard.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import num.edu.mn.flashcard.FlashcardApplication
import num.edu.mn.flashcard.di.AppViewModelFactory
import num.edu.mn.flashcard.ui.screens.StudyScreen
import num.edu.mn.flashcard.ui.viewmodel.StudyMode
import num.edu.mn.flashcard.ui.viewmodel.StudyViewModel

@Composable
fun StudyRoute(
    backStackEntry: NavBackStackEntry,
    studyMode: StudyMode,
    onBackClick: () -> Unit
) {
    // Navigation route-ийн {deckId} утгыг авна.
    val deckId = backStackEntry.arguments
        ?.getString("deckId")

    // practice дамжуулаагүй бол энгийн давталт байна.
    val isPractice = backStackEntry.arguments
        ?.getBoolean("practice")
        ?: false

    // Буруу аргументтай бол ViewModel үүсгэхээс өмнө зогсооно.
    if (deckId.isNullOrBlank()) {
        StudyRouteError(
            message = "Суралцах багцын ID олдсонгүй.",
            onBackClick = onBackClick
        )
        return
    }

    // Activity-ийн Context-оос Application-ийг авна.
    // AppContainer нь Application дотор нэг удаа үүсдэг.
    val application = LocalContext.current.applicationContext
            as FlashcardApplication

    val container = application.container

    // remember: Ижил параметртэй үед Factory объектыг
    // recomposition бүрд дахин байгуулахгүй.
    //
    // Энэ remember нь ViewModel-ийг хадгалж байгаа биш.
    // ViewModel-ийн lifecycle-ийг ViewModelProvider удирдана.
    val factory = remember(
        container,
        deckId,
        studyMode,
        isPractice
    ) {
        AppViewModelFactory(
            container = container,
            deckId = deckId,
            studyMode = studyMode,
            isPractice = isPractice
        )
    }

    // backStackEntry-г owner болгосноор энэ navigation entry
    // өөрийн StudyViewModel instance-тэй болно.
    //
    // Дэлгэц дахин зурагдахад одоо байгаа instance-ийг авна.
    // Entry back stack-аас бүрэн хасагдахад ViewModel цэвэрлэгдэнэ.
    val studyViewModel: StudyViewModel = viewModel(
        viewModelStoreOwner = backStackEntry,
        factory = factory
    )

    // StateFlow-г Compose-ийн ажиглаж болох State болгож авна.
    // Lifecycle идэвхгүй үед collection түр зогсоно.
    //
    // Анхаарах нь: collection зогсох нь ViewModel-ийн
    // хадгалалтын coroutine-ийг өөрөө цуцлахгүй.
    val uiState by studyViewModel.uiState.collectAsStateWithLifecycle(
        lifecycleOwner = backStackEntry
    )

    // Үр дүн хадгалагдаж байх үед Android-ийн back товч болон
    // back gesture-ээр энэ дэлгэцээс гарахыг түр хаана.
    //
    // Хадгалалт дуусмагц enabled=false болж,
    // navigation-ийн ердийн back үйлдэл ажиллана.
    BackHandler(enabled = uiState.isSubmitting) {
        // Хадгалалт дуустал энэ back үйлдлийг боловсруулахгүй.
    }

    StudyScreen(
        uiState = uiState,
        imageStorage = container.imageStorage,

        onBackClick = {
            // Дэлгэцийн "Буцах" товчийн нэмэлт хамгаалалт.
            if (!studyViewModel.uiState.value.isSubmitting) {
                onBackClick()
            }
        },

        onRetryClick = {
            studyViewModel.retryLoading()
        },

        onFlipClick = {
            studyViewModel.flipCard()
        },

        onRememberClick = { isRemembered ->
            studyViewModel.rateFlashcard(
                isRemembered = isRemembered
            )
        },

        onAnswerChange = { answer ->
            studyViewModel.onAnswerChange(answer)
        },

        onCheckAnswerClick = {
            studyViewModel.checkAnswer()
        },

        onNextClick = {
            studyViewModel.nextCard()
        },

        onDismissError = {
            studyViewModel.dismissActionError()
        }
    )
}

@Composable
private fun StudyRouteError(
    message: String,
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
            verticalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.CenterVertically
            )
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Button(onClick = onBackClick) {
                Text("Буцах")
            }
        }
    }
}