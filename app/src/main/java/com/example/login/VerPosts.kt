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

class VerPosts : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_post)

        // Encontrar el ImageView de regresar
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            // Redirigir al MenuAdministrador
            val intent = Intent(this, MenuAdministrador::class.java)
            startActivity(intent)
            finish() // Finalizar la actividad actual
        }

        // Encontrar el LinearLayout dentro del ScrollView
        val linearLayoutPosts = findViewById<LinearLayout>(R.id.linearLayoutPosts)

        // Obtener los posts desde UserManager
        val userManager = UserManager(this)
        val posts = userManager.getAllPosts()

        // Crear dinámicamente CardViews para cada post
        for ((index, post) in posts.withIndex()) {
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
                text = "Post ${index + 1}: ${post["name"]} ${post["lastname"]}"
                textSize = 18f
                setPadding(8, 8, 8, 4)
                setTextColor(resources.getColor(android.R.color.black, null))
            }

            val textViewContenido = TextView(this).apply {
                text = "Contenido: ${post["content"]}"
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }

            val buttonEliminar = Button(this).apply {
                text = "Eliminar"
                setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
                setTextColor(resources.getColor(android.R.color.white, null))
                setOnClickListener {
                    val postId = (post["post_id"] as? String)?.toIntOrNull()
                    if (postId != null) {
                        eliminarPost(postId)
                    } else {
                        mostrarError("ID del post no válido.")
                    }
                }
            }

            // Agregar contenido a la tarjeta
            cardContent.addView(textViewTitulo)
            cardContent.addView(textViewContenido)
            cardContent.addView(buttonEliminar)

            cardView.addView(cardContent)
            linearLayoutPosts.addView(cardView)
        }
    }

    private fun eliminarPost(postId: Int) {
        val userManager = UserManager(this)
        val eliminado = userManager.deletePost(postId)
        if (eliminado > 0) {
            recreate() // Recargar la actividad para actualizar la lista de posts
        } else {
            mostrarError("No se pudo eliminar el post.")
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
