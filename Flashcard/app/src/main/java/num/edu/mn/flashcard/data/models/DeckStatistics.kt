package num.edu.mn.flashcard.data.models

// Энэ class нь database хүснэгт биш.
// Картуудаас тооцож гаргасан статистикийг UI-д дамжуулна.
// Тиймээс @Entity annotation байхгүй.
data class DeckStatistics(

    // Багцын нийт картын тоо.
    val cardCount: Int = 0,

    // boxLevel = 0 буюу хараахан суралцаагүй картууд.
    val newCount: Int = 0,

    // boxLevel нь 1, 2, 3 байгаа картууд.
    val learningCount: Int = 0,

    // boxLevel нь 4 эсвэл 5 байгаа картууд.
    val masteredCount: Int = 0,

    // Суралцаж эхэлсэн бөгөөд давтах хугацаа нь болсон картууд.
    // New картуудыг энд оруулахгүй, newCount-д тусад нь тоолно.
    //

    // Mastered картын давтах хугацаа болсон бол
    // masteredCount болон dueCount-д хоёуланд нь орж болно.
    val dueCount: Int = 0
) {

    // get(): энэ property-г унших бүрд бусад талбараас тооцно.
    // Тусдаа хадгалсан хувь биш тул тоонуудтайгаа зөрөхгүй.
    val masteryPercent: Float
        get() = if (cardCount == 0) {

            // Картгүй үед тэгээр хуваахаас хамгаална.
            0f
        } else {

            // toFloat(): бүхэл тооны хуваалт хийхээс сэргийлнэ.
            // Жишээ: 3 / 10 = 0, харин 3f / 10 = 0.3.
            masteredCount.toFloat() / cardCount * 100f
        }
}