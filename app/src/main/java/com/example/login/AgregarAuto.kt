package com.example.login

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AgregarAuto : AppCompatActivity() {

    private lateinit var dbAutos: BDAutos
    private lateinit var userManager: UserManager
    private lateinit var spinnerMarca: Spinner
    private lateinit var spinnerModelo: Spinner
    private lateinit var spinnerAno: Spinner
    private lateinit var spinnerVersion: Spinner
    private lateinit var spinnerTransmision: Spinner
    private lateinit var editTextKilometraje: EditText
    private lateinit var editTextPlacaPatente: EditText
    private lateinit var barraNotificacion: LinearLayout
    private lateinit var botonAgregar: LinearLayout

    private var userId: Int = -1 // ID del usuario logueado

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_auto)

        // Inicializar bases de datos
        dbAutos = BDAutos(this)
        userManager = UserManager(this)

        // Recuperar el ID del usuario logueado
        userId = intent.getIntExtra("user_id", -1)

        // Vincular vistas
        spinnerMarca = findViewById(R.id.spinner_marca_auto)
        spinnerModelo = findViewById(R.id.spinner_modelo_auto)
        spinnerAno = findViewById(R.id.spinner_ano_auto)
        spinnerVersion = findViewById(R.id.spinner_version_auto)
        spinnerTransmision = findViewById(R.id.spinner_transmision_auto)
        editTextKilometraje = findViewById(R.id.edittext_kilometraje)
        editTextPlacaPatente = findViewById(R.id.edittext_placa_patente)
        barraNotificacion = findViewById(R.id.barra_notificacion)
        botonAgregar = findViewById(R.id.barra_navegacion)

        // Configurar clic en la barra de notificación para regresar
        barraNotificacion.setOnClickListener {
            val intent = Intent(this, Menu::class.java)
            intent.putExtra("user_id", userId)
            startActivity(intent)
            finish()
        }

        // Configurar clic en el botón de agregar auto
        botonAgregar.setOnClickListener {
            agregarAuto()
        }

        // Configurar vistas iniciales
        editTextPlacaPatente.isEnabled = false
        setupSpinnerMarcas()
        setupKilometrajeFormatter()
        setupPlacaPatenteFormatter()

        editTextKilometraje.isEnabled = false
    }

    private fun agregarAuto() {
        // Validar los campos
        val marca = spinnerMarca.selectedItem?.toString() ?: ""
        val modelo = spinnerModelo.selectedItem?.toString() ?: ""
        val ano = spinnerAno.selectedItem?.toString()?.toIntOrNull()
        val version = spinnerVersion.selectedItem?.toString() ?: ""
        val transmision = spinnerTransmision.selectedItem?.toString() ?: ""
        val kilometraje = editTextKilometraje.text.toString().replace(".", "").toIntOrNull()
        val placaPatente = editTextPlacaPatente.text.toString()

        if (marca.isBlank() || modelo.isBlank() || ano == null || version.isBlank() ||
            transmision.isBlank() || kilometraje == null || placaPatente.isBlank()
        ) {
            Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }

        // Insertar el auto en la base de datos
        val db = userManager.writableDatabase
        val values = ContentValues().apply {
            put("user_id", userId)
            put("marca", marca)
            put("modelo", modelo)
            put("año", ano)
            put("version", version)
            put("transmision", transmision)
            put("kilometraje", kilometraje)
            put("placa_patente", placaPatente)
        }
        val result = db.insert("autos", null, values)

        if (result != -1L) {
            Toast.makeText(this, "Auto agregado correctamente.", Toast.LENGTH_SHORT).show()
            limpiarCampos()
        } else {
            Toast.makeText(this, "Error al agregar el auto.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun limpiarCampos() {
        spinnerMarca.setSelection(0)
        spinnerModelo.setSelection(0)
        spinnerAno.setSelection(0)
        spinnerVersion.setSelection(0)
        spinnerTransmision.setSelection(0)
        editTextKilometraje.text.clear()
        editTextPlacaPatente.text.clear()
        editTextPlacaPatente.isEnabled = false
    }

    private fun setupSpinnerMarcas() {
        val marcas = listOf(getString(R.string.selecciona_marca)) + dbAutos.obtenerMarcas()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, marcas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMarca.adapter = adapter

        spinnerMarca.setSelection(0)
        spinnerMarca.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position > 0) {
                    val selectedMarca = parent.getItemAtPosition(position) as String
                    cargarModelos(selectedMarca)
                } else {
                    reiniciarSpinners(spinnerModelo, spinnerAno, spinnerVersion, spinnerTransmision)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun cargarModelos(marca: String) {
        val idMarca = dbAutos.obtenerIdMarca(marca)
        val modelos = listOf(getString(R.string.selecciona_modelo)) + dbAutos.obtenerModelosPorMarca(idMarca)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModelo.adapter = adapter

        spinnerModelo.setSelection(0)
        spinnerModelo.isEnabled = true

        spinnerModelo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position > 0) {
                    val selectedModelo = parent.getItemAtPosition(position) as String
                    cargarAnios(selectedModelo, idMarca)
                } else {
                    reiniciarSpinners(spinnerAno, spinnerVersion, spinnerTransmision)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun cargarAnios(modelo: String, idMarca: Int) {
        val idModelo = dbAutos.obtenerIdModelo(modelo, idMarca)
        val anios = listOf(getString(R.string.selecciona_ano)) + dbAutos.obtenerAniosPorModelo(idModelo)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, anios)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAno.adapter = adapter

        spinnerAno.setSelection(0)
        spinnerAno.isEnabled = anios.size > 1

        spinnerAno.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position > 0) {
                    val selectedAnio = parent.getItemAtPosition(position) as Int
                    cargarVersiones(idModelo, selectedAnio)
                } else {
                    reiniciarSpinners(spinnerVersion, spinnerTransmision)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun cargarVersiones(modeloId: Int, anio: Int) {
        val versiones = listOf(getString(R.string.selecciona_version)) + dbAutos.obtenerVersionesPorModeloYAnio(modeloId, anio)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, versiones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVersion.adapter = adapter

        spinnerVersion.setSelection(0)
        spinnerVersion.isEnabled = versiones.size > 1

        spinnerVersion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position > 0) {
                    spinnerTransmision.isEnabled = true
                    cargarTransmisiones()
                } else {
                    spinnerTransmision.isEnabled = false
                    editTextKilometraje.isEnabled = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun cargarTransmisiones() {
        val transmisiones = listOf(getString(R.string.selecciona_transmision), "Transmisión manual", "Transmisión automática")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, transmisiones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTransmision.adapter = adapter

        spinnerTransmision.setSelection(0)

        spinnerTransmision.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                editTextKilometraje.isEnabled = position > 0
                if (position > 0 && editTextKilometraje.text.isNotEmpty()) {
                    editTextPlacaPatente.isEnabled = true // Habilitar placa si el kilometraje ya tiene valor
                } else {
                    editTextPlacaPatente.isEnabled = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                editTextKilometraje.isEnabled = false
                editTextPlacaPatente.isEnabled = false // Deshabilitar si no hay selección
            }
        }
    }

    private fun reiniciarSpinners(vararg spinners: Spinner) {
        for (spinner in spinners) {
            spinner.isEnabled = false
            spinner.setSelection(0)
        }
    }

    private fun setupKilometrajeFormatter() {
        editTextKilometraje.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() != current) {
                    val cleanString = s.toString().replace(".", "")
                    val parsed = cleanString.toLongOrNull() ?: 0

                    if (parsed <= 999999) {
                        current = formatNumber(parsed)
                        editTextKilometraje.setText(current)
                        editTextKilometraje.setSelection(current.length)

                        // Habilitar el EditText de la patente si el kilometraje tiene valor
                        editTextPlacaPatente.isEnabled = current.isNotEmpty()
                    } else {
                        editTextKilometraje.setText(current)
                        editTextKilometraje.setSelection(current.length)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun formatNumber(number: Long): String {
        return String.format("%,d", number).replace(",", ".")
    }

    private fun setupPlacaPatenteFormatter() {
        editTextPlacaPatente.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val cleanString = s.toString().replace("·", "").replace(" ", "").toUpperCase()
                val formatted = formatPlacaPatente(cleanString)

                if (formatted != s.toString()) {
                    editTextPlacaPatente.setText(formatted)
                    editTextPlacaPatente.setSelection(formatted.length)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun formatPlacaPatente(input: String): String {
        val validInput = input.filter { it.isLetter() || it.isDigit() }

        return when {
            validInput.length >= 6 && validInput.matches(Regex("[A-Z]{4}\\d{2}")) -> {
                "${validInput.substring(0, 2)}·${validInput.substring(2, 4)}·${validInput.substring(4, 6)}"
            }
            validInput.length >= 6 && validInput.matches(Regex("[A-Z]{2}\\d{4}")) -> {
                "${validInput.substring(0, 2)}·${validInput.substring(2, 4)}·${validInput.substring(4, 6)}"
            }
            validInput.length >= 4 -> {
                if (validInput.length > 4) {
                    "${validInput.substring(0, 2)}·${validInput.substring(2, 4)}·${validInput.substring(4).take(2)}"
                } else {
                    validInput
                }
            }
            validInput.length >= 2 -> {
                "${validInput.substring(0, 2)}·${validInput.substring(2).take(4)}"
            }
            else -> validInput
        }
    }
}
