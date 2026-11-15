package num.edu.mn.studyplanner.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.w3c.dom.Text

@Composable
fun StudentPlannerScreen(){
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text="Student Planner",
                        fontSize=35.sp,
                        fontWeight= FontWeight.Bold,
                        overflow= TextOverflow.Ellipsis

                    )

                },
                modifier = Modifier.padding(10.dp)
            )
        }
    ) {

    }
}