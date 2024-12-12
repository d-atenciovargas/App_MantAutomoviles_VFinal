package com.example.login

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.util.Random

class SimuladorOBD : AppCompatActivity() {

    private var rpmValue: TextView? = null
    private var voltageValue: TextView? = null
    private var temperatureValue: TextView? = null
    private var errorCodesContainer: LinearLayout? = null
    private lateinit var startButton: Button
    private lateinit var clearButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var userManager: UserManager

    private var handler: Handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var codigosBD: CodigosBD? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simulador)

        // Inicializar vistas
        rpmValue = findViewById(R.id.rpm_value)
        voltageValue = findViewById(R.id.voltage_value)
        temperatureValue = findViewById(R.id.temperature_value)
        errorCodesContainer = findViewById(R.id.error_codes_container)
        startButton = findViewById(R.id.start_button)

        // Modificar diseño del botón "INICIAR" programáticamente
        startButton.apply {
            background = getDrawable(R.drawable.styled_button_background) // Asignar drawable
            setTextColor(Color.WHITE) // Configurar color del texto
            elevation = 8f // Añadir elevación
        }

        // Crear el botón "Borrar Códigos" con diseño redondeado
        clearButton = Button(this).apply {
            text = "Borrar Códigos"
            visibility = View.GONE
            background = getDrawable(R.drawable.styled_button_background2) // Fondo redondeado rojo
            setTextColor(Color.WHITE)
            textSize = 16f
            elevation = 8f
        }

        // Agregar el botón "Borrar Códigos" al contenedor
        val buttonsContainer = findViewById<LinearLayout>(R.id.buttons_container)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(16, 0, 16, 0) // Espaciado entre botones
        }
        clearButton.layoutParams = layoutParams
        buttonsContainer.addView(clearButton)

        // Inicializar UserManager
        userManager = UserManager(this)

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("AutoPreferences", MODE_PRIVATE)

        // Mostrar información del auto seleccionado
        mostrarAutoSeleccionado()

        // Configurar base de datos
        codigosBD = CodigosBD(this)
        try {
            codigosBD?.createDatabase()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Configurar eventos del botón de inicio
        configurarBotonIniciar()

        // Configurar botón de retroceso
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Regresar a la actividad anterior
        }

        configurarBotonBorrarErrores() // <--- Asegúrate de llamar a esta función
    }


    private fun mostrarAutoSeleccionado() {
        val selectedAutoId = sharedPreferences.getInt("selected_auto_id", -1)
        if (selectedAutoId != -1) {
            val auto = userManager.getAutoById(selectedAutoId)
            auto?.let {
                val autoInfoContainer = findViewById<LinearLayout>(R.id.auto_info_container)
                autoInfoContainer.removeAllViews()

                val cardView = CardView(this).apply {
                    radius = 16f
                    cardElevation = 8f
                    setCardBackgroundColor(Color.parseColor("#2E2E44"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(16, 8, 16, 8)
                    }
                }

                val contentLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(12, 8, 12, 8)
                }

                val titleView = TextView(this).apply {
                    text = "${it["marca"]} ${it["modelo"]}"
                    textSize = 24f
                    setTextColor(Color.WHITE)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 8, 0, 16)
                }
                contentLayout.addView(titleView)

                val details = listOf(
                    "Año: ${it["ano"]}",
                    "Versión: ${it["version"]}",
                    "Transmisión: ${it["transmision"]}",
                    "Kilometraje: ${it["kilometraje"]} km",
                    "Placa/Patente: ${it["placa_patente"]}"
                )

                details.forEach { detail ->
                    val detailView = TextView(this).apply {
                        text = detail
                        textSize = 18f
                        setTextColor(Color.LTGRAY)
                        setPadding(0, 4, 0, 4)
                    }
                    contentLayout.addView(detailView)
                }

                cardView.addView(contentLayout)
                autoInfoContainer.addView(cardView)
            }
        }
    }

    private fun configurarBotonIniciar() {
        val selectedAutoId = sharedPreferences.getInt("selected_auto_id", -1)
        if (selectedAutoId == -1) {
            startButton.isEnabled = true
            startButton.setOnClickListener {
                Toast.makeText(this, "Debe seleccionar un auto", Toast.LENGTH_SHORT).show()
            }
        } else {
            startButton.isEnabled = true
            startButton.setOnClickListener {
                if (!isRunning) {
                    isRunning = true
                    iniciarSimulacion()
                }
            }
        }
    }

    private fun iniciarSimulacion() {
        mostrarAnimacionEscaneo()

        // Iniciar la actualización de los indicadores dinámicamente
        isRunning = true
        handler.post(object : Runnable {
            override fun run() {
                if (isRunning) {
                    val random = Random()

                    // Generar valores realistas para los indicadores
                    val simulatedRPM = random.nextInt(4000) + 800 // RPM entre 800 y 4800
                    val simulatedVoltage = 12.6 + random.nextDouble() * 2.1 // Voltaje entre 12.6V y 14.7V
                    val simulatedTemperature = random.nextInt(21) + 85 // Temperatura entre 85°C y 105°C

                    // Actualizar los valores de los TextViews
                    rpmValue?.text = "$simulatedRPM RPM"
                    voltageValue?.text = String.format("%.1f V", simulatedVoltage)
                    temperatureValue?.text = "$simulatedTemperature°C"

                    // Programar la próxima actualización
                    handler.postDelayed(this, 1000) // Actualizar cada 1 segundo
                }
            }
        })

        // Simulación del escaneo (5 segundos)
        val random = Random()
        handler.postDelayed({
            if (isRunning) {
                isRunning = false // Detener la simulación tras completar el escaneo

                // Detener la actualización de indicadores
                handler.removeCallbacksAndMessages(null)

                // Simular posibilidad de no encontrar códigos de error (e.g., 30% de probabilidad)
                val noErrorDetected = random.nextFloat() < 0.3
                val detectedErrors = if (noErrorDetected) {
                    listOf(Pair("No Code", "No se detectaron códigos de error"))
                } else {
                    obtenerCodigosError(random)
                }

                mostrarCodigosError(detectedErrors)
            }
        }, 7000) // Simulación durante 7 segundos
    }



    private fun mostrarAnimacionEscaneo() {
        errorCodesContainer?.removeAllViews()
        val animacionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }

        val progressBar = ProgressBar(this).apply {
            isIndeterminate = true
        }

        val loadingText = TextView(this).apply {
            text = "Escaneando..."
            textSize = 24f
            setTextColor(Color.YELLOW)
            setPadding(16, 0, 0, 0)
        }

        animacionLayout.addView(progressBar)
        animacionLayout.addView(loadingText)
        errorCodesContainer?.addView(animacionLayout)
    }

    private fun obtenerCodigosError(random: Random): List<Pair<String, String>> {
        val errorList = mutableListOf<Pair<String, String>>()
        val db = codigosBD?.readableDatabase
        val cursor = db?.rawQuery("SELECT codigo, descripcion FROM codigos_dtc_obd2", null)

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val code = it.getString(0)
                    val description = it.getString(1)
                    errorList.add(Pair(code, description))
                } while (it.moveToNext())
            }
        }

        // Ajustar probabilidad de no encontrar errores
        val probabilidadSinErrores = 60 // 60% de probabilidad de no detectar códigos
        if (random.nextInt(100) < probabilidadSinErrores) {
            return listOf(Pair("No Code", "No se detectaron códigos de error"))
        }

        // Si hay errores en la base de datos, seleccionar algunos aleatoriamente
        return if (errorList.isNotEmpty()) {
            errorList.shuffled().take(random.nextInt(3) + 1) // Seleccionar entre 1 y 3 códigos aleatorios
        } else {
            listOf(Pair("No Code", "No se detectaron códigos de error"))
        }
    }


    private fun mostrarCodigosError(codigos: List<Pair<String, String>>) {
        errorCodesContainer?.removeAllViews()

        // Verificar si la lista de códigos contiene solo "No Code"
        if (codigos.size == 1 && codigos[0].first == "No Code") {
            // No se encontraron códigos de error
            clearButton.visibility = View.GONE

            val noErrorCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundResource(R.drawable.card_background) // Reutilizar estilo de las tarjetas
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 8, 8, 8) // Margen alrededor de la tarjeta
                }
                layoutParams = params
                elevation = 8f
            }

            val noErrorTitle = TextView(this).apply {
                text = "¡Sin errores detectados!"
                textSize = 20f
                setTextColor(Color.parseColor("#00E676")) // Verde para éxito
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 8)
            }

            val noErrorDescription = TextView(this).apply {
                text = "No se detectaron códigos de error en el auto. Todo está funcionando correctamente."
                textSize = 16f
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 8)
            }

            noErrorCard.addView(noErrorTitle)
            noErrorCard.addView(noErrorDescription)
            errorCodesContainer?.addView(noErrorCard)

            Toast.makeText(this, "No se encontraron códigos de error", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar códigos de error detectados
        for ((codigo, descripcion) in codigos) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundResource(R.drawable.card_background)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(8, 8, 8, 8)
                layoutParams = params
                elevation = 8f
            }

            val codigoView = TextView(this).apply {
                text = codigo
                setTextColor(Color.RED)
                textSize = 20f
                setPadding(0, 0, 0, 8)
            }

            val estadoView = TextView(this).apply {
                text = "Estado: Código Almacenado"
                setTextColor(Color.WHITE)
                textSize = 17f
            }

            val descripcionView = TextView(this).apply {
                text = "Descripción: $descripcion"
                setTextColor(Color.LTGRAY)
                textSize = 17f
                setPadding(0, 0, 0, 8)
            }

            val btnReparacion = Button(this).apply {
                text = "GUÍA DE REPARACIÓN"
                setTextColor(Color.WHITE)
                textSize = 16f
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.button_rounded_background)
                setPadding(16, 16, 16, 16)
                setOnClickListener {
                    val intent = Intent(this@SimuladorOBD, DetallesCodigos::class.java)
                    intent.putExtra("codigo", codigo)
                    startActivity(intent)
                }
            }

            card.addView(codigoView)
            card.addView(estadoView)
            card.addView(descripcionView)
            card.addView(btnReparacion)
            errorCodesContainer?.addView(card)
        }

        // Mostrar el botón "BORRAR CÓDIGOS" si se encontraron errores
        clearButton.visibility = View.VISIBLE
    }

    private fun configurarBotonBorrarErrores() {
        clearButton.setOnClickListener {
            // Crear un diálogo personalizado
            val dialog = android.app.AlertDialog.Builder(this).create()

            val dialogLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 32, 32, 32)
                setBackgroundColor(Color.parseColor("#2E2E44"))
                gravity = Gravity.CENTER
            }

            val title = TextView(this).apply {
                text = "¿Estás seguro?"
                textSize = 20f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }

            val message = TextView(this).apply {
                text = "Esto borrará todos los códigos almacenados del auto."
                textSize = 16f
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 32)
            }

            val buttonContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val confirmButton = Button(this).apply {
                text = "SÍ, SEGURO"
                background = getDrawable(R.drawable.styled_button_background) // Botón redondeado azul
                setTextColor(Color.WHITE)
                textSize = 16f
                elevation = 8f
                setPadding(16, 16, 16, 16)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 0, 16, 0) // Separación horizontal
                }
                this.layoutParams = layoutParams
                setOnClickListener {
                    // Acción de borrar códigos
                    errorCodesContainer?.removeAllViews()
                    clearButton.visibility = View.GONE
                    Toast.makeText(
                        this@SimuladorOBD,
                        "Códigos borrados de la computadora del auto",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss() // Cerrar el diálogo
                }
            }

            val cancelButton = Button(this).apply {
                text = "CANCELAR"
                background = getDrawable(R.drawable.styled_button_background2) // Botón redondeado rojo
                setTextColor(Color.WHITE)
                textSize = 16f
                elevation = 8f
                setPadding(16, 16, 16, 16)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 0, 16, 0) // Separación horizontal
                }
                this.layoutParams = layoutParams
                setOnClickListener {
                    dialog.dismiss() // Cerrar el diálogo
                }
            }

            // Añadir botones al contenedor
            buttonContainer.addView(confirmButton)
            buttonContainer.addView(cancelButton)

            // Añadir vistas al diseño del diálogo
            dialogLayout.addView(title)
            dialogLayout.addView(message)
            dialogLayout.addView(buttonContainer)

            // Configurar y mostrar el diálogo
            dialog.setView(dialogLayout)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Fondo transparente
            dialog.show()
        }
    }
}