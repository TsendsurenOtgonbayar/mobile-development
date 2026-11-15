package num.edu.mn.flashcard.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import num.edu.mn.flashcard.data.models.Deck
import num.edu.mn.flashcard.data.models.DeckStatistics
import num.edu.mn.flashcard.data.storage.ImageStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    deck: Deck,
    statistics: DeckStatistics,
    imageStorage: ImageStorage,
    isDeleting: Boolean,
    onBackClick: () -> Unit,

    // Boolean параметр:
    // false → хуваарьт суралцах.
    // true  → ахиц өөрчлөхгүй Practice.
    onFlashcardClick: (Boolean) -> Unit,
    onLearnClick: (Boolean) -> Unit,

    onManageCardsClick: () -> Unit,
    onEditDeckClick: () -> Unit,
    onDeleteDeckClick: () -> Unit
) {

    // Цэс нээлттэй эсэх нь түр UI төлөв.
    // Дэлгэцийг шинээр үүсгэхэд цэс хаалттай байж болно.
    var menuExpanded by remember(deck.id) {
        mutableStateOf(false)
    }

    // Устгах dialog болон Practice сонголтыг
    // Activity дахин үүсэх үед сэргээх боломжтой байлгана.
    var showDeleteDialog by rememberSaveable(deck.id) {
        mutableStateOf(false)
    }

    var isPractice by rememberSaveable(deck.id) {
        mutableStateOf(false)
    }

    val backgroundColor = Color(0xFFE8FAFF)
    val textColor = Color(0xFF172033)

    // Хуваарьт суралцахад шинэ болон хугацаа болсон карт орно.
    // dueCount нь шинэ картуудыг агуулаагүй тул нэмж болно.
    val scheduledCardCount =
        statistics.newCount + statistics.dueCount

    // Practice үед бүх картыг үзэх боломжтой.
    val availableCardCount = if (isPractice) {
        statistics.cardCount
    } else {
        scheduledCardCount
    }

    val canStartStudy =
        availableCardCount > 0 && !isDeleting

    Scaffold(
        containerColor = backgroundColor,

        topBar = {
            TopAppBar(
                title = {},

                navigationIcon = {
                    TextButton(
                        onClick = onBackClick,
                        enabled = !isDeleting
                    ) {
                        Text(
                            text = "← Back",
                            color = textColor
                        )
                    }
                },

                actions = {
                    Box {
                        IconButton(
                            enabled = !isDeleting,
                            onClick = {
                                menuExpanded = true
                            }
                        ) {
                            Text(
                                text = "⋮",
                                fontSize = 26.sp,
                                color = textColor
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = {
                                menuExpanded = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text("Manage Cards")
                                },
                                enabled = !isDeleting,
                                onClick = {
                                    menuExpanded = false
                                    onManageCardsClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Edit Deck")
                                },
                                enabled = !isDeleting,
                                onClick = {
                                    menuExpanded = false
                                    onEditDeckClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Delete Deck",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                enabled = !isDeleting,
                                onClick = {
                                    menuExpanded = false

                                    // Шууд устгахгүй.
                                    // Эхлээд баталгаажуулах dialog харуулна.
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            DetailDeckCover(
                fileName = deck.coverImageFileName,
                imageStorage = imageStorage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = deck.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                if (deck.description.isNotBlank()) {
                    Text(
                        text = deck.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF526070)
                    )
                }

                Text(
                    text = "${statistics.cardCount} Cards",
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }

            Text(
                text = "Your Statistics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailStatisticCard(
                    title = "New",
                    value = statistics.newCount.toString(),
                    modifier = Modifier.weight(1f)
                )

                DetailStatisticCard(
                    title = "Learning",
                    value = statistics.learningCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailStatisticCard(
                    title = "Mastered",
                    value = statistics.masteredCount.toString(),
                    modifier = Modifier.weight(1f)
                )

                DetailStatisticCard(
                    title = "Due now",
                    value = statistics.dueCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            // masteryPercent аль хэдийн 0–100 хүрээтэй.
            // Дахин 100-аар үржүүлэхгүй.
            DetailStatisticCard(
                title = "Mastery",
                value = "${statistics.masteryPercent.toInt()}%",
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Practice",
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Text(
                            text = if (isPractice) {
                                "Бүх картыг давтана. Хайрцаг болон хуваарь өөрчлөгдөхгүй."
                            } else {
                                "Шинэ болон давтах хугацаа болсон картуудыг суралцана."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF526070)
                        )
                    }

                    Switch(
                        checked = isPractice,
                        enabled = !isDeleting,
                        onCheckedChange = { checked ->
                            isPractice = checked
                        }
                    )
                }
            }

            Text(
                text = "Энэ ээлжид: $availableCardCount карт",
                color = textColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = {
                        onFlashcardClick(isPractice)
                    },
                    enabled = canStartStudy,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = 8.dp,
                        vertical = 16.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF002A4F)
                    )
                ) {
                    Text("Flashcards")
                }

                Button(
                    onClick = {
                        onLearnClick(isPractice)
                    },
                    enabled = canStartStudy,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = 8.dp,
                        vertical = 16.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF276B91)
                    )
                ) {
                    Text("Learn Mode")
                }
            }

            when {
                isDeleting -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )

                        Text("Багцыг устгаж байна…")
                    }
                }

                statistics.cardCount == 0 -> {
                    Text(
                        text = "Manage Cards хэсгээс эхний картаа нэмээрэй.",
                        color = Color(0xFF526070)
                    )
                }

                !isPractice && scheduledCardCount == 0 -> {
                    Text(
                        text = "Одоогоор давтах хугацаа болсон карт алга. " +
                                "Хүсвэл Practice асаагаад давтаж болно.",
                        color = Color(0xFF526070)
                    )
                }
            }
        }
    }

    // Dialog нь Scaffold-ийн дараа байгаа ч
    // дэлгэцийн дээр давхарга болон харагдана.
    if (showDeleteDialog && !isDeleting) {

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },

            title = {
                Text("Багцыг устгах уу?")
            },

            text = {
                Text(
                    text = "“${deck.title}” багц болон доторх бүх карт " +
                            "устна. Энэ үйлдлийг буцаах боломжгүй."
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false

                        // Устгах үйлдлийг Route/ViewModel руу дамжуулна.
                        onDeleteDeckClick()
                    }
                ) {
                    Text(
                        text = "Устгах",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Болих")
                }
            }
        )
    }
}

