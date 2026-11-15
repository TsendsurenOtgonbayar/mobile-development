package num.edu.mn.flashcard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import num.edu.mn.flashcard.data.models.DeckStatistics
import num.edu.mn.flashcard.data.models.FlashCard

@Dao
interface FlashcardDao {

    // Тухайн багцын картуудыг өөрчлөлттэй нь ажиглана.
    // Manage Cards дэлгэцэд ашиглана.
    //
    // id-аар эрэмбэлэх нь тогтвортой дараалал өгнө.
    // Энэ нь үүсгэсэн огнооны дараалал гэсэн үг биш.
    @Query(
        """
        SELECT *
        FROM flashcards
        WHERE deckId = :deckId
        ORDER BY id ASC
        """
    )
    fun observeCardsByDeckId(
        deckId: String
    ): Flow<List<FlashCard>>

    // Тухайн багцын бүх картыг нэг удаа уншина.
    // Жишээ нь багц устгахын өмнө картуудын зургийн
    // файлын нэрийг цуглуулахад ашиглана.
    @Query(
        """
        SELECT *
        FROM flashcards
        WHERE deckId = :deckId
        ORDER BY id ASC
        """
    )
    suspend fun getCardsByDeckId(
        deckId: String
    ): List<FlashCard>

    // Нэг картыг id-аар уншина.
    // Карт олдохгүй бол null буцаана.
    @Query(
        """
        SELECT *
        FROM flashcards
        WHERE id = :cardId
        """
    )
    suspend fun getCardById(
        cardId: String
    ): FlashCard?

    // Шинэ карт нэмнэ.
    // Ижил id байвал хуучин картыг дарж солихгүй.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCard(card: FlashCard)

    // Байгаа картыг id-аар нь олж шинэчилнэ.
    // Асуулт, хариулт, зураг болон ахицыг хамтад нь
    // шинэчлэх шаардлагатай үед ашиглаж болно.
    //
    // 1 = шинэчилсэн, 0 = карт олдоогүй.
    @Update
    suspend fun updateCard(card: FlashCard): Int

    // Зөвхөн суралцах төлөвийг шинэчилнэ.
    // Асуулт, хариулт, зурагт өөрчлөлт оруулахгүй.
    //
    // Шинэ boxLevel болон хугацаануудыг DAO тооцохгүй.
    // LeitnerScheduler тооцоолж, Repository энд дамжуулна.
    //
    // Хугацаанууд nullable байгаа нь картыг New төлөвт
    // буцаахдаа null утга өгөх боломж олгоно.
    @Query(
        """
        UPDATE flashcards
        SET boxLevel = :boxLevel,
            lastReviewedAt = :lastReviewedAt,
            nextReviewAt = :nextReviewAt
        WHERE id = :cardId
        """
    )
    suspend fun updateStudyProgress(
        cardId: String,
        boxLevel: Int,
        lastReviewedAt: Long?,
        nextReviewAt: Long?
    ): Int

    // Нэг картыг устгана.
    // Зургийн файлыг Repository + ImageStorage цэвэрлэнэ.
    @Query(
        """
        DELETE FROM flashcards
        WHERE id = :cardId
        """
    )
    suspend fun deleteCardById(
        cardId: String
    ): Int

    // Суралцах ээлжид орох картуудыг нэг удаа авна.
    //
    // 1. Шинэ картууд: boxLevel = 0.
    // 2. Давтах хугацаа болсон картууд:
    //    nextReviewAt <= currentTime.
    //
    // Хаалт нь OR нөхцөлийг зөвхөн сонгосон Deck-ийн
    // картуудад үйлчлэхээр бүлэглэж байна.
    //
    // Эхлээд хугацаа болсон картуудыг,
    // дараа нь шинэ картуудыг өгнө.
    @Query(
        """
        SELECT *
        FROM flashcards
        WHERE deckId = :deckId
          AND (
              boxLevel = 0
              OR (
                  boxLevel BETWEEN 1 AND 5
                  AND nextReviewAt IS NOT NULL
                  AND nextReviewAt <= :currentTime
              )
          )
        ORDER BY
            CASE WHEN boxLevel = 0 THEN 1 ELSE 0 END ASC,
            nextReviewAt ASC,
            id ASC
        """
    )
    suspend fun getStudyCards(
        deckId: String,
        currentTime: Long
    ): List<FlashCard>

    // Багцын статистикийг картуудаас тооцно.
    //
    // AS-ын ард байгаа нэрүүд DeckStatistics-ийн
    // constructor дахь property нэрүүдтэй таарна.
    //
    // COUNT(*) нь нийт мөрийн тоог гаргана.
    // COUNT(CASE ...) нь нөхцөл биелсэн мөрүүдийг тоолно.
    // Нөхцөл биелээгүй үед CASE нь null өгдөг бөгөөд
    // COUNT нь null утгуудыг тоолдоггүй.
    @Query(
        """
        SELECT
            COUNT(*) AS cardCount,

            COUNT(
                CASE WHEN boxLevel = 0 THEN 1 END
            ) AS newCount,

            COUNT(
                CASE WHEN boxLevel BETWEEN 1 AND 3 THEN 1 END
            ) AS learningCount,

            COUNT(
                CASE WHEN boxLevel IN (4, 5) THEN 1 END
            ) AS masteredCount,

            COUNT(
                CASE
                    WHEN boxLevel BETWEEN 1 AND 5
                         AND nextReviewAt IS NOT NULL
                         AND nextReviewAt <= :currentTime
                    THEN 1
                END
            ) AS dueCount

        FROM flashcards
        WHERE deckId = :deckId
        """
    )
    fun observeDeckStatistics(
        deckId: String,
        currentTime: Long
    ): Flow<DeckStatistics>
}