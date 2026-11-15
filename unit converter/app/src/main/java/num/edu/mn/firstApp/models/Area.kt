package num.edu.mn.firstApp.models

import num.edu.mn.firstApp.UnitConvertRule

class Area : UnitConvertRule {
//    mongol nershileer nerelsen
    val toMeterSquer = mapOf(
        "Акр" to 4046.8564224,
        "Ар" to 100.0,
        "Гектар" to 10000.0,
        "Километр квадрат" to 1000000.0,
        "Сантиметр квадрат" to 0.0001,
        "Метр квадрат" to 1.0
    )
    val areaSymbol = mapOf(
        "Акр" to "acre",
        "Ар" to "a",
        "Гектар" to "га",
        "Километр квадрат" to "км²",
        "Сантиметр квадрат" to "см²",
        "Метр квадрат" to "м²"
    )

    override fun getUnits(): List<String> {
        return toMeterSquer.keys.toList()
    }

    override fun getSymbols(unit: String): String {
        return areaSymbol[unit]?:""
    }
    override fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        var fromArea = toMeterSquer.getValue(fromUnit)
        var toArea = toMeterSquer.getValue(toUnit)
        return value * fromArea/toArea
    }
}