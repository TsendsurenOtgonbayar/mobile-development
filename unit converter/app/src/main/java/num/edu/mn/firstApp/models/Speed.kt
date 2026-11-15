package num.edu.mn.firstApp.models

import num.edu.mn.firstApp.UnitConvertRule

class Speed : UnitConvertRule {
    val speedFactors = mapOf(
        "Метр/секунд" to 1.0,
        "Километр/цаг" to 0.2777777778,
        "Миль/цаг" to 0.44704,
        "Узел" to 0.514444,
        "Фут/секунд" to 0.3048,
        "Мах" to 340.29
    )
    val speedSymbols = mapOf(
        "Метр/секунд" to "м/с",
        "Километр/цаг" to "км/ц",
        "Миль/цаг" to "mph",
        "Узел" to "kn",
        "Фут/секунд" to "ft/s",
        "Мах" to "Mach"
    )

    override fun getUnits(): List<String> {
        return speedFactors.keys.toList()
    }

    override fun getSymbols(unit: String): String {
        return speedSymbols[unit]?:""
    }
    override fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        var fromSpeed=speedFactors.getValue(fromUnit)
        var toSpeed=speedFactors.getValue(toUnit)
        return value*fromSpeed/toSpeed
    }
}