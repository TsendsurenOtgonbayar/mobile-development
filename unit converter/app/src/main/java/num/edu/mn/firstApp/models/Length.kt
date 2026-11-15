package num.edu.mn.firstApp.models

import num.edu.mn.firstApp.UnitConvertRule

class Length : UnitConvertRule {
    val toMeter = mapOf(
        "kilometer" to 1000.0,
        "meter" to 1.0,
        "centimeter" to 0.01,
        "millimeter" to 0.001,
        "inches" to 0.0254,
        "feet" to 0.3048,
    )
    val lengthSymbols = mapOf(
        "kilometer" to "km",
        "meter" to "m",
        "centimeter" to "cm",
        "millimeter" to "mm",
        "inches" to "in",
        "feet" to "ft",
    )

    override fun getUnits(): List<String> {
        return toMeter.keys.toList()
    }

    override fun getSymbols(unit: String): String {
        return lengthSymbols[unit]?:""
    }
    override fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        var FromFactor = toMeter.getValue(fromUnit)
        var ToFactor = toMeter.getValue(toUnit)
        return value*FromFactor /ToFactor

    }
}