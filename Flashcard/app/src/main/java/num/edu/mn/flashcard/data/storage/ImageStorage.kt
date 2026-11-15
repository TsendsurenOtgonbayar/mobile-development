package num.edu.mn.flashcard.data.storage

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

// Context-ийг гаднаас авна.
// Context нь Android-ийн файлын сан, ContentResolver зэрэг
// боломжид хандахад хэрэгтэй.
class ImageStorage(context: Context) {

    // Activity-ийн Context-ийг удаан хадгалахгүй.
    // Аппын амьдралын хугацаатай applicationContext ашиглана.
    private val appContext = context.applicationContext

    // Зургуудаа аппын filesDir доторх images хавтаст хадгална.
    //
    // File(...) нь замыг илэрхийлэх объект үүсгэнэ.
    // Энэ мөр өөрөө хавтас үүсгэхгүй.
    private val imageDirectory = File(
        appContext.filesDir,
        "images"
    )

    // Зөвхөн энэ class-ийн үүсгэсэн хэлбэртэй файлын нэрийг зөвшөөрнө.
    // Ингэснээр гаднаас өөр хавтас руу заасан зам дамжуулахгүй.
    private val imageFileNamePattern = Regex(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.img"
    )

    // Сонгосон зургийг дотоод санд хуулж,
    // амжилттай бол хадгалсан файлын нэрийг буцаана.
    //
    // suspend: coroutine дотроос дуудна.
    suspend fun saveImage(sourceUri: Uri): String =
        withContext(Dispatchers.IO) {

            // Dispatchers.IO: файл унших, бичих ажлыг
            // UI-ийн main thread-ээс өөр thread дээр ажиллуулна.

            // Хавтас байхгүй бол үүсгэнэ.
            // Үүсгэж чадаагүй бол алдаа гаргана.
            if (!imageDirectory.isDirectory) {
                if (!imageDirectory.mkdirs() &&
                    !imageDirectory.isDirectory
                ) {
                    throw IOException(
                        "Зураг хадгалах хавтас үүсгэж чадсангүй."
                    )
                }
            }

            // Давтагдах магадлал маш бага шинэ нэр үүсгэнэ.
            //
            // .img нь манай хадгалалтын ерөнхий өргөтгөл.
            // Зургийн жинхэнэ форматыг өөрчлөхгүй:
            // JPEG бол JPEG, PNG бол PNG хэвээр хуулна.
            val fileName = "${UUID.randomUUID()}.img"

            val destinationFile = File(
                imageDirectory,
                fileName
            )

            try {
                // Uri нь энгийн файлын зам байх албагүй.
                // Тиймээс ContentResolver-оор унших урсгал нээнэ.
                val inputStream = appContext.contentResolver
                    .openInputStream(sourceUri)
                    ?: throw IOException(
                        "Сонгосон зургийг нээж чадсангүй."
                    )

                // use: ажил дуусах эсвэл алдаа гарахад
                // stream-ийг автоматаар хаана.
                inputStream.use { input ->

                    destinationFile.outputStream().use { output ->

                        // Эх үүсвэрийн байтуудыг шинэ файлд хуулна.
                        // Зургийг бүхэлд нь Bitmap болгон санах ойд
                        // задлах шаардлагагүй.
                        input.copyTo(output)
                    }
                }

                // Хоосон файл үүссэн бол амжилттай гэж үзэхгүй.
                if (destinationFile.length() == 0L) {
                    throw IOException(
                        "Сонгосон зургийн файл хоосон байна."
                    )
                }

                // withContext блокийн сүүлийн утга нь
                // saveImage() функцийн буцаах утга болно.
                fileName

            } catch (exception: Exception) {

                // Хуулах явцад алдаа гарвал хагас бичигдсэн
                // файлыг боломжтой бол устгана.
                destinationFile.delete()

                // Алдааг нуухгүй, дуудагч талд дамжуулна.
                // Дараа нь ViewModel хэрэглэгчид мэдээлнэ.
                throw exception
            }
        }

    // Database-д хадгалсан файлын нэрээс File объект гаргана.
    // Дараа нь зураг харуулах Composable-д дамжуулна.
    //
    // Энэ функц зургийн байтуудыг уншихгүй.
    // Файлын замыг илэрхийлэх объект л үүсгэнэ.
    fun getImageFile(fileName: String): File {

        require(imageFileNamePattern.matches(fileName)) {
            "Зургийн файлын нэр буруу байна."
        }

        return File(
            imageDirectory,
            fileName
        )
    }

    // Хэрэггүй болсон зургийн файлыг устгана.
    // Жишээ нь зураг солигдсон эсвэл карт устсан үед.
    suspend fun deleteImage(fileName: String) {
        withContext(Dispatchers.IO) {

            val imageFile = getImageFile(fileName)

            // Файл аль хэдийн байхгүй бол дахин устгах шаардлагагүй.
            if (imageFile.exists() && !imageFile.delete()) {
                throw IOException(
                    "Зургийн файлыг устгаж чадсангүй."
                )
            }
        }
    }
}