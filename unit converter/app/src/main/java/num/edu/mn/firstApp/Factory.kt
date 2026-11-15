package num.edu.mn.firstApp
import num.edu.mn.firstApp.models.Length
import num.edu.mn.firstApp.models.Mass
import num.edu.mn.firstApp.models.Fuel
import num.edu.mn.firstApp.models.Time
import num.edu.mn.firstApp.models.Energy
import num.edu.mn.firstApp.models.Angle
import num.edu.mn.firstApp.models.Area
import num.edu.mn.firstApp.models.Density
import num.edu.mn.firstApp.models.Speed
import num.edu.mn.firstApp.models.Temperature
import num.edu.mn.firstApp.models.Volume
import num.edu.mn.firstApp.models.Weight
import num.edu.mn.firstApp.UnitConvertRule
object ConverterFactory {

    fun getConverter(category: String): UnitConvertRule {

        return when (category) {
            "Length" -> Length()
            "Mass" -> Mass()
            "Fuel" -> Fuel()
            "Time" -> Time()
            "Energy" -> Energy()
            "Angle" -> Angle()
            "Area" -> Area()
            "Density" -> Density()
            "Speed" -> Speed()
            "Temperature" -> Temperature()
            "Volume" -> Volume()
            "Weight" -> Weight()
            else -> throw IllegalArgumentException(
                "Unknown category: $category"
            )
        }
    }
}