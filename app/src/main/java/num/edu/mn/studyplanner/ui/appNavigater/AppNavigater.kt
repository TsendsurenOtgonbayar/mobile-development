package num.edu.mn.studyplanner.ui.appNavigater

import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
@Composable
fun NavigateRouter(){
    val navController= rememberNavController()
    NavHost(navController=navController, startDestination = "first"){
        composable("first"){

        }
    }
}