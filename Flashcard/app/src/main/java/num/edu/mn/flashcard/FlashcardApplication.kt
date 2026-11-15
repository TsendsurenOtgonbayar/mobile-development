package num.edu.mn.flashcard

import android.app.Application
import num.edu.mn.flashcard.di.AppContainer

// Application: аппын process-ийн хэмжээнд амьдардаг Android class.
//
// Activity нь нэг дэлгэцийн орчныг хариуцдаг бол
// Application нь аппын нийтлэг объектуудыг эзэмшихэд тохиромжтой.
//
// Manifest дээр бүртгэснээр Android энэ class-ийн объектыг үүсгэнэ.
class FlashcardApplication : Application() {

    // AppContainer-ийг анх ашиглах үед нэг удаа үүсгэнэ.
    // Дараа нь энэ Application instance-ээс хандах бүрд
    // өмнөх container-ийг буцаана.
    val container: AppContainer by lazy {

        AppContainer(
            // this нь одоогийн FlashcardApplication объект.
            // Application нь өөрөө Context учраас дамжуулж болно.
            context = this
        )
    }
}