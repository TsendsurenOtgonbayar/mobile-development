package num.edu.mn.flashcard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import num.edu.mn.flashcard.data.models.Deck

// DAO = Data Access Object.
// @Dao: Энэ interface нь database-д хандах үйлдлүүдийг тодорхойлно.
//
// Бид функцүүдийн хэрэгжүүлэлтийг гараар бичихгүй.
// Room нь annotation болон SQL query-г уншаад
// хэрэгжүүлэлтийн кодыг build хийх үед үүсгэнэ.
@Dao
interface DeckDao {

    // Бүх багцыг нэрээр нь эрэмбэлж авна.
    // Нэр ижил үед id-аар эрэмбэлж дарааллыг тогтвортой байлгана.
    //
    // Flow: нэг удаа жагсаалт аваад дуусахгүй.
    // Ажиглаж байх хугацаанд decks хүснэгт өөрчлөгдвөл
    // query дахин ажиллаж шинэ жагсаалт өгнө.
    @Query(
        """
        SELECT *
        FROM decks
        ORDER BY title ASC, id ASC
        """
    )
    fun observeDecks(): Flow<List<Deck>>

    // Тодорхой нэг багцын мэдээллийг өөрчлөлттэй нь ажиглана.
    //
    // :deckId нь доорх функцийн deckId параметрийн утга.
    // Deck? учир багц олдохгүй эсвэл устсан бол null өгч болно.
    @Query(
        """
        SELECT *
        FROM decks
        WHERE id = :deckId
        """
    )
    fun observeDeckById(deckId: String): Flow<Deck?>

    // Нэг багцыг нэг удаа уншина.
    // Жишээлбэл хадгалах үйлдлийн өмнө багц байгаа эсэхийг шалгана.
    //
    // suspend: coroutine дотроос дуудна.
    // Room энэ асинхрон query-г main thread-ийг
    // блоклохгүйгээр гүйцэтгэнэ.
    @Query(
        """
        SELECT *
        FROM decks
        WHERE id = :deckId
        """
    )
    suspend fun getDeckById(deckId: String): Deck?

    // Шинэ багц нэмнэ.
    //
    // ABORT: ижил primary key буюу id аль хэдийн байвал
    // байгаа өгөгдлийг дарж солихгүй, алдаа гаргана.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDeck(deck: Deck)

    // Байгаа багцыг primary key буюу id-аар нь олж шинэчилнэ.
    // Шинэ багц үүсгэхгүй.
    //
    // Int: шинэчилсэн мөрийн тоо.
    // 1 = багц олдож шинэчлэгдсэн.
    // 0 = тухайн id-тай багц байхгүй.
    @Update
    suspend fun updateDeck(deck: Deck): Int

    // Зөвхөн сүүлд суралцсан хугацааг шинэчилнэ.
    // Нэр, тайлбар, зураг зэрэг бусад талбарыг өөрчлөхгүй.
    @Query(
        """
        UPDATE decks
        SET lastStudiedAt = :studiedAt
        WHERE id = :deckId
        """
    )
    suspend fun updateLastStudiedAt(
        deckId: String,
        studiedAt: Long
    ): Int

    // Багцыг id-аар устгана.
    //
    // FlashCard дээр ForeignKey.CASCADE тохируулсан учир
    // энэ багцын картын мөрүүд мөн database-аас устна.
    //
    // Анхаарах нь: энэ query зургийн файлуудыг устгахгүй.
    // Тэдгээрийг Repository болон ImageStorage хариуцна.
    //
    // Буцаах тоо нь устгасан Deck мөрийн тоо:
    // 1 = устгасан, 0 = багц олдоогүй.
    @Query(
        """
        DELETE FROM decks
        WHERE id = :deckId
        """
    )
    suspend fun deleteDeckById(deckId: String): Int
}