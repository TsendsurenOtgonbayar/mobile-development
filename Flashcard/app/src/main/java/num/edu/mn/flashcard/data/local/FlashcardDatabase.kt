package num.edu.mn.flashcard.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import num.edu.mn.flashcard.data.models.Deck
import num.edu.mn.flashcard.data.models.FlashCard

// @Database: Энэ class нь Room database-ийн бүтцийг тодорхойлно.
@Database(

    // Database-д ямар хүснэгтүүд байхыг бүртгэнэ.
    // Deck → decks хүснэгт.
    // FlashCard → flashcards хүснэгт.
    entities = [
        Deck::class,
        FlashCard::class
    ],

    // Database-ийн бүтцийн анхны хувилбар.
    // Энэ нь аппын versionCode-оос тусдаа ойлголт.
    version = 1,

    // Database-ийн бүтцийг JSON файл болгон гаргана.
    // Өмнө Gradle-д тохируулсан app/schemas хавтаст хадгалагдана.
    // Энэ JSON нь хэрэглэгчийн өгөгдөл биш, хүснэгтийн бүтэц юм.
    exportSchema = true
)

// abstract: Энэ class-ийн бүрэн хэрэгжүүлэлтийг энд бичихгүй.
// Room нь build хийх үед хэрэгжүүлсэн class-ийг үүсгэнэ.
//
// : RoomDatabase(): RoomDatabase суурь class-аас удамшиж байна.
abstract class FlashcardDatabase : RoomDatabase() {

    // Багцын хүснэгттэй ажиллах DAO-г өгнө.
    // Функцийн хэрэгжүүлэлтийг Room үүсгэнэ.
    abstract fun deckDao(): DeckDao

    // Картын хүснэгттэй ажиллах DAO-г өгнө.
    abstract fun flashcardDao(): FlashcardDao
}