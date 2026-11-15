package num.edu.mn.flashcard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import num.edu.mn.flashcard.data.storage.ImageStorage
import num.edu.mn.flashcard.ui.viewmodel.StudyMode
import num.edu.mn.flashcard.ui.viewmodel.StudyUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    uiState: StudyUiState,
    imageStorage: ImageStorage,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit,
    onFlipClick: () -> Unit,
    onRememberClick: (Boolean) -> Unit,
    onAnswerChange: (String) -> Unit,
    onCheckAnswerClick: () -> Unit,
    onNextClick: () -> Unit,
    onDismissError: () -> Unit
) {
    val backgroundColor = Color(0xFFE8FAFF)
    val primaryColor = Color(0xFF163B65)

    val screenTitle = when (uiState.studyMode) {
        StudyMode.FLASHCARD -> "Flashcard"
        StudyMode.LEARN -> "Learn"
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isPractice) {
                            "$screenTitle · Practice"
                        } else {
                            screenTitle
                        }
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBackClick,

                        // Хадгалалт дуусахаас өмнө буцах товчийг идэвхгүй болгоно.
                        // System Back-ийг дараагийн Route файлд зохицуулна.
                        enabled = !uiState.isSubmitting
                    ) {
                        Text("Буцах")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { innerPadding ->

        // innerPadding: TopAppBar зэрэг Scaffold-ийн хэсгүүдэд
        // контент давхардахаас хамгаалах зай.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(20.dp)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = primaryColor
                    )
                }

                uiState.loadErrorMessage != null -> {
                    StudyMessage(
                        message = uiState.loadErrorMessage,
                        buttonText = "Дахин оролдох",
                        onButtonClick = onRetryClick
                    )
                }

                uiState.totalCardCount == 0 -> {
                    StudyMessage(
                        message = if (uiState.isPractice) {
                            "Энэ багцад карт байхгүй байна."
                        } else {
                            "Одоогоор давтах карт байхгүй байна."
                        },
                        buttonText = "Багц руу буцах",
                        onButtonClick = onBackClick
                    )
                }

                uiState.isFinished -> {
                    StudyResult(
                        uiState = uiState,
                        onBackClick = onBackClick
                    )
                }

                else -> {
                    // Одоогийн индексээр картыг авна.
                    // Хэрэв төлөв зөрсөн бол хоосон дэлгэц харуулахгүй.
                    val card = uiState.currentCard

                    if (card == null) {
                        StudyMessage(
                            message = "Одоогийн карт олдсонгүй.",
                            buttonText = "Багц руу буцах",
                            onButtonClick = onBackClick
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Карт ${uiState.currentCardIndex + 1}" +
                                        " / ${uiState.totalCardCount}",
                                style = MaterialTheme.typography.titleMedium,
                                color = primaryColor
                            )

                            Text(
                                text = "Хариулсан: ${uiState.answeredCount}" +
                                        " · Зөв: ${uiState.correctCount}" +
                                        " · Буруу: ${uiState.wrongCount}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (uiState.isPractice) {
                                Text(
                                    text = "Practice: хайрцаг болон " +
                                            "давтах хугацаа өөрчлөгдөхгүй.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // Flashcard-ийн ар талд зөвхөн хариулт гарна.
                            // Learn үед асуулт үргэлж харагдана.
                            val showQuestion =
                                uiState.studyMode == StudyMode.LEARN ||
                                        !uiState.isAnswerVisible

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    Text(
                                        text = if (showQuestion) {
                                            "Асуулт"
                                        } else {
                                            "Хариулт"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = primaryColor
                                    )

                                    if (showQuestion) {
                                        // Зураг байхгүй бол зай эзлэхгүй.
                                        card.imageFileName?.let { fileName ->
                                            StudyQuestionImage(
                                                fileName = fileName,
                                                imageStorage = imageStorage
                                            )
                                        }

                                        Text(
                                            text = card.question,
                                            style = MaterialTheme.typography.headlineSmall,
                                            textAlign = TextAlign.Center
                                        )
                                    } else {
                                        Text(
                                            text = card.answer,
                                            style = MaterialTheme.typography.headlineSmall,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            when (uiState.studyMode) {
                                StudyMode.FLASHCARD -> {
                                    if (!uiState.isAnswerVisible) {
                                        Button(
                                            onClick = onFlipClick,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = primaryColor
                                            )
                                        ) {
                                            Text("Хариулт харах")
                                        }
                                    } else if (uiState.answerResult == null) {
                                        // Хариултыг харсны дараа үнэлгээний
                                        // хоёр товч гарч ирнэ.
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement =
                                                Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    onRememberClick(false)
                                                },
                                                enabled = !uiState.isSubmitting,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Санахгүй байна")
                                            }

                                            Button(
                                                onClick = {
                                                    onRememberClick(true)
                                                },
                                                enabled = !uiState.isSubmitting,
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = primaryColor
                                                )
                                            ) {
                                                Text("Санаж байна")
                                            }
                                        }
                                    }
                                }

                                StudyMode.LEARN -> {
                                    val canEditAnswer =
                                        !uiState.isSubmitting &&
                                                uiState.answerResult == null

                                    OutlinedTextField(
                                        value = uiState.typedAnswer,

                                        // Текстийг энд remember-д давхар хадгалахгүй.
                                        // ViewModel-ийн typedAnswer нь нэг эх үүсвэр болно.
                                        onValueChange = onAnswerChange,
                                        modifier = Modifier.fillMaxWidth(),
                                        label = {
                                            Text("Таны хариулт")
                                        },
                                        enabled = canEditAnswer,
                                        singleLine = true,
                                        isError = uiState.answerInputError != null,
                                        supportingText = {
                                            uiState.answerInputError?.let { message ->
                                                Text(message)
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                if (canEditAnswer) {
                                                    onCheckAnswerClick()
                                                }
                                            }
                                        )
                                    )

                                    if (uiState.answerResult == null) {
                                        Button(
                                            onClick = onCheckAnswerClick,
                                            enabled = !uiState.isSubmitting,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = primaryColor
                                            )
                                        ) {
                                            Text("Шалгах")
                                        }
                                    }

                                    // ViewModel шалгаж эхлэхдээ true болгодог.
                                    // Буруу хариулсан ч зөв хариултыг шууд харуулна.
                                    if (uiState.isAnswerVisible) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color.White
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                verticalArrangement =
                                                    Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "Зөв хариулт",
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Text(text = card.answer)
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.isSubmitting) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = primaryColor
                                    )

                                    Text("Үр дүнг боловсруулж байна…")
                                }
                            }

                            // answerResult зөвхөн үнэлгээ дуусахад утгатай болно.
                            // Энгийн давталтад энэ нь Room хадгалалт амжилттай
                            // болсон гэсэн үг.
                            uiState.answerResult?.let { isCorrect ->
                                val feedbackText = when (uiState.studyMode) {
                                    StudyMode.LEARN -> {
                                        if (isCorrect) {
                                            "Зөв хариуллаа."
                                        } else {
                                            "Буруу хариуллаа. Зөв хариултыг уншаарай."
                                        }
                                    }

                                    StudyMode.FLASHCARD -> {
                                        if (isCorrect) {
                                            "“Санаж байна” гэж тэмдэглэлээ."
                                        } else {
                                            "“Санахгүй байна” гэж тэмдэглэлээ."
                                        }
                                    }
                                }

                                Text(
                                    text = feedbackText,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Button(
                                    onClick = onNextClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = primaryColor
                                    )
                                ) {
                                    Text(
                                        text = if (
                                            uiState.currentCardIndex ==
                                            uiState.totalCardCount - 1
                                        ) {
                                            "Үр дүн харах"
                                        } else {
                                            "Дараагийн карт"
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Хадгалалтын алдаа гарвал тухайн карт дээрээ үлдэнэ.
    // Dialog хаасны дараа хариултаа дахин илгээж болно.
    uiState.actionErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = {
                Text("Үр дүнг хадгалж чадсангүй")
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text("Ойлголоо")
                }
            }
        )
    }
}

