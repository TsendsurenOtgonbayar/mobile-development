package num.edu.mn.flashcard.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import num.edu.mn.flashcard.data.models.Deck
import num.edu.mn.flashcard.data.models.DeckStatistics
import num.edu.mn.flashcard.data.storage.ImageStorage
import num.edu.mn.flashcard.ui.viewmodel.LibraryUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    imageStorage: ImageStorage,
    onDeckClick: (String) -> Unit,
    onAddDeckClick: () -> Unit,
    onRetryClick: () -> Unit,

    // Багцын ID → тухайн багцын статистик.
    // Статистикийн урсгалыг дараагийн алхамд ViewModel-той холбоно.
    // Хоосон map үед зохиомол тоо харуулахгүй.
    deckStatistics: Map<String, DeckStatistics> = emptyMap()
) {

    // Хайлтын бичвэр нь түр UI төлөв.
    // rememberSaveable: recomposition болон Activity дахин
    // үүсэх үед боломжтой нөхцөлд энэ жижиг утгыг сэргээнэ.
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    val backgroundColor = Color(0xFFE8FAFF)
    val textColor = Color(0xFF172033)

    // Query эсвэл багцууд өөрчлөгдөхөд жагсаалтыг дахин шүүнэ.
    // remember энд үр дүнг түр санах ойд хадгалж байна.
    // Database хадгалалт хийж байгаа биш.
    val filteredDecks = remember(uiState.decks, searchQuery) {

        val query = searchQuery.trim()

        if (query.isEmpty()) {
            uiState.decks
        } else {
            uiState.decks.filter { deck ->
                deck.title.contains(
                    other = query,
                    ignoreCase = true
                ) ||
                        deck.description.contains(
                            other = query,
                            ignoreCase = true
                        )
            }
        }
    }

    Scaffold(
        containerColor = backgroundColor,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Flashcard           Library",
                        fontSize = 37.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = textColor
                )
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddDeckClick,
                shape = CircleShape,
                containerColor = Color(0xFF276B91),
                contentColor = Color.White
            ) {
                Text(
                    text = "+",
                    fontSize = 28.sp
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()

                // Scaffold-ийн TopAppBar зэрэг хэсэгтэй
                // давхардахаас хамгаалсан зай.
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            OutlinedTextField(
                value = searchQuery,

                // Хэрэглэгч бичих бүрд UI төлөв шинэчлэгдэнэ.
                onValueChange = { newValue ->
                    searchQuery = newValue
                },

                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Багц хайх") },
                placeholder = { Text("Нэр эсвэл тайлбар") },
                shape = RoundedCornerShape(16.dp)
            )

            Text(
                text = "My Flashcard Sets",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Нэг л үндсэн төлөвийг харуулна.
            when {

                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF276B91)
                        )
                    }
                }

                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(onClick = onRetryClick) {
                            Text("Дахин оролдох")
                        }
                    }
                }

                uiState.decks.isEmpty() -> {
                    LibraryMessage(
                        message = "Одоогоор багц алга.\n+ товчоор багц нэмээрэй.",
                        modifier = Modifier.weight(1f)
                    )
                }

                filteredDecks.isEmpty() -> {
                    LibraryMessage(
                        message = "Хайлтад тохирох багц олдсонгүй.",
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),

                        verticalArrangement = Arrangement.spacedBy(12.dp),

                        // Сүүлийн Card-ийг FAB-ийн ард дарагдуулахгүй.
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {

                        items(
                            items = filteredDecks,

                            // Тогтвортой ID ашигласнаар Compose
                            // жагсаалтын item-уудыг зөв ялгана.
                            key = { deck -> deck.id }
                        ) { deck ->

                            LibraryDeckCard(
                                deck = deck,
                                statistics = deckStatistics[deck.id],
                                imageStorage = imageStorage,
                                onDeckClick = onDeckClick
                            )
                        }
                    }
                }
            }
        }
    }
}

// Зөвхөн энэ файлд хэрэгтэй жижиг Composable тул private.
@Composable
private fun LibraryMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color(0xFF526070)
        )
    }
}

