package num.edu.mn.firstApp.models

import num.edu.mn.firstApp.UnitConvertRule

class Temperature : UnitConvertRule {
    val temdegt = mapOf(
        "цельс" to "°C",
        "фарангейт" to "°F",
        "келвин" to "°K"
    )

    override fun getUnits(): List<String> {
        return temdegt.keys.toList()
    }

    override fun getSymbols(unit: String): String {
        return temdegt[unit]?:""
    }
    override fun convert(value: Double, fromUnit: String, toUnit: String): Double {
        if (fromUnit=="цельс" && toUnit=="келвин"){
            return value+273.15
        }
        else if (fromUnit=="цельс" && toUnit=="фарангейт"){
            return value*1.8+32
        }
        else if (fromUnit==toUnit){
            return value
        }
        else if (fromUnit=="келвин" && toUnit=="цельс"){
            return value-273.15
        }
        else if (fromUnit=="келвин" && toUnit=="фарангейт"){
            return (value+459.67)*5/9
        }
        else if (fromUnit=="фарангейт" && toUnit=="цельс"){
            return (value-32)*5/9
        }
        else if (fromUnit=="фарангейт" && toUnit=="келвин"){
            return value*1.8-459.67
        }
        else return value
    }
}