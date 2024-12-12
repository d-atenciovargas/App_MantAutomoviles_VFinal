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

class VerRespuestas : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_respuestas)

        // Encontrar el ImageView de regresar
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            // Redirigir al MenuAdministrador
            val intent = Intent(this, MenuAdministrador::class.java)
            startActivity(intent)
            finish() // Finalizar la actividad actual
        }

        // Encontrar el LinearLayout dentro del ScrollView
        val linearLayoutRespuestas = findViewById<LinearLayout>(R.id.linearLayoutRespuestas)

        // Obtener las respuestas desde UserManager
        val userManager = UserManager(this)
        val respuestas = userManager.getAllResponses()

        // Crear dinámicamente CardViews para cada respuesta
        for ((index, respuesta) in respuestas.withIndex()) {
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
                text = "Respuesta ${index + 1} de ${respuesta["name"]} ${respuesta["lastname"]}"
                textSize = 18f
                setPadding(8, 8, 8, 4)
                setTextColor(resources.getColor(android.R.color.black, null))
            }

            val textViewContenido = TextView(this).apply {
                text = "Contenido: ${respuesta["content"]}"
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }

            val buttonEliminar = Button(this).apply {
                text = "Eliminar"
                setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
                setTextColor(resources.getColor(android.R.color.white, null))
                setOnClickListener {
                    val responseId = (respuesta["response_id"] as? String)?.toIntOrNull()
                    if (responseId != null) {
                        eliminarRespuesta(responseId)
                    } else {
                        mostrarError("ID de la respuesta no válido.")
                    }
                }
            }

            // Agregar contenido a la tarjeta
            cardContent.addView(textViewTitulo)
            cardContent.addView(textViewContenido)
            cardContent.addView(buttonEliminar)

            cardView.addView(cardContent)
            linearLayoutRespuestas.addView(cardView)
        }
    }

    private fun eliminarRespuesta(responseId: Int) {
        val userManager = UserManager(this)
        val eliminado = userManager.deleteResponse(responseId)
        if (eliminado > 0) {
            recreate() // Recargar la actividad para actualizar la lista de respuestas
        } else {
            mostrarError("No se pudo eliminar la respuesta.")
        }
    }

    private fun mostrarError(mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
