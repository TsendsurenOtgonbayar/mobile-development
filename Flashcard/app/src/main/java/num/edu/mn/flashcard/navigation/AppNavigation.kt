package num.edu.mn.flashcard.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import num.edu.mn.flashcard.ui.viewmodel.StudyMode
@Composable
fun AppNavigation() {

    // Дэлгэцийн шилжилт болон back stack-ийг удирдана.
    val navController = rememberNavController()

    // Route бүр ямар дэлгэц нээхийг тодорхойлно.
    NavHost(
        navController = navController,
        startDestination = "library"
    ) {

        // ───── БАГЦУУДЫН ЖАГСААЛТ ─────

        composable("library") { backStackEntry ->

            // LibraryRoute нь ViewModel-оо авч,
            // LibraryScreen-д өгөгдлийг дамжуулна.
            LibraryRoute(
                backStackEntry = backStackEntry,

                onDeckClick = { deckId ->
                    navController.navigate("deck/$deckId")
                },

                onAddDeckClick = {
                    // deckId дамжуулахгүй → шинэ багц.
                    navController.navigate("DeckEditor")
                }
            )
        }

        // ───── НЭГ БАГЦЫН ДЭЛГЭРЭНГҮЙ ─────

        composable(
            route = "deck/{deckId}",
            arguments = listOf(
                navArgument("deckId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val deckId = backStackEntry.arguments
                ?.getString("deckId")

            DeckDetailRoute(
                backStackEntry = backStackEntry,

                onBackClick = {
                    navController.popBackStack()
                },

                onFlashcardClick = { isPractice ->
                    if (!deckId.isNullOrBlank()) {
                        navController.navigate(
                            "Flashcard/$deckId?practice=$isPractice"
                        )
                    }
                },

                onLearnClick = { isPractice ->
                    if (!deckId.isNullOrBlank()) {
                        navController.navigate(
                            "LearnScreen/$deckId?practice=$isPractice"
                        )
                    }
                },

                onManageCardsClick = {
                    if (!deckId.isNullOrBlank()) {
                        navController.navigate(
                            "ManageCard/$deckId"
                        )
                    }
                },

                onEditDeckClick = {
                    if (!deckId.isNullOrBlank()) {
                        // deckId дамжуулна → байгаа багцыг засах.
                        navController.navigate(
                            "DeckEditor?deckId=$deckId"
                        )
                    }
                }
            )
        }

        // ───── БАГЦ ҮҮСГЭХ / ЗАСАХ ─────

        composable(
            route = "DeckEditor?deckId={deckId}",
            arguments = listOf(
                navArgument("deckId") {
                    type = NavType.StringType

                    // Шинэ багц үүсгэхэд ID байхгүй байж болно.
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->

            DeckFormRoute(
                backStackEntry = backStackEntry,

                onBackClick = {
                    navController.popBackStack()
                },

                onSaved = { _ ->
                    // Repository аль хэдийн Room-д хадгалсан.
                    // Энд зөвхөн өмнөх дэлгэц рүү буцна.
                    navController.popBackStack()
                }
            )
        }

        // ───── FLASHCARDS ─────

        composable(
            route = "Flashcard/{deckId}?practice={practice}",
            arguments = listOf(
                navArgument("deckId") {
                    type = NavType.StringType
                },
                navArgument("practice") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->

            StudyRoute(
                backStackEntry = backStackEntry,

                // Ижил Route, ViewModel, Screen ашиглах боловч
                // энэ entry нь Flashcard горимоор ажиллана.
                studyMode = StudyMode.FLASHCARD,

                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "LearnScreen/{deckId}?practice={practice}",
            arguments = listOf(
                navArgument("deckId") {
                    type = NavType.StringType
                },
                navArgument("practice") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->

            StudyRoute(
                backStackEntry = backStackEntry,

                // Энэ entry дээр хариулт бичих Learn горим ажиллана.
                studyMode = StudyMode.LEARN,

                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        // ───── КАРТУУД УДИРДАХ ─────

        composable(
            route = "ManageCard/{deckId}",
            arguments = listOf(
                navArgument("deckId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val deckId = backStackEntry.arguments
                ?.getString("deckId")

            ManageCardsRoute(
                backStackEntry = backStackEntry,

                onBackClick = {
                    navController.popBackStack()
                },

                onAddCardClick = {
                    if (!deckId.isNullOrBlank()) {

                        // cardId өгөөгүй тул хоосон form нээгдэнэ.
                        navController.navigate(
                            "CardEditor/$deckId"
                        )
                    }
                },

                onEditCardClick = { cardId ->
                    if (!deckId.isNullOrBlank()) {

                        // cardId өгсөн тул тухайн картын мэдээллээр бөглөнө.
                        navController.navigate(
                            "CardEditor/$deckId?cardId=$cardId"
                        )
                    }
                }
            )
        }

        // ───── КАРТ ҮҮСГЭХ / ЗАСАХ ─────

        composable(
            route = "CardEditor/{deckId}?cardId={cardId}",
            arguments = listOf(

                // Шинэ карт ч заавал нэг багцад хамаарна.
                navArgument("deckId") {
                    type = NavType.StringType
                },

                // Шинэ карт үүсгэхэд cardId байхгүй.
                navArgument("cardId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->

            CardFormRoute(
                backStackEntry = backStackEntry,

                onBackClick = {
                    navController.popBackStack()
                },

                onSaved = { _ ->
                    // Repository аль хэдийн хадгалсан.
                    // Өмнөх картын жагсаалт Room-оос шинэчлэгдэнэ.
                    navController.popBackStack()
                }
            )
        }
    }
}