package num.edu.mn.flashcard.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import num.edu.mn.flashcard.data.storage.ImageStorage
import num.edu.mn.flashcard.ui.viewmodel.CardFormUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardFormScreen(
    uiState: CardFormUiState,
    imageStorage: ImageStorage,

    onQuestionChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,

    onPickImageClick: () -> Unit,
    onRemoveImageClick: () -> Unit,

    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    onRetryLoadingClick: () -> Unit,
    onDismissSaveError: () -> Unit,

    // true → New төлөвт буцаах.
    // false → ахицыг хэвээр хадгалах.
    onProgressChoice: (Boolean) -> Unit,

    // Хадгалахгүйгээр dialog-ийг хааж form-д үлдэх.
    onCancelProgressChoice: () -> Unit
) {

    val backgroundColor = Color(0xFFE8FAFF)

    val canEdit = !uiState.isLoading &&
            !uiState.isSaving &&
            !uiState.showProgressDialog &&
            uiState.loadErrorMessage == null &&
            uiState.savedCardId == null

    val hasImage = !uiState.removeImage &&
            (
                    uiState.selectedImageUri != null ||
                            uiState.existingImageFileName != null
                    )

    Scaffold(
        containerColor = backgroundColor,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) {
                            "Edit Card"
                        } else {
                            "Create Card"
                        }
                    )
                },

                navigationIcon = {
                    TextButton(
                        onClick = onBackClick,
                        enabled = !uiState.isSaving
                    ) {
                        Text("← Back")
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = Color(0xFF172033)
                )
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {

            when {

                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF276B91)
                    )
                }

                uiState.loadErrorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.loadErrorMessage,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(onClick = onRetryLoadingClick) {
                            Text("Дахин оролдох")
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        OutlinedTextField(
                            value = uiState.question,
                            onValueChange = onQuestionChange,
                            enabled = canEdit,
                            modifier = Modifier.fillMaxWidth(),

                            label = {
                                Text("Асуулт эсвэл томьёо")
                            },

                            placeholder = {
                                Text("Жишээ: Алим гэдгийг англиар?")
                            },

                            minLines = 2,
                            maxLines = 5,

                            isError = uiState.questionError != null,

                            supportingText = {
                                val error = uiState.questionError

                                if (error != null) {
                                    Text(error)
                                }
                            },

                            shape = RoundedCornerShape(12.dp)
                        )

                        Text(
                            text = "Зураг — сонголттой",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Зураг сонгосон үед л preview харуулна.
                        // Deck-ээс ялгаатай нь зураггүй Card-д
                        // default зураг оруулахгүй.
                        if (hasImage) {
                            CardFormImagePreview(
                                uiState = uiState,
                                imageStorage = imageStorage,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            Text(
                                text = "Зураггүй бол суралцах үед " +
                                        "зөвхөн асуулт харагдана.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF526070)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPickImageClick,
                                enabled = canEdit,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (hasImage) {
                                        "Зураг солих"
                                    } else {
                                        "Зураг сонгох"
                                    }
                                )
                            }

                            if (hasImage) {
                                OutlinedButton(
                                    onClick = onRemoveImageClick,
                                    enabled = canEdit,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Зураг арилгах")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.answer,
                            onValueChange = onAnswerChange,
                            enabled = canEdit,
                            modifier = Modifier.fillMaxWidth(),

                            label = {
                                Text("Богино хариулт")
                            },

                            placeholder = {
                                Text("Жишээ: Apple")
                            },

                            singleLine = true,

                            isError = uiState.answerError != null,

                            supportingText = {
                                val error = uiState.answerError

                                if (error != null) {
                                    Text(error)
                                } else {
                                    Text(
                                        "Learn горимд энэ хариултыг бичиж шалгуулна."
                                    )
                                }
                            },

                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = onSaveClick,
                            enabled = canEdit,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF002A4F)
                            )
                        ) {

                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )

                                Spacer(Modifier.width(10.dp))

                                Text("Хадгалж байна…")

                            } else {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }

    // ───── АХИЦЫН СОНГОЛТ ─────

    if (uiState.showProgressDialog && !uiState.isSaving) {
        AlertDialog(
            onDismissRequest = onCancelProgressChoice,

            title = {
                Text("Суралцах ахицыг шинэчлэх үү?")
            },

            text = {
                Text(
                    "Картын асуулт эсвэл хариулт өөрчлөгдсөн байна.\n\n" +
                            "Шинээр суралцахыг сонговол карт New төлөвт орно.\n\n" +
                            "Ахицыг хадгалбал одоогийн хайрцаг, " +
                            "давтах хугацаа хэвээр үлдэнэ."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        onProgressChoice(true)
                    }
                ) {
                    Text("Шинээр суралцах")
                }
            },

            dismissButton = {
                // Гурван сонголтыг жижиг дэлгэцэд
                // багтаахын тулд хоёр товчийг босоогоор байрлуулна.
                Column(
                    horizontalAlignment = Alignment.End
                ) {

                    TextButton(
                        onClick = {
                            onProgressChoice(false)
                        }
                    ) {
                        Text("Ахицыг хадгалах")
                    }

                    TextButton(
                        onClick = onCancelProgressChoice
                    ) {
                        Text("Болих")
                    }
                }
            }
        )
    }

    // ───── ХАДГАЛАЛТЫН АЛДАА ─────

    val saveError = uiState.saveErrorMessage

    if (saveError != null) {
        AlertDialog(
            onDismissRequest = onDismissSaveError,

            title = {
                Text("Хадгалж чадсангүй")
            },

            text = {
                Text(saveError)
            },

            confirmButton = {
                TextButton(onClick = onDismissSaveError) {
                    Text("Ойлголоо")
                }
            }
        )
    }
}

@Composable
private fun CardFormImagePreview(
    uiState: CardFormUiState,
    imageStorage: ImageStorage,
    modifier: Modifier = Modifier
) {

    // Сонгосон шинэ Uri эсвэл өмнө хадгалсан File-ийг
    // Coil-ийн model болгон өгнө.
    val imageModel: Any? = remember(
        uiState.selectedImageUri,
        uiState.existingImageFileName,
        uiState.removeImage,
        imageStorage
    ) {

        val selectedUri = uiState.selectedImageUri
        val existingFileName = uiState.existingImageFileName

        when {
            uiState.removeImage -> null

            selectedUri != null -> Uri.parse(selectedUri)

            existingFileName != null -> {
                try {
                    imageStorage.getImageFile(existingFileName)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }

            else -> null
        }
    }

    // Зураг байгаа гэж form-д тэмдэглэгдсэн боловч
    // файлын нэр буруу эсвэл файл уншигдахгүй байж болно.
    if (imageModel == null) {
        CardImageError(modifier)
    } else {
        SubcomposeAsyncImage(
            model = imageModel,
            contentDescription = "Картын зургийн урьдчилсан харагдац",
            modifier = modifier,

            // Томьёо, диаграммын захыг тайрахгүй.
            contentScale = ContentScale.Fit,

            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                }
            },

            error = {
                CardImageError(
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}

@Composable
private fun CardImageError(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFDDF2FA)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Зургийг харуулж чадсангүй.\n" +
                        "Дахин сонгох эсвэл арилгана уу.",
                color = Color(0xFF526070)
            )
        }
    }
}