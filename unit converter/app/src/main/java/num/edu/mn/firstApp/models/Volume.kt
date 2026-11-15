package num.edu.mn.firstApp.models

import num.edu.mn.firstApp.UnitConvertRule

class Volume : UnitConvertRule {
    val toLiter = mapOf(
        "миллилитр" to 0.001,
        "литр" to 1.0,
        "сантиметр куб" to 0.001,
        "метр куб" to 1000.0,
        "галлон" to 3.785411784,
        "инч куб" to 0.016387064
    )
    val volumeSymbols=mapOf(
        "миллилитр" to "мл",
        "литр" to "л",
        "сантиметр куб" to "см",
        "метр куб" to "м³",
        "галлон" to "галлон",
        "инч куб" to "инч³"
    )

    override fun getUnits(): List<String> {
        return toLiter.keys.toList()
    }

    override fun getSymbols(unit: String): String {
        return volumeSymbols[unit]?:""
    }
    override fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        var fromVolume = toLiter.getValue(fromUnit)
        var toVolume =toLiter.getValue(toUnit)
        return value* fromVolume/toVolume
    }
}