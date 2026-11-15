package num.edu.mn.firstApp

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.ImageButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_second)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val lengthCard = findViewById<LinearLayout>(R.id.length)
        val areaCard = findViewById<LinearLayout>(R.id.area)
        val volumeCard = findViewById<LinearLayout>(R.id.volume)
        val massCard = findViewById<LinearLayout>(R.id.mass)
        val timeCard = findViewById<LinearLayout>(R.id.Time)
        val speedCard = findViewById<LinearLayout>(R.id.speed)
        val temperatureCard = findViewById<LinearLayout>(R.id.temperature)
        val densityCard = findViewById<LinearLayout>(R.id.density)
        val energyCard = findViewById<LinearLayout>(R.id.energy)
        val angleCard = findViewById<LinearLayout>(R.id.angle)
        val weightCard = findViewById<LinearLayout>(R.id.weigth)
        val fuelCard = findViewById<LinearLayout>(R.id.fuel)

        lengthCard.setOnClickListener { openConverter("Length") }
        areaCard.setOnClickListener { openConverter("Area") }
        volumeCard.setOnClickListener { openConverter("Volume") }
        massCard.setOnClickListener { openConverter("Mass") }
        timeCard.setOnClickListener { openConverter("Time") }
        speedCard.setOnClickListener { openConverter("Speed") }
        temperatureCard.setOnClickListener { openConverter("Temperature") }
        densityCard.setOnClickListener { openConverter("Density") }
        energyCard.setOnClickListener { openConverter("Energy") }
        angleCard.setOnClickListener { openConverter("Angle") }
        weightCard.setOnClickListener { openConverter("Weight") }
        fuelCard.setOnClickListener { openConverter("Fuel") }

        val categories=mapOf(
            "Length" to lengthCard,
            "Area" to areaCard,
            "Volume" to volumeCard,
            "Mass" to massCard,
            "Time" to timeCard,
            "Speed" to speedCard,
            "Temperature" to temperatureCard,
            "Density" to densityCard,
            "Energy" to energyCard,
            "Angle" to angleCard,
            "Weight" to weightCard,
            "Fuel" to fuelCard
        )
        val searchInput =findViewById<EditText>(R.id.searchInput)
        searchInput.addTextChangedListener{ text->
            var keyLetter =text.toString()
            categories.forEach { (name, card) ->
                if (name.contains(keyLetter, ignoreCase = true)) {
                    card.visibility = View.VISIBLE
                } else {
                    card.visibility = View.GONE
                }
            }
        }

    }
    private fun openConverter(category: String) {
        val intent = Intent(this, ThirdActivity::class.java)
        intent.putExtra("CATEGORY", category)
        startActivity(intent)
    }

}