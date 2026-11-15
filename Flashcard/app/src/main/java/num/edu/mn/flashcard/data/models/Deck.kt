package num.edu.mn.flashcard.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity: Энэ class-ийн өгөгдлийг Room-ийн хүснэгтэд хадгална.
// tableName: Database доторх хүснэгтийн нэр.
@Entity(tableName = "decks")
data class Deck(

    // @PrimaryKey: Багц бүрийг давтагдашгүй ялгах үндсэн түлхүүр.
    // UUID-г багц үүсгэх үйлдэл дотор үүсгэж энд дамжуулна.
    @PrimaryKey
    val id: String,

    // Багцын нэр.
    // Хоосон нэртэй багц хадгалахгүй байх шалгалтыг
    // form болон хадгалах үйлдэл дээр хэрэгжүүлнэ.
    val title: String,

    // Багцын тайлбар.
    // Хэрэглэгч тайлбар бичээгүй бол хоосон String байна.
    val description: String = "",

    // Аппын дотоод санд хадгалсан нүүр зургийн файлын нэр.
    // Энд зургийн пиксел эсвэл Bitmap хадгалахгүй.
    //
    // String? гэдэг нь String эсвэл null утгатай байж болно гэсэн үг.
    // null бол хэрэглэгч зураг сонгоогүй тул UI default зураг харуулна.
    val coverImageFileName: String? = null,

    // Энэ багцын карт дээр хамгийн сүүлд үнэлгээ өгсөн хугацаа.
    // Unix epoch-оос хойших миллисекундээр хадгална.
    //
    // Long? ашигласан учир огт суралцаагүй багц null утгатай байна.
    // Зүгээр багцын дэлгэцийг нээхэд энэ хугацааг шинэчлэхгүй.
    val lastStudiedAt: Long? = null
)