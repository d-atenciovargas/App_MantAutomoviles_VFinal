package com.example.login

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PerfilUsuario : AppCompatActivity() {

    private lateinit var userManager: UserManager
    private lateinit var currentUserEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_usuario)

        // Configurar el botón de retroceso
        val btnBackPerfilUsuario = findViewById<ImageView>(R.id.btnBack)
        btnBackPerfilUsuario.setOnClickListener {
            finish() // Regresa a la actividad anterior
        }

        // Inicializar UserManager
        userManager = UserManager(this)

        // Obtener el email del usuario actual desde SharedPreferences
        val sharedPreferences = getSharedPreferences("LoginPreferences", MODE_PRIVATE)
        currentUserEmail = sharedPreferences.getString("user_email", null) ?: ""

        // Obtener los datos del usuario
        val usuario = userManager.getUserByEmail(currentUserEmail)

        // Referencias a las vistas
        val nombreCompletoUsuario = findViewById<TextView>(R.id.nombre_completo_usuario)
        val iconoGeneroUsuario = findViewById<ImageView>(R.id.icono_genero_usuario)
        val emailUsuario = findViewById<TextView>(R.id.email_usuario)
        val fechaNacimientoUsuario = findViewById<TextView>(R.id.fecha_nacimiento_usuario)
        val telefonoUsuario = findViewById<TextView>(R.id.telefono_usuario)
        val rolUsuario = findViewById<TextView>(R.id.rol_usuario)
        val generoUsuario = findViewById<TextView>(R.id.genero_usuario)
        val cantidadAutosUsuario = findViewById<TextView>(R.id.cantidad_autos_usuario)

        // Mostrar los datos en la vista
        usuario?.let {
            nombreCompletoUsuario.text = "${it["name"]} ${it["lastname"]}"
            emailUsuario.text = "Email: ${it["email"]}"
            fechaNacimientoUsuario.text = "Fecha de Nacimiento: ${it["birthdate"]}"
            telefonoUsuario.text = "Teléfono: ${it["phone"]}"
            rolUsuario.text = "Rol: ${it["role"]}"
            generoUsuario.text = "Género: ${it["gender"]}"

            // Cambiar el ícono según el género
            val genero = it["gender"]?.toString()?.lowercase()
            when (genero) {
                "masculino" -> iconoGeneroUsuario.setImageResource(R.drawable.hombre)
                "femenino" -> iconoGeneroUsuario.setImageResource(R.drawable.mujer)
                else -> iconoGeneroUsuario.setImageResource(R.drawable.circle_background2)
            }

            // Obtener y mostrar la cantidad de autos agregados por el usuario
            val userId = userManager.getUserIdByEmail(currentUserEmail)
            val cantidadAutos = userManager.getAutosByUserId(userId).size
            cantidadAutosUsuario.text = "Autos agregados: $cantidadAutos"
        }
    }
}
