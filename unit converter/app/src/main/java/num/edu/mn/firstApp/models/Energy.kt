package num.edu.mn.firstApp.models

import num.edu.mn.firstApp.UnitConvertRule

class Energy : UnitConvertRule {
    val energyFactors = mapOf(
        "Жоуль" to 1.0,
        "Киложоуль" to 1000.0,
        "Калори" to 4.184,
        "Килокалори" to 4184.0,
        "Ватт-цаг" to 3600.0,
        "Киловатт-цаг" to 3600000.0
    )
    val energySymbols = mapOf(
        "Жоуль" to "Ж",
        "Киложоуль" to "кЖ",
        "Калори" to "кал",
        "Килокалори" to "ккал",
        "Ватт-цаг" to "Вт·ц",
        "Киловатт-цаг" to "кВт·ц"
    )

    override fun getUnits(): List<String> {
        return energyFactors.keys.toList()
    }

    override fun getSymbols(unit: String): String {
        return energySymbols[unit]?:""
    }
    override fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        var fromEnergy=energyFactors.getValue(fromUnit)
        var toEnergy=energyFactors.getValue(toUnit)
        return value*fromEnergy/toEnergy
    }
}