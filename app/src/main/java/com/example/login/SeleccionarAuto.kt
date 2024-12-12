package com.example.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class SeleccionarAuto : AppCompatActivity() {

    private lateinit var userManager: UserManager
    private lateinit var autosContainer: LinearLayout
    private lateinit var sharedPreferences: SharedPreferences
    private var userId: Int = -1
    private var selectedAutoId: Int? = null // Variable temporal para la selección actual
    private var savedAutoId: Int? = null // ID del auto guardado en SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccionar_auto)

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("AutoPreferences", MODE_PRIVATE)
        savedAutoId = sharedPreferences.getInt("selected_auto_id", -1).takeIf { it != -1 }
        selectedAutoId = savedAutoId // Configurar el auto seleccionado al iniciar

        // Obtener el ID del usuario desde el Intent
        userId = intent.getIntExtra("user_id", -1)

        // Inicializar vistas y UserManager
        autosContainer = findViewById(R.id.autos_container)
        userManager = UserManager(this)

        // Configurar los eventos de los botones en la barra de navegación
        configurarBarraNavegacion()

        // Cargar autos del usuario
        if (userId != -1) {
            cargarAutos(userId)
        } else {
            mostrarMensajeError()
        }
    }

    private fun cargarAutos(userId: Int) {
        val autos = userManager.getAutosByUserId(userId)
        autosContainer.removeAllViews()

        if (autos.isNotEmpty()) {
            for (auto in autos) {
                val cardView = crearCardAuto(auto)
                autosContainer.addView(cardView)
            }
        } else {
            val noAutosView = TextView(this).apply {
                text = "No tienes autos registrados."
                textSize = 18f
                setPadding(16, 16, 16, 16)
            }
            autosContainer.addView(noAutosView)
        }
    }

    private fun crearCardAuto(auto: Map<String, Any>): View {
        val autoId = auto["id"] as Int

        val cardView = CardView(this).apply {
            radius = 16f
            cardElevation = 8f
            setContentPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val marcaModeloView = TextView(this).apply {
            text = "${auto["marca"]} ${auto["modelo"]}"
            textSize = 21f
            setTextColor(ContextCompat.getColor(this@SeleccionarAuto, R.color.primary2))
            setPadding(0, 8, 0, 8)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val anoView = TextView(this).apply {
            text = "Año: ${auto["ano"]}"
            textSize = 17f
        }

        val versionView = TextView(this).apply {
            text = "Versión: ${auto["version"]}"
            textSize = 17f
        }

        val transmisionView = TextView(this).apply {
            text = "Transmisión: ${auto["transmision"]}"
            textSize = 17f
        }

        val kilometrajeView = TextView(this).apply {
            text = "Kilometraje: ${auto["kilometraje"]} km"
            textSize = 17f
        }

        val placaView = TextView(this).apply {
            text = "Placa/Patente: ${auto["placa_patente"]}"
            textSize = 17f
        }

        val selectButton = Button(this).apply {
            text = if (autoId == selectedAutoId) "Seleccionado" else "Seleccionar"
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@SeleccionarAuto, android.R.color.white))
            background = ContextCompat.getDrawable(
                this@SeleccionarAuto,
                if (autoId == selectedAutoId) R.drawable.styled_button_selected else R.drawable.styled_button_background
            )
            setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }

            setOnClickListener {
                if (selectedAutoId == autoId) {
                    selectedAutoId = null
                } else {
                    selectedAutoId = autoId
                }
                actualizarEstadoBotones()
            }
        }

        val deleteButton = Button(this).apply {
            text = "Eliminar"
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@SeleccionarAuto, android.R.color.white))
            background = ContextCompat.getDrawable(
                this@SeleccionarAuto,
                R.drawable.styled_button_background2
            )
            setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }

            setOnClickListener {
                eliminarAuto(autoId)
            }
        }

        contentLayout.addView(marcaModeloView)
        contentLayout.addView(anoView)
        contentLayout.addView(versionView)
        contentLayout.addView(transmisionView)
        contentLayout.addView(kilometrajeView)
        contentLayout.addView(placaView)
        contentLayout.addView(selectButton)
        contentLayout.addView(deleteButton)

        cardView.addView(contentLayout)
        cardView.tag = autoId
        return cardView
    }

    private fun eliminarAuto(autoId: Int) {
        val isDeleted = userManager.deleteAutoById(autoId)
        if (isDeleted) {
            Toast.makeText(this, "Auto eliminado correctamente", Toast.LENGTH_SHORT).show()
            cargarAutos(userId)
        } else {
            Toast.makeText(this, "Error al eliminar el auto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarEstadoBotones() {
        for (i in 0 until autosContainer.childCount) {
            val cardView = autosContainer.getChildAt(i) as CardView
            val autoId = cardView.tag as Int
            val selectButton = (cardView.getChildAt(0) as LinearLayout).getChildAt(6) as Button

            selectButton.text = if (autoId == selectedAutoId) "Seleccionado" else "Seleccionar"
            selectButton.background = ContextCompat.getDrawable(
                this,
                if (autoId == selectedAutoId) R.drawable.styled_button_selected else R.drawable.styled_button_background
            )
        }
    }

    private fun configurarBarraNavegacion() {
        findViewById<LinearLayout>(R.id.aceptar_seleccion).setOnClickListener {
            sharedPreferences.edit()
                .putInt("selected_auto_id", selectedAutoId ?: -1)
                .apply()

            Toast.makeText(
                this,
                if (selectedAutoId != null) "Auto seleccionado correctamente" else "Ningún auto seleccionado",
                Toast.LENGTH_SHORT
            ).show()

            startActivity(Intent(this, Menu::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.regresar_menu).setOnClickListener {
            finish()
        }
    }

    private fun mostrarMensajeError() {
        val errorView = TextView(this).apply {
            text = "Ocurrió un error al cargar los autos."
            textSize = 18f
            setPadding(16, 16, 16, 16)
        }
        autosContainer.addView(errorView)
    }
}
