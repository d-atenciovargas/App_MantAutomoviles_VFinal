package com.example.login

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class HistorialMantenimiento : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_mantenimiento)

        // Configurar el botón de retroceso
        val btnBackHistorial = findViewById<ImageView>(R.id.btnBackHistorial)
        btnBackHistorial.setOnClickListener {
            finish() // Regresa a la actividad anterior
        }

        // Obtener el contenedor principal
        val contentContainer = findViewById<LinearLayout>(R.id.contentContainer)
        val placeholderHistorial = findViewById<TextView>(R.id.placeholderHistorial)

        // Intentar cargar el historial desde la base de datos
        val sharedPreferences = getSharedPreferences("AutoPreferences", MODE_PRIVATE)
        val selectedAutoId = sharedPreferences.getInt("selected_auto_id", -1)
        val userManager = UserManager(this)

        if (selectedAutoId != -1) {
            val historial = userManager.obtenerHistorialPorAutoId(selectedAutoId)

            if (historial.isNotEmpty()) {
                placeholderHistorial.visibility = View.GONE // Ocultar el placeholder si hay datos

                // Mostrar cada entrada del historial en tarjetas
                historial.forEach { registro ->
                    val registroId = registro["id"]?.toInt() ?: -1
                    val datosMantenimiento = registro["datos_mantenimiento"] ?: "Información no disponible"
                    val formattedContent = formatContent(datosMantenimiento)
                    addExpandableCard(contentContainer, "Registro de Mantenimiento", formattedContent, registroId, userManager)
                }
            } else {
                placeholderHistorial.text = "No hay historial de mantenimiento disponible para este auto."
            }
        } else {
            placeholderHistorial.text = "No se ha seleccionado un auto para mostrar el historial."
        }
    }

    // Método para agregar una tarjeta expandible con título, contenido y botón de eliminar
    private fun addExpandableCard(
        container: LinearLayout,
        title: String,
        content: String,
        registroId: Int,
        userManager: UserManager
    ) {
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

        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
            gravity = android.view.Gravity.START // Alineado a la izquierda
        }

        // Contenedor del contenido que será expandible
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE // Inicialmente oculto
            val formattedLines = content.split("\n")
            formattedLines.forEach { line ->
                addView(createNormalTextView(line)) // Crear texto normal
            }
        }

        // Icono para expandir o colapsar el contenido
        val expandIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.arrow_down_float) // Ícono de desplegar
            setPadding(8, 8, 8, 8)
            setOnClickListener {
                if (contentLayout.visibility == View.GONE) {
                    contentLayout.visibility = View.VISIBLE
                    setImageResource(android.R.drawable.arrow_up_float) // Cambiar al ícono de colapsar
                } else {
                    contentLayout.visibility = View.GONE
                    setImageResource(android.R.drawable.arrow_down_float) // Cambiar al ícono de desplegar
                }
            }
        }

        // Botón para eliminar el registro
        val deleteButton = TextView(this).apply {
            text = "Eliminar"
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            setPadding(0, 16, 0, 8)
            setOnClickListener {
                showConfirmationDialog(container, cardView, registroId, userManager)
            }
        }

        val titleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(titleView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(expandIcon)
        }

        linearLayout.addView(titleLayout)
        linearLayout.addView(contentLayout)
        linearLayout.addView(deleteButton)
        cardView.addView(linearLayout)
        container.addView(cardView)
    }

    // Método para mostrar un diálogo de confirmación
    private fun showConfirmationDialog(container: LinearLayout, cardView: CardView, registroId: Int, userManager: UserManager) {
        val dialog = AlertDialog.Builder(this).apply {
            setTitle("Confirmar Eliminación")
            setMessage("¿Estás seguro de que deseas eliminar este registro de mantenimiento?")
            setPositiveButton("Sí, seguro") { _, _ ->
                val success = userManager.eliminarRegistroMantenimiento(registroId)
                if (success) {
                    container.removeView(cardView) // Eliminar la tarjeta de la vista
                    Toast.makeText(this@HistorialMantenimiento, "Registro eliminado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@HistorialMantenimiento, "Error al eliminar el registro", Toast.LENGTH_SHORT).show()
                }
            }
            setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss() // Cerrar el diálogo
            }
        }.create()

        dialog.show()
    }

    // Método para crear un TextView con texto normal
    private fun createNormalTextView(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            typeface = android.graphics.Typeface.MONOSPACE // Fuente monoespaciada
            setPadding(0, 8, 0, 8)
            gravity = android.view.Gravity.START
        }
    }

    // Método para formatear el contenido
    private fun formatContent(content: String): String {
        // Separar las líneas y agregar espaciado para que estén alineadas
        val lines = content.split("\n")
        return lines.joinToString("\n") { line ->
            val parts = line.split(":")
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                "%-25s: %s".format(key, value) // Alinear las claves con un ancho fijo
            } else {
                line.trim() // Si no tiene ":", devolver la línea tal cual
            }
        }
    }
}