@Composable
private fun StudyQuestionImage(
    fileName: String,
    imageStorage: ImageStorage
) {
    // remember: Ижил filename-тэй үед File объектыг
    // recomposition бүрд дахин байгуулахгүй.
    //
    // Энэ нь зургийг байнгын хадгалалттай болгож байгаа хэрэг биш.
    // Зураг аль хэдийн app-ийн дотоод файлд хадгалагдсан.
    val imageFile = remember(fileName, imageStorage) {
        try {
            imageStorage.getImageFile(fileName)
        } catch (exception: IllegalArgumentException) {
            null
        }
    }

    if (imageFile == null) {
        Text("Зургийг харуулах боломжгүй.")
    } else {
        // Coil файлыг ачаалж, дэлгэцэд харуулна.
        SubcomposeAsyncImage(
            model = imageFile,
            contentDescription = "Асуултын зураг",
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),

            // Fit: Зургийг тайрахгүй, бүтнээр нь багтаана.
            // Томьё болон диаграммтай картанд тохиромжтой.
            contentScale = ContentScale.Fit,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Зургийг ачаалж чадсангүй.")
                }
            }
        )
    }
}

@Composable
private fun StudyMessage(
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onButtonClick) {
            Text(buttonText)
        }
    }
}

@Composable
private fun StudyResult(
    uiState: StudyUiState,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 20.dp,
            alignment = Alignment.CenterVertically
        )
    ) {
        Text(
            text = "Дасгал дууслаа",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text("Хариулсан карт: ${uiState.answeredCount}")

        Text("Зөв / санаж байна: ${uiState.correctCount}")

        Text("Буруу / санахгүй байна: ${uiState.wrongCount}")

        Text(
            text = if (uiState.isPractice) {
                "Practice тул картуудын ахиц өөрчлөгдөөгүй."
            } else {
                "Картуудын ахиц хадгалагдсан."
            },
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF163B65)
            )
        ) {
            Text("Багц руу буцах")
        }
    }
}