package num.edu.mn.firstApp.models

import android.R
import num.edu.mn.firstApp.UnitConvertRule

class Angle : UnitConvertRule {
    val angleFactors = mapOf(
        "Градус" to 1.0,
        "Радиан" to 57.295779513,
        "Градиан" to 0.9,
        "Минут" to 0.0166666667,
        "Секунд" to 0.0002777778,
        "Бүтэн эргэлт" to 360.0
    )
    val angleSymbols = mapOf(
        "Градус" to "°",
        "Радиан" to "рад",
        "Градиан" to "grad",
        "Минут" to "'",
        "Секунд" to "\"",
        "Бүтэн эргэлт" to "rev"
    )

    override fun getUnits(): List<String> {
        return angleFactors.keys.toList()
    }

    override fun getSymbols(unit: String): String {
        return angleSymbols[unit]?:""
    }
    override fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        var fromAngle=angleFactors.getValue(fromUnit)
        var toAngle=angleFactors.getValue(toUnit)
        return value*fromAngle/toAngle
    }

}
