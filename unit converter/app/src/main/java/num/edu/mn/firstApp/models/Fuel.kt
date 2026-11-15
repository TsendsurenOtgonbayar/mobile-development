package num.edu.mn.firstApp.models

import num.edu.mn.firstApp.UnitConvertRule

class Fuel : UnitConvertRule {

    val fuelToMoney = mapOf(
        "A92" to 2390.0,
        "A92 euro" to 2390.0,
        "A95" to 3910.0,
        "A95 euro" to 3910.0,
        "A80" to 2390.0,
        "Дизель түлш" to 3690.0
    )


    override fun getUnits(): List<String> {
        return fuelToMoney.keys.toList()
    }

    override fun getSymbols(unit: String): String {
        return "л"
    }
    override fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        val fromFuelValue=fuelToMoney.getValue(fromUnit)
        val toFuelValue=fuelToMoney.getValue(toUnit)
        return value*fromFuelValue/toFuelValue
    }
}