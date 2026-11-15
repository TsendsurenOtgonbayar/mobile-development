package num.edu.mn.flashcard.ui.screens

import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import num.edu.mn.flashcard.data.storage.ImageStorage
import num.edu.mn.flashcard.ui.viewmodel.DeckFormUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckFormScreen(
    uiState: DeckFormUiState,
    imageStorage: ImageStorage,

    // TextField өөрчлөгдөхөд шинэ бичвэрийг дамжуулна.
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,

    // Picker нээх ажиллагааг Route хариуцна.
    onPickImageClick: () -> Unit,
    onRemoveImageClick: () -> Unit,

    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    onRetryLoadingClick: () -> Unit,
    onDismissSaveError: () -> Unit
) {

    val backgroundColor = Color(0xFFE8FAFF)

    // Ачаалж эсвэл хадгалж байх үед талбаруудыг засуулахгүй.
    // Хадгалалт дууссаны дараа Route буцах хүртэл мөн идэвхгүй.
    val canEdit = !uiState.isLoading &&
            !uiState.isSaving &&
            uiState.loadErrorMessage == null &&
            uiState.savedDeckId == null

    // Хэрэглэгчийн зураг одоогийн form дээр байгаа эсэх.
    val hasCustomImage = !uiState.removeImage &&
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
                            "Edit Deck"
                        } else {
                            "Create Deck"
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

        // Keyboard гарсан үед доод талын агуулгыг
        // keyboard-ийн ард дарагдуулахгүй.
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

                        Text(
                            text = "Нүүр зураг",
                            style = MaterialTheme.typography.titleMedium
                        )

                        DeckFormImagePreview(
                            uiState = uiState,
                            imageStorage = imageStorage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )

                        Text(
                            text = if (hasCustomImage) {
                                "Зургийн өөрчлөлт Save дарсны дараа хадгалагдана."
                            } else {
                                "Зураг сонгоогүй бол default нүүр ашиглана."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF526070)
                        )

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
                                    text = if (hasCustomImage) {
                                        "Зураг солих"
                                    } else {
                                        "Зураг сонгох"
                                    }
                                )
                            }

                            if (hasCustomImage) {
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
                            value = uiState.title,
                            onValueChange = onTitleChange,
                            enabled = canEdit,
                            modifier = Modifier.fillMaxWidth(),

                            label = {
                                Text("Багцын нэр")
                            },

                            placeholder = {
                                Text("Жишээ: Англи үгс A2")
                            },

                            singleLine = true,

                            // Нэрийн шалгалт ViewModel дээр хийгдэнэ.
                            isError = uiState.titleError != null,

                            supportingText = {
                                val titleError = uiState.titleError

                                if (titleError != null) {
                                    Text(titleError)
                                }
                            },

                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = onDescriptionChange,
                            enabled = canEdit,
                            modifier = Modifier.fillMaxWidth(),

                            label = {
                                Text("Тайлбар — сонголттой")
                            },

                            placeholder = {
                                Text("Энэ багцад юу суралцах вэ?")
                            },

                            minLines = 3,
                            maxLines = 6,
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

    // Зураг унших эсвэл database-д хадгалах үед гарсан алдаа.
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

// Form дээр аль зургийг харуулахыг шийднэ.
@Composable
private fun DeckFormImagePreview(
    uiState: DeckFormUiState,
    imageStorage: ImageStorage,
    modifier: Modifier = Modifier
) {

    // Coil-ийн model нь Uri эсвэл File байж болно.
    // Тиймээс энд нийтлэг төрөл болох Any? ашиглаж байна.
    val imageModel: Any? = remember(
        uiState.removeImage,
        uiState.selectedImageUri,
        uiState.existingImageFileName,
        imageStorage
    ) {

        val selectedUri = uiState.selectedImageUri
        val existingFileName = uiState.existingImageFileName

        when {

            // Арилгахыг сонгосон бол хуучин зураг байсан ч
            // default нүүрийг харуулна.
            uiState.removeImage -> null

            // Шинээр сонгосон зураг хуучин зургаас давуу.
            selectedUri != null -> Uri.parse(selectedUri)

            // Шинэ зураг сонгоогүй бол хадгалсан зургийг харуулна.
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

    if (imageModel == null) {
        DefaultFormDeckCover(modifier)
    } else {
        SubcomposeAsyncImage(
            model = imageModel,
            contentDescription = "Багцын нүүр зургийн урьдчилсан харагдац",
            modifier = modifier,
            contentScale = ContentScale.Crop,

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
                Surface(
                    modifier = Modifier.fillMaxSize(),
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
        )
    }
}

// Бусад Deck дэлгэцтэй ижил утгатай default дүрслэл.
// Өөрөө database эсвэл файлд зураг үүсгэхгүй, UI дээр зурна.
@Composable
private fun DefaultFormDeckCover(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {

        drawRect(Color(0xFFDDF2FA))

        val unit = size.minDimension
        val cardWidth = unit * 0.66f
        val cardHeight = unit * 0.46f

        val left = (size.width - cardWidth) / 2f
        val top = (size.height - cardHeight) / 2f

        drawRoundRect(
            color = Color(0xFF8CC5DD),
            topLeft = Offset(
                left - unit * 0.07f,
                top - unit * 0.07f
            ),
            size = Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(10.dp.toPx())
        )

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(10.dp.toPx())
        )

        drawLine(
            color = Color(0xFF276B91),
            start = Offset(
                left + cardWidth * 0.18f,
                top + cardHeight * 0.38f
            ),
            end = Offset(
                left + cardWidth * 0.82f,
                top + cardHeight * 0.38f
            ),
            strokeWidth = 4.dp.toPx()
        )

        drawLine(
            color = Color(0xFF8CC5DD),
            start = Offset(
                left + cardWidth * 0.18f,
                top + cardHeight * 0.65f
            ),
            end = Offset(
                left + cardWidth * 0.62f,
                top + cardHeight * 0.65f
            ),
            strokeWidth = 4.dp.toPx()
        )
    }
}