// Нэг статистикийн жижиг Card.
// private учраас бусад файлын ижил нэртэй функцтэй зөрчилдөхгүй.
@Composable
private fun DetailStatisticCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF172033)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF526070)
            )
        }
    }
}

@Composable
private fun DetailDeckCover(
    fileName: String?,
    imageStorage: ImageStorage,
    modifier: Modifier = Modifier
) {
    val imageFile = remember(fileName, imageStorage) {

        if (fileName == null) {
            null
        } else {
            try {
                imageStorage.getImageFile(fileName)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }

    if (imageFile == null) {
        DefaultDetailCover(modifier)
    } else {
        SubcomposeAsyncImage(
            model = imageFile,
            contentDescription = "Багцын нүүр зураг",
            modifier = modifier,
            contentScale = ContentScale.Crop,

            loading = {
                DefaultDetailCover(Modifier.fillMaxSize())
            },

            error = {
                DefaultDetailCover(Modifier.fillMaxSize())
            }
        )
    }
}

// Зураггүй багцын default нүүр.
// Байгаа Library default нүүртэй ижил өнгө, картын дүрслэл ашиглана.
@Composable
private fun DefaultDetailCover(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {

        drawRect(Color(0xFFDDF2FA))

        // minDimension: өргөн, өндрийн бага утга.
        // Өргөн banner дээр картын дүрсийг хэт сунгахгүй.
        val unit = size.minDimension

        val cardWidth = unit * 0.66f
        val cardHeight = unit * 0.46f

        val left = (size.width - cardWidth) / 2f
        val top = (size.height - cardHeight) / 2f

        drawRoundRect(
            color = Color(0xFF8CC5DD),
            topLeft = Offset(
                x = left - unit * 0.07f,
                y = top - unit * 0.07f
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