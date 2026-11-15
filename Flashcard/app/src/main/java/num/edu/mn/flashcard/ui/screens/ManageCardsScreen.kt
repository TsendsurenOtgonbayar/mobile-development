package num.edu.mn.flashcard.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import num.edu.mn.flashcard.data.models.Deck
import num.edu.mn.flashcard.data.models.FlashCard
import num.edu.mn.flashcard.data.storage.ImageStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCardsScreen(
    deck: Deck,
    cards: List<FlashCard>,
    imageStorage: ImageStorage,

    // null → устгаж байгаа карт байхгүй.
    // Утгатай → тухайн ID-тай картыг устгаж байна.
    deletingCardId: String?,

    onBackClick: () -> Unit,
    onAddCardClick: () -> Unit,
    onEditCardClick: (String) -> Unit,
    onDeleteCardClick: (String) -> Unit
) {

    // Хэрэглэгч аль картыг устгах гэж байгааг хадгална.
    // Энэ бол зөвхөн dialog-ийн сонголт.
    // Database-аас хараахан устгаагүй байна.
    var pendingDeleteCardId by rememberSaveable(deck.id) {
        mutableStateOf<String?>(null)
    }

    val isDeletingCard = deletingCardId != null

    // ID-аар нь одоогийн жагсаалтаас картыг олно.
    // Карт өөр газраас устсан бол null болж dialog харагдахгүй.
    val cardToDelete = cards.firstOrNull { card ->
        card.id == pendingDeleteCardId
    }

    Scaffold(
        containerColor = Color(0xFFE8FAFF),

        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Manage Cards",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = deck.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF526070),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },

                navigationIcon = {
                    TextButton(
                        onClick = onBackClick,
                        enabled = !isDeletingCard
                    ) {
                        Text("← Back")
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE8FAFF),
                    titleContentColor = Color(0xFF172033)
                )
            )
        },

        floatingActionButton = {
            // Устгах үйлдэл үргэлжилж байхад
            // шинэ form нээхээс сэргийлж FAB-ийг түр нуух.
            if (!isDeletingCard) {
                FloatingActionButton(
                    onClick = onAddCardClick,
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
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            Text(
                text = "${cards.size} карт",
                modifier = Modifier.padding(vertical = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF172033)
            )

            // Устгасан карт Room-ийн жагсаалтаас арилсны дараа ч
            // зургийн файл цэвэрлэх ажил үргэлжилж байж болно.
            // Тиймээс progress-ийг item-аас гадна харуулна.
            if (isDeletingCard) {
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )

                    Text("Картыг устгаж байна…")
                }
            }

            if (cards.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Энэ багцад карт алга.\n" +
                                "+ товчоор эхний картаа нэмээрэй.",
                        color = Color(0xFF526070)
                    )
                }

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {

                    items(
                        items = cards,

                        // Index биш, картын тогтвортой ID ашиглана.
                        key = { card -> card.id }
                    ) { card ->

                        ManagedCardItem(
                            card = card,
                            imageStorage = imageStorage,

                            // Нэг устгал дуустал бусад үйлдлийг түр хаана.
                            actionsEnabled = !isDeletingCard,

                            onEditClick = {
                                onEditCardClick(card.id)
                            },

                            onDeleteClick = {
                                // Эхлээд dialog харуулна.
                                pendingDeleteCardId = card.id
                            }
                        )
                    }
                }
            }
        }
    }

    // ───── УСТГАХ БАТАЛГААЖУУЛАЛТ ─────

    if (cardToDelete != null && !isDeletingCard) {
        AlertDialog(
            onDismissRequest = {
                pendingDeleteCardId = null
            },

            title = {
                Text("Картыг устгах уу?")
            },

            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = cardToDelete.question,
                        fontWeight = FontWeight.Medium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Карт болон түүний суралцах ахиц устна. " +
                                "Энэ үйлдлийг буцаах боломжгүй."
                    )
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        val cardId = cardToDelete.id

                        // Dialog-ийг эхлээд хаана.
                        pendingDeleteCardId = null

                        // Бодит устгалыг Route/ViewModel-д дамжуулна.
                        onDeleteCardClick(cardId)
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
                        pendingDeleteCardId = null
                    }
                ) {
                    Text("Болих")
                }
            }
        )
    }
}

// Жагсаалтын нэг карт.
@Composable
private fun ManagedCardItem(
    card: FlashCard,
    imageStorage: ImageStorage,
    actionsEnabled: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {

    val progressText = when (card.boxLevel) {
        0 -> "New"
        in 1..3 -> "Learning · Box ${card.boxLevel}"
        in 4..5 -> "Mastered · Box ${card.boxLevel}"
        else -> "Төлөв тодорхойгүй"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Зурагтай карт дээр л thumbnail харуулна.
                val imageFileName = card.imageFileName

                if (imageFileName != null) {
                    ManagedCardImage(
                        fileName = imageFileName,
                        imageStorage = imageStorage,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = card.question,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF172033),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Энэ бол удирдах дэлгэц тул
                    // хариултыг нуух шаардлагагүй.
                    Text(
                        text = card.answer,
                        color = Color(0xFF526070),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF276B91)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedButton(
                    onClick = onEditClick,
                    enabled = actionsEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Засах")
                }

                TextButton(
                    onClick = onDeleteClick,
                    enabled = actionsEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Устгах",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ManagedCardImage(
    fileName: String,
    imageStorage: ImageStorage,
    modifier: Modifier = Modifier
) {

    val imageFile = remember(fileName, imageStorage) {
        try {
            imageStorage.getImageFile(fileName)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    // model null эсвэл файл уншигдахгүй бол error UI харагдана.
    SubcomposeAsyncImage(
        model = imageFile,
        contentDescription = "Картын зураг",
        modifier = modifier,

        // Томьёо, диаграммын захыг тайрахгүй.
        contentScale = ContentScale.Fit,

        loading = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        },

        error = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFDDF2FA)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Зураг\nолдсонгүй",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    )
}