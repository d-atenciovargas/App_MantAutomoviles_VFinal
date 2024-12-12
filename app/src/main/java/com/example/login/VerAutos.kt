package com.example.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class VerAutos : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_autos)

        // Encontrar el ImageView de regresar
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            // Redirigir al MenuAdministrador
            val intent = Intent(this, MenuAdministrador::class.java)
            startActivity(intent)
            finish() // Finalizar la actividad actual
        }

        // Encontrar el LinearLayout dentro del ScrollView
        val linearLayoutAutos = findViewById<LinearLayout>(R.id.linearLayoutAutos)

        // Obtener los autos desde UserManager
        val userManager = UserManager(this)
        val autos = userManager.getAllAutos()

        // Crear dinámicamente CardViews para cada auto
        for (auto in autos) {
            val cardView = CardView(this).apply {
                radius = 16f
                cardElevation = 8f
                useCompatPadding = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 16)
                }
            }

            val cardContent = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 16, 16, 16)
                }
            }

            // Título: Muestra el propietario del auto
            val textViewHeader = TextView(this).apply {
                text = "Auto de: ${auto["ownerName"]} ${auto["ownerLastname"]}"
                textSize = 20f
                setPadding(0, 0, 0, 8)
                setTextColor(resources.getColor(android.R.color.black, null))
                setTextAppearance(android.R.style.TextAppearance_Material_Medium)
            }

            // Detalles: Ocultos inicialmente
            val textViewDetails = TextView(this).apply {
                text = """
                    Marca: ${auto["marca"]}
                    Modelo: ${auto["modelo"]}
                    Año: ${auto["ano"]}
                    Versión: ${auto["version"]}
                    Transmisión: ${auto["transmision"]}
                    Kilometraje: ${auto["kilometraje"]} KM
                    Placa: ${auto["placa_patente"]}
                """.trimIndent()
                textSize = 16f
                setPadding(0, 0, 0, 16)
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                visibility = View.GONE // Oculto inicialmente
            }

            // Botón Eliminar
            val buttonEliminar = Button(this).apply {
                text = "ELIMINAR"
                setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, null))
                setTextColor(resources.getColor(android.R.color.white, null))
                visibility = View.GONE // Oculto inicialmente
                setOnClickListener { eliminarAuto(auto["id"]!!.toInt()) }
            }

            // Configurar el evento de clic en el encabezado para expandir/contraer
            textViewHeader.setOnClickListener {
                if (textViewDetails.visibility == View.GONE) {
                    textViewDetails.visibility = View.VISIBLE
                    buttonEliminar.visibility = View.VISIBLE
                } else {
                    textViewDetails.visibility = View.GONE
                    buttonEliminar.visibility = View.GONE
                }
            }

            // Agregar contenido a la tarjeta
            cardContent.addView(textViewHeader)
            cardContent.addView(textViewDetails)
            cardContent.addView(buttonEliminar)

            cardView.addView(cardContent)
            linearLayoutAutos.addView(cardView)
        }
    }

    private fun eliminarAuto(autoId: Int) {
        val userManager = UserManager(this)
        val eliminado = userManager.deleteAutoById(autoId)
        if (eliminado) {
            recreate() // Recargar la actividad para actualizar la lista de autos
        } else {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("No se pudo eliminar el auto.")
                .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}
