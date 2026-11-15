package num.edu.mn.firstApp.models

import num.edu.mn.firstApp.UnitConvertRule

class Weight : UnitConvertRule {
    val forceFactors = mapOf(
        "Ньютон" to 1.0,
        "Килоньютон" to 1000.0,
        "Килограмм-хүч" to 9.80665,
        "Дина" to 0.00001,
        "Фунт-хүч" to 4.448222,
        "Меганьютон" to 1000000.0
    )
    val forceSymbols = mapOf(
        "Ньютон" to "Н",
        "Килоньютон" to "кН",
        "Килограмм-хүч" to "кгх",
        "Дина" to "dyn",
        "Фунт-хүч" to "lbf",
        "Меганьютон" to "МН"
    )

    override fun getUnits(): List<String> {
        return forceFactors.keys.toList()
    }

    override fun getSymbols(unit: String): String {
        return forceSymbols[unit]?:""
    }
    override fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        var fromForce=forceFactors.getValue(fromUnit)
        var toForce=forceFactors.getValue(toUnit)
        return value*fromForce/toForce
    }
}