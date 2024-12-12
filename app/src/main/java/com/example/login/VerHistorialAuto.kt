package com.example.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class VerHistorialAuto : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_historial)

        // Encontrar el ImageView de regresar
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            // Redirigir al MenuAdministrador
            val intent = Intent(this, MenuAdministrador::class.java)
            startActivity(intent)
            finish() // Finalizar la actividad actual
        }

        val linearLayoutHistorial = findViewById<LinearLayout>(R.id.linearLayoutHistorial)
        val userManager = UserManager(this)
        val historial = userManager.getHistorialCompleto()

        for ((index, registro) in historial.withIndex()) {
            val cardView = CardView(this).apply {
                radius = 16f
                cardElevation = 8f
                useCompatPadding = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 16, 16, 16)
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

            val textViewTitulo = TextView(this).apply {
                text = "Historial ${index + 1}"
                textSize = 18f
                setPadding(8, 8, 8, 4)
            }

            val textViewDetalle = TextView(this).apply {
                text = """
                Modelo: ${registro["auto_modelo"]} - Marca: ${registro["auto_marca"]}
                Usuario: ${registro["user_name"]} ${registro["user_lastname"]}
            """.trimIndent()
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }

            val buttonEliminar = Button(this).apply {
                text = "Eliminar"
                setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
                setTextColor(resources.getColor(android.R.color.white, null))
                setOnClickListener { eliminarHistorial(registro["historial_id"]!!.toInt()) }
            }

            cardContent.addView(textViewTitulo)
            cardContent.addView(textViewDetalle)
            cardContent.addView(buttonEliminar)

            cardView.addView(cardContent)
            linearLayoutHistorial.addView(cardView)
        }
    }

    private fun eliminarHistorial(historialId: Int) {
        val userManager = UserManager(this)
        val eliminado = userManager.eliminarRegistroMantenimiento(historialId)
        if (eliminado) {
            recreate() // Recargar la actividad
        } else {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("No se pudo eliminar el historial.")
                .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}

