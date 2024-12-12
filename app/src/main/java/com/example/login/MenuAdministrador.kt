package com.example.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MenuAdministrador : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_administrador)

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("LoginPreferences", MODE_PRIVATE)

        // Configurar el evento para el LinearLayout de cerrar sesión
        val cerrarSesionLayout = findViewById<LinearLayout>(R.id.cerrar_sesion_layout)
        cerrarSesionLayout.setOnClickListener {
            cerrarSesion()
        }

        // Configurar el evento para el LinearLayout de "ver usuarios"
        val verUsuariosLayout = findViewById<LinearLayout>(R.id.ver_usuarios)
        verUsuariosLayout.setOnClickListener {
            abrirVerUsuarios()
        }

        // Configurar el evento para el LinearLayout de ver autos
        val verAutosLayout = findViewById<LinearLayout>(R.id.ver_autos)
        verAutosLayout.setOnClickListener {
            // Redirigir al activity_ver_autos
            val intent = Intent(this, VerAutos::class.java)
            startActivity(intent)
        }

        val verPostsLayout = findViewById<LinearLayout>(R.id.ver_posts)
        verPostsLayout.setOnClickListener {
            val intent = Intent(this, VerPosts::class.java)
            startActivity(intent)
        }

        // Configurar el evento para ver las respuestas
        val verRespuestasLayout = findViewById<LinearLayout>(R.id.verRespuestas_posts)
        verRespuestasLayout.setOnClickListener {
            val intent = Intent(this, VerRespuestas::class.java)
            startActivity(intent)
        }

        // Configurar el evento para ver el historial de mantenimiento
        val verHistorialMantLayout = findViewById<LinearLayout>(R.id.verHistorial_Mant)
        verHistorialMantLayout.setOnClickListener {
            val intent = Intent(this, VerHistorialAuto::class.java)
            startActivity(intent)
        }
    }

    private fun cerrarSesion() {
        // Limpiar la sesión en SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", false)
        editor.putString("user_email", null)
        editor.putBoolean("isAdmin", false)
        editor.apply()

        // Redirigir al Login
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish() // Finalizar la actividad actual
    }

    private fun abrirVerUsuarios() {
        // Abrir la actividad VerUsuarios
        val intent = Intent(this, VerUsuarios::class.java)
        startActivity(intent)
    }
}
