package num.edu.mn.firstApp.models

import num.edu.mn.firstApp.UnitConvertRule

class Mass : UnitConvertRule {
    val massFactors = mapOf(
        "Миллиграмм" to 0.000001,
        "Грамм" to 0.001,
        "Килограмм" to 1.0,
        "Центнер" to 100.0,
        "Тонн" to 1000.0,
        "Унци" to 0.0283495
    )
    val massSymbols = mapOf(
        "Миллиграмм" to "мг",
        "Грамм" to "г",
        "Килограмм" to "кг",
        "Центнер" to "ц",
        "Тонн" to "т",
        "Унци" to "oz"
    )

    override fun getUnits(): List<String> {
        return massFactors.keys.toList()
    }

    override fun getSymbols(unit: String): String {
        return massSymbols[unit]?:""
    }
    override fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        var fromMass=massFactors.getValue(fromUnit)
        var toMass=massFactors.getValue(toUnit)
        return value*fromMass/toMass
    }
}