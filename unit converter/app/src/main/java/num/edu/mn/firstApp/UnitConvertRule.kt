package num.edu.mn.firstApp

import android.R

interface UnitConvertRule {
    fun getUnits(): List<String>
    fun getSymbols(unit: String):String
    fun convert(value: Double, fromUnit: String, toUnit: String): Double

}