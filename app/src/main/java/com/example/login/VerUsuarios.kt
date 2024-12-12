package com.example.login

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class VerUsuarios : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_usuarios)

        // Encontrar el ImageView de regresar
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            // Redirigir al MenuAdministrador
            val intent = Intent(this, MenuAdministrador::class.java)
            startActivity(intent)
            finish() // Finalizar la actividad actual
        }

        // Encontrar el LinearLayout dentro del ScrollView
        val linearLayoutUsuarios = findViewById<LinearLayout>(R.id.linearLayoutUsuarios)

        // Obtener los usuarios desde UserManager
        val userManager = UserManager(this)
        val usuarios = userManager.getAllUsers()

        // Crear dinámicamente CardViews para cada usuario
        for ((index, usuario) in usuarios.withIndex()) {
            val cardView = CardView(this).apply {
                radius = 16f
                cardElevation = 8f
                useCompatPadding = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 16, 16, 16) // Márgenes para separar las tarjetas
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

            val textViewNombre = TextView(this).apply {
                text = "Usuario ${index + 1}: ${usuario["name"]} ${usuario["lastname"]}"
                textSize = 18f
                setPadding(8, 8, 8, 8)
                setTextColor(Color.BLACK)
            }

            val buttonDetalles = Button(this).apply {
                text = "Ver Detalles"
                textSize = 16f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1976D2")) // Azul moderno
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8) // Separación entre botones
                }
                setOnClickListener { mostrarDetallesUsuario(usuario) }
            }

            val buttonEliminar = Button(this).apply {
                text = "Eliminar"
                textSize = 16f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#D32F2F")) // Rojo moderno
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                setOnClickListener { eliminarUsuario(usuario["id"]!!.toInt()) }
            }

            // Agregar contenido a la tarjeta
            cardContent.addView(textViewNombre)
            cardContent.addView(buttonDetalles)
            cardContent.addView(buttonEliminar)

            cardView.addView(cardContent)
            linearLayoutUsuarios.addView(cardView)
        }
    }

    private fun mostrarDetallesUsuario(usuario: Map<String, String>) {
        val detalles = """
            ID: ${usuario["id"]}
            Nombre: ${usuario["name"]} ${usuario["lastname"]}
            Email: ${usuario["email"]}
            Fecha de Nacimiento: ${usuario["birthdate"]}
            Teléfono: ${usuario["phone"]}
            Rol: ${usuario["role"]}
            Género: ${usuario["gender"]}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Detalles del Usuario")
            .setMessage(detalles)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun eliminarUsuario(userId: Int) {
        val userManager = UserManager(this)
        val eliminado = userManager.deleteUserById(userId)
        if (eliminado) {
            recreate() // Recargar la actividad para actualizar la lista de usuarios
        } else {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("No se pudo eliminar el usuario.")
                .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}
