package num.edu.mn.firstApp.models

import num.edu.mn.firstApp.UnitConvertRule

class Time : UnitConvertRule {
    val timeFactors = mapOf(
        "Миллисекунд" to 0.001,
        "Секунд" to 1.0,
        "Минут" to 60.0,
        "Цаг" to 3600.0,
        "Хоног" to 86400.0,
        "Жил" to 31536000.0
    )
    val timeSymbols = mapOf(
        "Миллисекунд" to "мс",
        "Секунд" to "с",
        "Минут" to "мин",
        "Цаг" to "ц",
        "Хоног" to "өдөр",
        "Жил" to "жил"
    )

    override fun getUnits(): List<String> {
        return timeFactors.keys.toList()
    }

    override fun getSymbols(unit: String): String {
        return timeSymbols[unit]?:""
    }
    override fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        var fromTime=timeFactors.getValue(fromUnit)
        var toUnit =timeFactors.getValue(toUnit)
        return value*fromTime/toUnit
    }
}