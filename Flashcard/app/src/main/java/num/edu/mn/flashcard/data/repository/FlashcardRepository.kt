package num.edu.mn.flashcard.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import num.edu.mn.flashcard.data.local.FlashcardDatabase
import num.edu.mn.flashcard.data.models.Deck
import num.edu.mn.flashcard.data.models.DeckStatistics
import num.edu.mn.flashcard.data.models.FlashCard
import num.edu.mn.flashcard.data.study.LeitnerScheduler
import android.net.Uri
import android.util.Log
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import num.edu.mn.flashcard.data.storage.ImageStorage
// Repository нь database үйлдлүүд болон суралцах дүрмийг холбоно.
//
// Database, Scheduler-ийг өөрөө шинээр үүсгэхгүй.
// Constructor-оор гаднаас хүлээн авна.
// Үүнийг constructor dependency injection гэж нэрлэдэг.
class FlashcardRepository(
    private val database: FlashcardDatabase,
    private val leitnerScheduler: LeitnerScheduler,
    private val imageStorage: ImageStorage
) {

    // Өмнө бичсэн DAO-уудаа database-аас авна.
    private val deckDao = database.deckDao()
    private val flashcardDao = database.flashcardDao()

    // ───── ӨГӨГДӨЛ УНШИХ ─────

    // Library дэлгэцэд багцуудын жагсаалт өгнө.
    // Database өөрчлөгдвөл Flow шинэ жагсаалт дамжуулна.
    fun observeDecks(): Flow<List<Deck>> {
        return deckDao.observeDecks()
    }

    // Deck Detail дэлгэцэд нэг багцыг ажиглана.
    // Багц устсан эсвэл олдохгүй бол null ирнэ.
    fun observeDeckById(deckId: String): Flow<Deck?> {
        return deckDao.observeDeckById(deckId)
    }

    // Manage Cards дэлгэцэд тухайн багцын картуудыг ажиглана.
    fun observeCardsByDeckId(
        deckId: String
    ): Flow<List<FlashCard>> {
        return flashcardDao.observeCardsByDeckId(deckId)
    }

    // Form-ийн анхны утгуудыг авах зэрэгт
    // багцыг нэг удаа уншиж болно.
    suspend fun getDeckById(deckId: String): Deck? {
        return deckDao.getDeckById(deckId)
    }

    // Засах гэж байгаа картыг нэг удаа уншина.
    suspend fun getCardById(cardId: String): FlashCard? {
        return flashcardDao.getCardById(cardId)
    }

    // Статистикийг ажиглахын өмнө 7 хоногийн дүрмийг хэрэгжүүлнэ.
    //
    // flow { ... }: урсгалыг үүсгэнэ.
    // Доторх код нь урсгалыг collect буюу ажиглаж эхлэхэд ажиллана.
    fun observeDeckStatistics(
        deckId: String,
        currentTime: Long
    ): Flow<DeckStatistics> = flow {

        // Хуучирсан Mastered төлөв байвал эхлээд засаж хадгална.
        refreshExpiredProgress(
            deckId = deckId,
            currentTime = currentTime
        )

        // emitAll: DAO-ийн Flow-оос ирсэн утгуудыг
        // энэ Flow-оор цааш дамжуулна.
        emitAll(
            flashcardDao.observeDeckStatistics(
                deckId = deckId,
                currentTime = currentTime
            )
        )
    }

    // ───── ХУГАЦАА ХЭТЭРСЭН АХИЦ ШИНЭЧЛЭХ ─────

    // Гаднаас дуудаж болох функц.
    // Жишээ нь апп буцаад идэвхжих үед ашиглаж болно.
    suspend fun refreshExpiredProgress(
        deckId: String,
        currentTime: Long
    ) {
        database.withTransaction {
            refreshExpiredProgressInTransaction(
                deckId = deckId,
                currentTime = currentTime
            )
        }
    }

    // Энэ private функцийг зөвхөн transaction дотроос дуудна.
    // Өөрөө дахин transaction эхлүүлэхгүй.
    private suspend fun refreshExpiredProgressInTransaction(
        deckId: String,
        currentTime: Long
    ) {
        val cards = flashcardDao.getCardsByDeckId(deckId)

        for (card in cards) {

            // Scheduler шинэ төлөв тооцно.
            // Өөрчлөх шаардлагагүй бол анхны картыг буцаана.
            val updatedCard = leitnerScheduler.resetExpiredProgress(
                card = card,
                currentTime = currentTime
            )

            // data class-ийн != нь талбаруудын утгыг харьцуулна.
            // Өөрчлөгдөөгүй картыг database-д дахин бичихгүй.
            if (updatedCard != card) {

                val updatedRows = flashcardDao.updateStudyProgress(
                    cardId = updatedCard.id,
                    boxLevel = updatedCard.boxLevel,
                    lastReviewedAt = updatedCard.lastReviewedAt,
                    nextReviewAt = updatedCard.nextReviewAt
                )

                // check: програмын хүлээсэн нөхцөл биелсэн эсэх.
                // Нэг карт шинэчлэгдэх ёстой.
                check(updatedRows == 1) {
                    "Картын ахицыг шинэчилж чадсангүй."
                }
            }
        }
    }

    // ───── СУРАЛЦАХ ЭЭЛЖ БЭЛТГЭХ ─────

    // Шинэ болон хугацаа болсон картуудыг авна.
    // Flashcards, Learn-ийн хуваарьт ээлжид ашиглана.
    suspend fun getStudyCards(
        deckId: String,
        currentTime: Long
    ): List<FlashCard> {
        return database.withTransaction {

            requireNotNull(deckDao.getDeckById(deckId)) {
                "Суралцах багц олдсонгүй."
            }

            // Эхлээд 7 хоногийн дүрмээр шаардлагатай картыг буулгана.
            refreshExpiredProgressInTransaction(
                deckId = deckId,
                currentTime = currentTime
            )

            // Дараа нь шинэчилсэн төлөвөөс суралцах картуудаа авна.
            flashcardDao.getStudyCards(
                deckId = deckId,
                currentTime = currentTime
            )
        }
    }

    // ───── НЭГ КАРТЫН ҮНЭЛГЭЭ ХАДГАЛАХ ─────

    // Flashcards болон Learn хоёулаа энэ функцийг ашиглана.
    //
    // isCorrect:
    // true  = зөв хариулсан / санаж байна.
    // false = буруу хариулсан / санахгүй байна.
    //
    // Practice горимд энэ функцийг дуудахгүй.
    suspend fun submitReview(
        cardId: String,
        isCorrect: Boolean,
        currentTime: Long
    ): FlashCard {
        return database.withTransaction {

            // UI-д байгаа хуучин хуулбарыг шууд хадгалахгүй.
            // Database дахь одоогийн картыг дахин уншина.
            val currentCard = requireNotNull(
                flashcardDao.getCardById(cardId)
            ) {
                "Үнэлэх карт олдсонгүй."
            }

            // 1. Дүрмийн дагуу шинэ хайрцаг, хугацааг тооцно.
            val updatedCard = leitnerScheduler.reviewCard(
                card = currentCard,
                isCorrect = isCorrect,
                currentTime = currentTime
            )

            // 2. Картын шинэ ахицыг Room-д хадгална.
            val updatedCardRows = flashcardDao.updateStudyProgress(
                cardId = updatedCard.id,
                boxLevel = updatedCard.boxLevel,
                lastReviewedAt = updatedCard.lastReviewedAt,
                nextReviewAt = updatedCard.nextReviewAt
            )

            check(updatedCardRows == 1) {
                "Картын үнэлгээг хадгалж чадсангүй."
            }

            // 3. Багцын сүүлд суралцсан хугацааг шинэчилнэ.
            val updatedDeckRows = deckDao.updateLastStudiedAt(
                deckId = updatedCard.deckId,
                studiedAt = currentTime
            )

            check(updatedDeckRows == 1) {
                "Багцын суралцсан хугацааг шинэчилж чадсангүй."
            }

            // Transaction амжилттай бол шинэ картыг буцаана.
            updatedCard
        }
    }
    // ───── БАГЦ ҮҮСГЭХ / ЗАСАХ ─────

    // deckId = null бол шинэ багц үүсгэнэ.
    // deckId утгатай бол байгаа багцыг засна.
    //
    // selectedImageUri = null, removeImage = false:
    // байгаа зургийг хэвээр үлдээнэ.
    //
    // selectedImageUri утгатай:
    // шинэ зураг хадгалж солино.
    //
    // removeImage = true:
    // хэрэглэгчийн зургийг арилгана.
    // Deck-ийн UI дараа нь default зураг харуулна.
    suspend fun saveDeck(
        title: String,
        description: String,
        deckId: String? = null,
        selectedImageUri: Uri? = null,
        removeImage: Boolean = false
    ): String {

        // trim(): эхлэл, төгсгөлийн хоосон зайг арилгана.
        val cleanedTitle = title.trim()
        val cleanedDescription = description.trim()

        require(cleanedTitle.isNotEmpty()) {
            "Багцын нэрийг оруулна уу."
        }

        require(!(selectedImageUri != null && removeImage)) {
            "Зураг сонгох болон арилгахыг зэрэг хийх боломжгүй."
        }

        return saveWithImage(selectedImageUri) { newImageFileName ->

            // Энэ блок saveWithImage()-ийн transaction дотор ажиллана.
            val existingDeck = if (deckId == null) {
                null
            } else {
                requireNotNull(deckDao.getDeckById(deckId)) {
                    "Засах багц олдсонгүй."
                }
            }

            val finalImageFileName = when {
                removeImage -> null
                newImageFileName != null -> newImageFileName
                else -> existingDeck?.coverImageFileName
            }

            val savedDeck = if (existingDeck == null) {

                // Шинэ багцад шинэ ID үүсгэнэ.
                Deck(
                    id = UUID.randomUUID().toString(),
                    title = cleanedTitle,
                    description = cleanedDescription,
                    coverImageFileName = finalImageFileName
                )
            } else {

                // copy() нь бусад талбарыг хэвээр хадгална.
                // Тиймээс lastStudiedAt өөрчлөгдөхгүй.
                existingDeck.copy(
                    title = cleanedTitle,
                    description = cleanedDescription,
                    coverImageFileName = finalImageFileName
                )
            }

            if (existingDeck == null) {
                deckDao.insertDeck(savedDeck)
            } else {
                check(deckDao.updateDeck(savedDeck) == 1) {
                    "Багцыг шинэчилж чадсангүй."
                }
            }

            // Хуучин зураг солигдсон эсвэл арилсан бол цэвэрлэнэ.
            // Хэвээр үлдсэн зургийг устгахгүй.
            val imageToDelete =
                if (existingDeck?.coverImageFileName != finalImageFileName) {
                    existingDeck?.coverImageFileName
                } else {
                    null
                }

            // Pair: хоёр утгыг хамтад нь буцаах энгийн бүтэц.
            // first = хадгалсан багцын ID.
            // second = database хадгалсны дараа устгах хуучин зураг.
            Pair(savedDeck.id, imageToDelete)
        }
    }

    // ───── КАРТ ҮҮСГЭХ / ЗАСАХ ─────

    // cardId = null бол шинэ карт үүсгэнэ.
    //
    // resetProgress:
    // true  → агуулга өөрчлөгдсөн үед New төлөвт буцаана.
    // false → агуулга өөрчлөгдсөн ч ахицыг хадгална.
    // null  → хэрэглэгч хараахан сонгоогүй.
    //
    // Суралцсан картын агуулга өөрчлөгдсөн бол
    // хэрэглэгчийн сонголтыг заавал дамжуулах ёстой.
    suspend fun saveCard(
        deckId: String,
        question: String,
        answer: String,
        cardId: String? = null,
        selectedImageUri: Uri? = null,
        removeImage: Boolean = false,
        resetProgress: Boolean? = null
    ): String {

        val cleanedQuestion = question.trim()
        val cleanedAnswer = answer.trim()

        require(cleanedQuestion.isNotEmpty()) {
            "Асуултаа оруулна уу."
        }

        require(cleanedAnswer.isNotEmpty()) {
            "Богино хариултаа оруулна уу."
        }

        require(!(selectedImageUri != null && removeImage)) {
            "Зураг сонгох болон арилгахыг зэрэг хийх боломжгүй."
        }

        return saveWithImage(selectedImageUri) { newImageFileName ->

            requireNotNull(deckDao.getDeckById(deckId)) {
                "Карт хадгалах багц олдсонгүй."
            }

            val existingCard = if (cardId == null) {
                null
            } else {
                requireNotNull(flashcardDao.getCardById(cardId)) {
                    "Засах карт олдсонгүй."
                }
            }

            // Энэ form-оор картыг өөр багц руу шилжүүлэхгүй.
            require(existingCard == null || existingCard.deckId == deckId) {
                "Карт сонгосон багцад хамаарахгүй байна."
            }

            val contentChanged = existingCard != null &&
                    (
                            existingCard.question != cleanedQuestion ||
                                    existingCard.answer != cleanedAnswer
                            )

            // Зөвхөн суралцсан картын асуулт/хариулт өөрчлөгдсөн үед
            // ахиц шинэчлэх сонголт шаардлагатай.
            val needsProgressDecision =
                existingCard != null &&
                        existingCard.boxLevel > 0 &&
                        contentChanged

            val shouldResetProgress = if (needsProgressDecision) {
                requireNotNull(resetProgress) {
                    "Ахиц шинэчлэх эсвэл хадгалах сонголтоо хийнэ үү."
                }
            } else {
                false
            }

            val finalImageFileName = when {
                removeImage -> null
                newImageFileName != null -> newImageFileName
                else -> existingCard?.imageFileName
            }

            val savedCard = if (existingCard == null) {

                // Шинэ карт default утгуудаараа New төлөвтэй байна.
                FlashCard(
                    id = UUID.randomUUID().toString(),
                    deckId = deckId,
                    question = cleanedQuestion,
                    answer = cleanedAnswer,
                    imageFileName = finalImageFileName
                )
            } else {

                existingCard.copy(
                    question = cleanedQuestion,
                    answer = cleanedAnswer,
                    imageFileName = finalImageFileName,

                    boxLevel = if (shouldResetProgress) {
                        0
                    } else {
                        existingCard.boxLevel
                    },

                    lastReviewedAt = if (shouldResetProgress) {
                        null
                    } else {
                        existingCard.lastReviewedAt
                    },

                    nextReviewAt = if (shouldResetProgress) {
                        null
                    } else {
                        existingCard.nextReviewAt
                    }
                )
            }

            if (existingCard == null) {
                flashcardDao.insertCard(savedCard)
            } else {
                check(flashcardDao.updateCard(savedCard) == 1) {
                    "Картыг шинэчилж чадсангүй."
                }
            }

            val imageToDelete =
                if (existingCard?.imageFileName != finalImageFileName) {
                    existingCard?.imageFileName
                } else {
                    null
                }

            Pair(savedCard.id, imageToDelete)
        }
    }

    // ───── КАРТ УСТГАХ ─────

    suspend fun deleteCard(cardId: String) {
        withContext(NonCancellable) {

            // Эхлээд database-аас устгана.
            // Амжилттай болсны дараа устгах зургийн нэрийг авна.
            val imageToDelete = database.withTransaction {

                val card = flashcardDao.getCardById(cardId)

                // Аль хэдийн устсан бол дахин устгах шаардлагагүй.
                if (card == null) {
                    return@withTransaction null
                }

                check(flashcardDao.deleteCardById(cardId) == 1) {
                    "Картыг устгаж чадсангүй."
                }

                card.imageFileName
            }

            // Database устгал амжилттай дууссаны дараа файл цэвэрлэнэ.
            deleteUnusedImage(imageToDelete)
        }
    }

    // ───── БАГЦ БОЛОН ТҮҮНИЙ КАРТУУДЫГ УСТГАХ ─────

    suspend fun deleteDeck(deckId: String) {
        withContext(NonCancellable) {

            val imagesToDelete = database.withTransaction {

                val deck = deckDao.getDeckById(deckId)

                if (deck == null) {
                    return@withTransaction emptyList<String>()
                }

                // CASCADE ажиллахаас өмнө картын зургуудын нэрийг авна.
                val cards = flashcardDao.getCardsByDeckId(deckId)

                val imageNames = mutableListOf<String>()

                // ?.let: утга null биш үед л доторх код ажиллана.
                deck.coverImageFileName?.let { fileName ->
                    imageNames.add(fileName)
                }

                for (card in cards) {
                    card.imageFileName?.let { fileName ->
                        imageNames.add(fileName)
                    }
                }

                // Холбоотой картууд ForeignKey.CASCADE-аар устна.
                check(deckDao.deleteDeckById(deckId) == 1) {
                    "Багцыг устгаж чадсангүй."
                }

                // distinct(): ижил нэр байвал нэг удаа цэвэрлэнэ.
                imageNames.distinct()
            }

            for (fileName in imagesToDelete) {
                deleteUnusedImage(fileName)
            }
        }
    }

    // ───── ЗУРАГ БА DATABASE ХАДГАЛАЛТЫГ ХОЛБОХ ─────

    // Зурагтай Deck, Card хадгалахад ижил дараалал хэрэгтэй.
    // Давхардсан кодыг энэ private функцэд төвлөрүүлсэн.
    //
    // saveToDatabase: нэг параметртэй suspend функц хүлээн авна.
    // Параметр нь шинээр хадгалсан зургийн нэр эсвэл null.
    //
    // Тэр функц Pair буцаана:
    // first  → хадгалсан Deck/Card-ийн ID.
    // second → устгах хуучин зургийн нэр эсвэл null.
    private suspend fun saveWithImage(
        selectedImageUri: Uri?,
        saveToDatabase: suspend (String?) -> Pair<String, String?>
    ): String = withContext(NonCancellable) {

        // Энэ хадгалалт эхэлсний дараа дэлгэц хаагдсанаас үүдсэн
        // coroutine cancellation-аар дундаас нь таслахгүй.
        //
        // NonCancellable нь background thread гэсэн үг биш.
        // ImageStorage нь файлын ажлаа Dispatchers.IO дээр хийдэг.
        // Room нь database-ийн асинхрон үйлдлээ өөрөө зохицуулна.

        val newImageFileName = if (selectedImageUri == null) {
            null
        } else {
            imageStorage.saveImage(selectedImageUri)
        }

        val savedResult = try {

            // Эхлээд шинэ зураг бэлэн болсон.
            // Одоо database дахь мэдээллийг хадгална.
            database.withTransaction {
                saveToDatabase(newImageFileName)
            }

        } catch (exception: Exception) {

            // Database хадгалалт бүтэлгүйтсэн бол
            // шинээр хуулсан, ашиглагдахгүй зургийг цэвэрлэнэ.
            deleteUnusedImage(newImageFileName)

            // Үндсэн алдааг ViewModel рүү дамжуулна.
            throw exception
        }

        // Database хадгалалт амжилттай болсон.
        // Одоо хуучин зургийг устгаж болно.
        deleteUnusedImage(savedResult.second)

        savedResult.first
    }

    // Ашиглагдахгүй болсон зургийг цэвэрлэнэ.
    private suspend fun deleteUnusedImage(fileName: String?) {

        if (fileName == null) {
            return
        }

        try {
            imageStorage.deleteImage(fileName)

        } catch (exception: IOException) {

            // Database хадгалалт/устгал аль хэдийн амжилттай болсон.
            // Хуучин файл цэвэрлэх алдааг "хадгалалт бүтэлгүйтсэн"
            // гэж хэрэглэгчид буруу мэдээлэхгүй.
            //
            // Log.w: Android Studio-ийн Logcat-д анхааруулга бичнэ.
            Log.w(
                "FlashcardRepository",
                "Ашиглагдахгүй зургийг цэвэрлэж чадсангүй.",
                exception
            )
        }
    }
}