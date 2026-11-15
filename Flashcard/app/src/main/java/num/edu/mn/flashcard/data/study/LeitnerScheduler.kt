package num.edu.mn.flashcard.data.study

import java.util.concurrent.TimeUnit
import num.edu.mn.flashcard.data.models.FlashCard

// Leitner-ийн дүрмийг нэг газарт төвлөрүүлнэ.
// Flashcards болон Learn горим хоёулаа энэ дүрмийг ашиглана.
//
// Android UI болон Room-оос хамааралгүй энгийн Kotlin class.
// Тиймээс хугацаа, хайрцгийн дүрмийг тусад нь шалгахад хялбар.
class LeitnerScheduler {

    // 7 хоногийг миллисекунд болгон хувиргана.
    // Хугацааны бүх харьцуулалтыг ижил нэгжээр хийнэ.
    private val inactivityLimitMillis = TimeUnit.DAYS.toMillis(7)

    // Тухайн хайрцагт орсны дараа хэдий хугацааны дараа
    // дахин давтахыг миллисекундээр буцаана.
    //
    // private: зөвхөн энэ class дотроос ашиглах функц.
    private fun getReviewIntervalMillis(boxLevel: Int): Long {
        return when (boxLevel) {
            1 -> TimeUnit.HOURS.toMillis(1)
            2 -> TimeUnit.HOURS.toMillis(6)
            3 -> TimeUnit.HOURS.toMillis(24)
            4 -> TimeUnit.HOURS.toMillis(48)
            5 -> TimeUnit.HOURS.toMillis(72)

            // 0 нь New төлөв тул давталтын интервал байхгүй.
            // Бусад тоо ч хүчинтэй хайрцаг биш.
            else -> error("Давталтын хайрцаг 1–5 байх ёстой.")
        }
    }

    // 7-оос олон хоног үнэлүүлээгүй картыг шалгана.
    //
    // currentTime: одоогийн хугацаа, миллисекундээр.
    // Хугацааг гаднаас авдаг тул тестэд хүссэн цаг дамжуулж болно.
    //
    // Энэ функц database-д бичихгүй.
    // Өөрчлөлттэй эсвэл анхны картыг буцаана.
    fun resetExpiredProgress(
        card: FlashCard,
        currentTime: Long
    ): FlashCard {

        // require: нөхцөл буруу бол алдаа гаргана.
        // Програмын дотоод буруу төлөвийг эрт илрүүлэх шалгалт.
        require(card.boxLevel in 0..5) {
            "Картын boxLevel 0–5 хооронд байх ёстой."
        }

        // Шинэ карт суралцаж эхлээгүй тул
        // 7 хоногийн буулгах дүрэмд хамаарахгүй.
        if (card.boxLevel == 0) {
            return card
        }

        // Суралцсан карт хамгийн сүүлд үнэлүүлсэн хугацаатай байна.
        // requireNotNull: null биш утгыг авч, Long болгон ашиглана.
        val lastReviewedAt = requireNotNull(card.lastReviewedAt) {
            "Суралцсан карт lastReviewedAt утгатай байх ёстой."
        }

        val nextReviewAt = requireNotNull(card.nextReviewAt) {
            "Суралцсан карт nextReviewAt утгатай байх ёстой."
        }

        // Яг 7 хоног биш, 7 хоногоос ИЛҮҮ өнгөрсөн үед буулгана.
        val hasExpired =
            currentTime - lastReviewedAt > inactivityLimitMillis

        if (!hasExpired) {
            return card
        }

        // Аль хэдийн Box 1-д, шууд давтах боломжтой байгаа бол
        // дахин дахин өөрчлөх шаардлагагүй.
        if (card.boxLevel == 1 && nextReviewAt <= currentTime) {
            return card
        }

        // copy(): анхны объектыг өөрчлөхгүй,
        // өгсөн талбаруудыг сольсон шинэ объект үүсгэнэ.
        return card.copy(
            boxLevel = 1,

            // Дахин 1 цаг хүлээлгэхгүй, шууд давтах боломж олгоно.
            nextReviewAt = currentTime

            // lastReviewedAt-ийг өөрчлөхгүй.
            // Автоматаар буулгасан нь хэрэглэгч давтсан гэсэн үг биш.
        )
    }

    // Карт одоо хуваарьт суралцах боломжтой эсэхийг шалгана.
    //
    // Энэ функцийг ашиглахын өмнө 7 хоногийн дүрмээр
    // төлөвийг resetExpiredProgress() ашиглан шинэчилсэн байна.
    fun isReadyForReview(
        card: FlashCard,
        currentTime: Long
    ): Boolean {

        require(card.boxLevel in 0..5) {
            "Картын boxLevel 0–5 хооронд байх ёстой."
        }

        // Шинэ картыг шууд суралцаж болно.
        if (card.boxLevel == 0) {
            return true
        }

        val nextReviewAt = requireNotNull(card.nextReviewAt) {
            "Суралцсан карт nextReviewAt утгатай байх ёстой."
        }

        // Товлосон цаг болсон эсвэл өнгөрсөн бол давтаж болно.
        return nextReviewAt <= currentTime
    }

    // Нэг хуваарьт үнэлгээний дараах шинэ төлөвийг гаргана.
    //
    // isCorrect:
    // true  → Learn зөв, эсвэл Flashcards "Санаж байна".
    // false → Learn буруу, эсвэл Flashcards "Санахгүй байна".
    //
    // Practice горим энэ функцийг дуудахгүй.
    // Учир нь Practice хайрцаг, хуваарийг өөрчлөхгүй.
    fun reviewCard(
        card: FlashCard,
        isCorrect: Boolean,
        currentTime: Long
    ): FlashCard {

        // Эхлээд 7 хоногийн дүрмийг хэрэгжүүлнэ.
        // Хуучин mastered төлөвөөс шууд ахиулахаас сэргийлнэ.
        val currentCard = resetExpiredProgress(
            card = card,
            currentTime = currentTime
        )

        // Хугацаа болоогүй картыг хуваарьт үнэлгээгээр
        // дахин дахин ахиулахаас хамгаална.
        require(
            isReadyForReview(
                card = currentCard,
                currentTime = currentTime
            )
        ) {
            "Энэ картын давтах хугацаа хараахан болоогүй байна."
        }

        // when: нөхцөлүүдийг дээрээс нь шалгаж,
        // хамгийн эхний биелсэн нөхцөлийн утгыг авна.
        val nextBoxLevel = when {

            // Анхны үнэлгээ зөв эсвэл буруу байсан ч Box 1.
            // Ингэснээр эхний 1 цагийн давталтыг алгасахгүй.
            currentCard.boxLevel == 0 -> 1

            // ! нь Boolean утгыг эсрэг болгоно.
            // Буруу хариулсан бол аль ч хайрцгаас Box 1 рүү буцна.
            !isCorrect -> 1

            // Зөв хариулсан бол нэг хайрцаг ахиулна.
            // coerceAtMost(5): үр дүнг 5-аас их болгохгүй.
            else -> (currentCard.boxLevel + 1).coerceAtMost(5)
        }

        val intervalMillis = getReviewIntervalMillis(
            boxLevel = nextBoxLevel
        )

        return currentCard.copy(
            boxLevel = nextBoxLevel,

            // Бодитоор үнэлгээ өгсөн мөч.
            lastReviewedAt = currentTime,

            // Шинэ хайрцгийн хугацааг энэ үнэлгээний мөчөөс тоолно.
            nextReviewAt = currentTime + intervalMillis
        )
    }
}