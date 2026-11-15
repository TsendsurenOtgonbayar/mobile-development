package num.edu.mn.flashcard.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",

    // ForeignKey: картын deckId нь бодит Deck-ийн id-тай холбогдоно.
    // Ингэснээр байхгүй багцад карт хадгалахаас хамгаална.
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],

            // Багцыг устгахад түүнд хамаарах картын мөрүүд
            // database-аас автоматаар устна.
            // Зургийн файлуудыг тусад нь цэвэрлэх шаардлагатай.
            onDelete = ForeignKey.CASCADE
        )
    ],

    // deckId-аар карт хайх болон холбоос шалгах ажиллагааг
    // хурдасгах индекс үүсгэнэ.
    indices = [
        Index(value = ["deckId"])
    ]
)
data class FlashCard(

    // Карт бүрийг давтагдашгүй ялгах ID.
    @PrimaryKey
    val id: String,

    // Энэ карт аль багцад хамаарахыг заана.
    val deckId: String,

    // Картын нүүрэнд харуулах асуулт эсвэл томьёо.
    val question: String,

    // Flip хийхэд харуулах богино хариулт.
    // Learn горимд хэрэглэгчийн бичсэн хариулттай харьцуулна.
    val answer: String,

    // Сонголттой зургийн файлын нэр.
    // null бол картын нүүрэнд зөвхөн асуултыг харуулна.
    val imageFileName: String? = null,

    // 0: шинэ, хараахан үнэлээгүй карт.
    // 1..5: Leitner системийн хайрцгууд.
    //
    // 4 болон 5-р хайрцгийн картыг Mastered гэж тооцно.
    // isMastered гэсэн давхар талбар database-д хадгалахгүй.
    val boxLevel: Int = 0,

    // Хамгийн сүүлд хуваарьт суралцах үнэлгээ өгсөн хугацаа.
    // Unix epoch-оос хойших миллисекунд.
    // Шинэ карт хараахан үнэлэгдээгүй тул null байна.
    val lastReviewedAt: Long? = null,

    // Дараагийн хуваарьт давталтын хугацаа.
    // Шинэ карт null утгатай бөгөөд шууд суралцах боломжтой.
    val nextReviewAt: Long? = null
)