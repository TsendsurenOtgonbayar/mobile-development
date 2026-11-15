package num.edu.mn.flashcard.di

import android.content.Context
import androidx.room.Room
import num.edu.mn.flashcard.data.local.FlashcardDatabase
import num.edu.mn.flashcard.data.repository.FlashcardRepository
import num.edu.mn.flashcard.data.storage.ImageStorage
import num.edu.mn.flashcard.data.study.LeitnerScheduler

// Аппын нийтлэг объектуудыг үүсгэж, хооронд нь холбоно.
//
// Энэ class өөрөө дэлгэц харуулахгүй,
// картын үнэлгээ тооцохгүй, SQL query бичихгүй.
class AppContainer(context: Context) {

    // Activity-ийн оронд аппын Context-ийг ашиглана.
    // Эдгээр объектууд нэг дэлгэцээс удаан хугацаанд ашиглагдана.
    private val appContext = context.applicationContext

    // by lazy:
    // Энэ property-г анх ашиглах үед блок нэг удаа ажиллана.
    // Дараагийн удаа өмнө үүсгэсэн объектыг буцаана.
    //
    // Энэ нь Room-ийн өгөгдөл хадгалах механизм биш.
    // Зөвхөн объект үүсгэх хугацааг удирдаж байна.
    private val database: FlashcardDatabase by lazy {

        // Room.databaseBuilder(): database үүсгэх тохиргоог бэлдэнэ.
        Room.databaseBuilder(

            // Database-ийн файлыг аппын орчинд байрлуулах Context.
            appContext,

            // Өмнө бичсэн database-ийн тодорхойлолт.
            // .java: Room-ийн API-д хэрэгтэй Java Class хэлбэр.
            FlashcardDatabase::class.java,

            // Төхөөрөмж дээр хадгалах database файлын нэр.
            "flashcard.db"

        ).build()

        // build(): тохиргооноос database instance үүсгэнэ.
        // Room database холболтоо хэрэг болох үед нээнэ.
    }

    // Leitner-ийн дүрэм тооцох нэг объект.
    private val leitnerScheduler: LeitnerScheduler by lazy {
        LeitnerScheduler()
    }

    // Зургийн файлуудтай ажиллах объект.
    //
    // UI зураг харуулахдаа getImageFile()-ийг ашиглах боломжтой
    // байлгахын тулд private болгоогүй.
    val imageStorage: ImageStorage by lazy {
        ImageStorage(
            context = appContext
        )
    }

    // Repository-д хэрэгтэй гурван объектыг дамжуулна.
    //
    // Энэ property-г анх ашиглах үед доорх dependency-уудын
    // lazy блокууд шаардлагатай дарааллаар ажиллана.
    val flashcardRepository: FlashcardRepository by lazy {
        FlashcardRepository(
            database = database,
            leitnerScheduler = leitnerScheduler,
            imageStorage = imageStorage
        )
    }
}