package num.edu.mn.firstApp

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import num.edu.mn.firstApp.ConverterFactory
import java.text.DecimalFormat
class ThirdActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_third)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val category= intent.getStringExtra("CATEGORY")?:"Length"
        val backBtn= findViewById<ImageButton>(R.id.backBtn)
        val title=findViewById<TextView>(R.id.unitName)
        val fromSpinner=findViewById<Spinner>(R.id.fromSpinner)
        val toSpinner=findViewById<Spinner>(R.id.toSpinner)
        val fromValue=findViewById<EditText>(R.id.fromValue)
        val toValue=findViewById<EditText>(R.id.toValue)
        val switchBtn=findViewById<ImageButton>(R.id.switchBtn)

        title.text=category

        val converter= ConverterFactory.getConverter(category)
        val units=converter.getUnits()

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            units
        )
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        fromSpinner.adapter=adapter
        toSpinner.adapter=adapter
        fromSpinner.setSelection(0)
        toSpinner.setSelection(1)

        fromValue.addTextChangedListener{text ->
            val value= text.toString().toDoubleOrNull()
            if (value!=null){
                value.toDouble()
                if (isValidValue(value,category)){
                    val fromUnit=fromSpinner.selectedItem.toString()
                    val toUnit=toSpinner.selectedItem.toString()
                    val result= converter.convert(value,fromUnit,toUnit)
                    val formatter= DecimalFormat("0.######")
                    val formattedResult =formatter.format(result)
                    val fromSymbol=converter.getSymbols(fromUnit)
                    val symbol=converter.getSymbols(toUnit)
                    toValue.setText("$formattedResult $symbol")
                }else toValue.setText("Error")
            }else toValue.setText("")
        }
        val spinnerList= listOf(
            fromSpinner,toSpinner
        )
        spinnerList.forEach { spinner ->
            spinner.onItemSelectedListener= object : AdapterView.OnItemSelectedListener{
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val value= fromValue.text.toString().toDoubleOrNull()
                    if (value!=null){
                        value.toDouble()
                        if (isValidValue(value,category)) {
                            val fromUnit = fromSpinner.selectedItem.toString()
                            val toUnit = toSpinner.selectedItem.toString()
                            val result = converter.convert(value, fromUnit, toUnit)
                            val formatter = DecimalFormat("0.#######")
                            val formattedResult = formatter.format(result)
                            val symbol =converter.getSymbols(toUnit)
                            toValue.setText("$formattedResult $symbol")
                        } else toValue.setText("Error")
                    }else toValue.setText("")
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
        switchBtn.setOnClickListener {
            val currentFrom = fromSpinner.selectedItem.toString()
            val currentTo = toSpinner.selectedItem.toString()
            fromSpinner.setSelection(units.indexOf(currentTo))
            toSpinner.setSelection(units.indexOf(currentFrom))
            val value= fromValue.text.toString().toDoubleOrNull()
            if (value!=null) {
                if (isValidValue(value,category)) {
                    val fromUnit = fromSpinner.selectedItem.toString()
                    val toUnit = toSpinner.selectedItem.toString()
                    val result = converter.convert(value, fromUnit, toUnit)
                    val formatter = DecimalFormat("0.#######")
                    val formattedResult = formatter.format(result)
                    val symbol=converter.getSymbols(toUnit)
                    toValue.setText("$formattedResult $symbol")
                }else toValue.setText("Error")
            }
            else toValue.setText("")
        }
        backBtn.setOnClickListener {
            finish()
        }
    }
    fun isValidValue(value: Double, category: String): Boolean {
        if (category == "Temperature" || category == "Angle" || category == "Force") {
            return true
        }
        return value >= 0
    }
}
