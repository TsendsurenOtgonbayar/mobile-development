package num.edu.mn.flashcard

import java.util.concurrent.TimeUnit
import num.edu.mn.flashcard.data.models.FlashCard
import num.edu.mn.flashcard.data.study.LeitnerScheduler
import org.junit.Assert.*
import org.junit.Test

class LeitnerSchedulerTest {
    private val scheduler = LeitnerScheduler()
    private val newCard = FlashCard("card", "deck", "Question", "Answer")

    @Test fun correctAnswersFollowAllFiveIntervals() {
        var card = newCard
        for ((index, hours) in listOf(1L, 6L, 24L, 48L, 72L, 72L).withIndex()) {
            val now = card.nextReviewAt ?: 0L
            assertTrue(scheduler.isReadyForReview(card, now))
            card = scheduler.reviewCard(card, true, now)
            assertEquals(minOf(index + 1, 5), card.boxLevel)
            val due = now + TimeUnit.HOURS.toMillis(hours)
            assertEquals(due, card.nextReviewAt)
            assertFalse(scheduler.isReadyForReview(card, due - 1))
        }
    }

    @Test fun wrongAnswerRestartsAtOneHour() {
        val card = newCard.copy(boxLevel = 5, lastReviewedAt = 0L, nextReviewAt = 1L)
        val reviewed = scheduler.reviewCard(card, false, 1L)
        assertEquals(1, reviewed.boxLevel)
        assertEquals(1L + TimeUnit.HOURS.toMillis(1), reviewed.nextReviewAt)
    }

    @Test fun inactivityResetsOnlyAfterSevenFullDays() {
        val card = newCard.copy(boxLevel = 4, lastReviewedAt = 0L, nextReviewAt = 1L)
        val week = TimeUnit.DAYS.toMillis(7)
        assertEquals(card, scheduler.resetExpiredProgress(card, week))
        val expired = scheduler.resetExpiredProgress(card, week + 1)
        assertEquals(1, expired.boxLevel)
        assertTrue(scheduler.isReadyForReview(expired, week + 1))
    }
}