@Composable
private fun LibraryDeckCard(
    deck: Deck,
    statistics: DeckStatistics?,
    imageStorage: ImageStorage,
    onDeckClick: (String) -> Unit
) {

    val lastStudiedText = deck.lastStudiedAt?.let { timestamp ->

        val formattedDate = SimpleDateFormat(
            "yyyy.MM.dd HH:mm",
            Locale.getDefault()
        ).format(Date(timestamp))

        "Сүүлд суралцсан: $formattedDate"

    } ?: "Одоогоор суралцаагүй"

    Card(
        onClick = {
            // Дэлгэц солихыг энэ Composable өөрөө хийхгүй.
            // Аль багц сонгосныг callback-аар дамжуулна.
            onDeckClick(deck.id)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Color(0xFF172033)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            LibraryDeckCover(
                fileName = deck.coverImageFileName,
                imageStorage = imageStorage,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = deck.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = lastStudiedText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF526070)
                )

                // Статистик ирсэн үед л бодит тоонуудыг харуулна.
                // Одоогоор ViewModel-той энэ хэсэг холбогдоогүй.
                if (statistics != null) {
                    Text(
                        text = "${statistics.cardCount} карт · " +
                                "${statistics.masteredCount} mastered",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF276B91)
                    )
                }
            }

            Text(
                text = "›",
                fontSize = 28.sp,
                color = Color(0xFF526070)
            )
        }
    }
}

@Composable
private fun LibraryDeckCover(
    fileName: String?,
    imageStorage: ImageStorage,
    modifier: Modifier = Modifier
) {

    // Файлын замыг илэрхийлэх объект авна.
    // Энэ нь зургийн байтуудыг UI thread дээр уншихгүй.
    val imageFile = remember(fileName, imageStorage) {

        if (fileName == null) {
            null
        } else {
            try {
                imageStorage.getImageFile(fileName)
            } catch (_: IllegalArgumentException) {
                // Буруу файлын нэртэй бол fallback дүрслэл ашиглана.
                null
            }
        }
    }

    if (imageFile == null) {

        DefaultDeckCover(modifier = modifier)

    } else {

        // Coil зургийг файлаас асинхроноор уншиж харуулна.
        // Өмнө төсөлд байсан coil-compose dependency-г ашиглаж байна.
        SubcomposeAsyncImage(
            model = imageFile,
            contentDescription = "Багцын нүүр зураг",
            modifier = modifier,
            contentScale = ContentScale.Crop,

            // Ачаалах зуур default дүрслэл.
            loading = {
                DefaultDeckCover(
                    modifier = Modifier.fillMaxSize()
                )
            },

            // Файл байхгүй эсвэл зураг уншигдахгүй бол fallback.
            error = {
                DefaultDeckCover(
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}

// Зураггүй багцад зориулсан жижиг картуудын дүрслэл.
// Canvas-аар зурдаг тул нэмэлт drawable файл шаардахгүй.
//
// Зөвхөн Deck-ийн default нүүр.
// Зураггүй FlashCard дээр үүнийг харуулахгүй.
@Composable
private fun DefaultDeckCover(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {

        drawRect(
            color = Color(0xFFDDF2FA)
        )

        // Арын карт.
        drawRoundRect(
            color = Color(0xFF8CC5DD),
            topLeft = Offset(
                x = size.width * 0.16f,
                y = size.height * 0.18f
            ),
            size = Size(
                width = size.width * 0.60f,
                height = size.height * 0.52f
            ),
            cornerRadius = CornerRadius(6.dp.toPx())
        )

        // Урд карт.
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(
                x = size.width * 0.25f,
                y = size.height * 0.30f
            ),
            size = Size(
                width = size.width * 0.60f,
                height = size.height * 0.52f
            ),
            cornerRadius = CornerRadius(6.dp.toPx())
        )

        // Карт дээрх бичвэрийг төлөөлөх зураас.
        drawLine(
            color = Color(0xFF276B91),
            start = Offset(
                x = size.width * 0.36f,
                y = size.height * 0.48f
            ),
            end = Offset(
                x = size.width * 0.72f,
                y = size.height * 0.48f
            ),
            strokeWidth = 3.dp.toPx()
        )

        drawLine(
            color = Color(0xFF8CC5DD),
            start = Offset(
                x = size.width * 0.36f,
                y = size.height * 0.63f
            ),
            end = Offset(
                x = size.width * 0.61f,
                y = size.height * 0.63f
            ),
            strokeWidth = 3.dp.toPx()
        )
    }
}