package num.edu.mn.firstApp.models

import num.edu.mn.firstApp.UnitConvertRule

class Density : UnitConvertRule {
    val densityFactors = mapOf(
        "Килограмм/куб метр" to 1.0,
        "Грамм/куб сантиметр" to 1000.0,
        "Грамм/миллилитр" to 1000.0,
        "Килограмм/литр" to 1000.0,
        "Тонн/куб метр" to 1000.0,
        "Фунт/куб фут" to 16.018463
    )
    val densitySymbols = mapOf(
        "Килограмм/куб метр" to "кг/м³",
        "Грамм/куб сантиметр" to "г/см³",
        "Грамм/миллилитр" to "г/мл",
        "Килограмм/литр" to "кг/л",
        "Тонн/куб метр" to "т/м³",
        "Фунт/куб фут" to "lb/ft³"
    )

    override fun getUnits(): List<String> {
        return  densityFactors.keys.toList()
    }

    override fun getSymbols(unit: String): String {
        return densitySymbols[unit]?:""
    }
    override fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        var fromDensity=densityFactors.getValue(fromUnit)
        var toDensity=densityFactors.getValue(toUnit)
        return value*fromDensity/toDensity
    }
}