package com.example.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Menu : AppCompatActivity() {

    private lateinit var userManager: UserManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var nombreUsuarioTextView: TextView
    private lateinit var iconoUsuarioImageView: ImageView
    private var userId: Int = -1 // Variable para almacenar el ID del usuario logueado

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // Inicializar base de datos y preferencias
        userManager = UserManager(this)
        sharedPreferences = getSharedPreferences("LoginPreferences", MODE_PRIVATE)

        // Vincular vistas
        nombreUsuarioTextView = findViewById(R.id.nombre_usuario)
        iconoUsuarioImageView = findViewById(R.id.icono_usuario)

        // Cargar datos del usuario al iniciar
        cargarUsuario()

        // Configurar el botón de cerrar sesión
        findViewById<LinearLayout>(R.id.cerrar_sesion_layout).setOnClickListener {
            logout()
        }

        // Configurar el botón de agregar auto
        findViewById<LinearLayout>(R.id.agregar_auto_layout).setOnClickListener {
            abrirAgregarAuto()
        }

        // Configurar el botón de Seleccionar Auto
        findViewById<LinearLayout>(R.id.seleccionar_auto).setOnClickListener {
            val intent = Intent(this, SeleccionarAuto::class.java)
            intent.putExtra("user_id", userId) // Pasar el ID del usuario logueado
            startActivity(intent)
        }

        val historialMantenimiento = findViewById<LinearLayout>(R.id.historial_mantenimiento)
        historialMantenimiento.setOnClickListener {
            val intent = Intent(this, HistorialMantenimiento::class.java)
            startActivity(intent)
        }

        // Configurar el botón de Lista Códigos
        findViewById<LinearLayout>(R.id.lista_codigos).setOnClickListener {
            val intent = Intent(this, ListaCodigos::class.java)
            startActivity(intent)
        }

        // Configurar el botón del Lector OBD
        findViewById<LinearLayout>(R.id.lector_obd).setOnClickListener {
            val intent = Intent(this, SimuladorOBD::class.java)
            startActivity(intent)
        }

        // Configurar el botón del Foto Comunitario
        findViewById<LinearLayout>(R.id.foro_comunitario).setOnClickListener {
            val intent = Intent(this, ForoComunitario::class.java)
            startActivity(intent)
        }

        // Configurar el botón de Fallas Comunes
        val fallasComunesLayout = findViewById<LinearLayout>(R.id.fallasComunes)
        fallasComunesLayout.setOnClickListener {
            val intent = Intent(this, FallasComunes::class.java)
            startActivity(intent)
        }

        // Configurar el botón de Repuestos
        findViewById<LinearLayout>(R.id.comparacion_repuestos).setOnClickListener {
            val intent = Intent(this, ComparacionRepuestos::class.java)
            startActivity(intent)
        }

        // Configurar el botón de Mantenimiento
        val mantencionAutoLayout = findViewById<LinearLayout>(R.id.mantencion_auto)
        mantencionAutoLayout.setOnClickListener {
            val intent = Intent(this, MantenimientoAuto::class.java)
            startActivity(intent)
        }

        // Configurar el botón del Perfíl
        val confPerfil = findViewById<LinearLayout>(R.id.conf_perfil)
        confPerfil.setOnClickListener {
            val intent = Intent(this, PerfilUsuario::class.java)
            startActivity(intent)
        }

        val ajustesApp = findViewById<LinearLayout>(R.id.ajustes_app)
        ajustesApp.setOnClickListener {
            val intent = Intent(this, AjustesApp::class.java)
            startActivity(intent)
        }

        val talleresCercanosLayout = findViewById<LinearLayout>(R.id.talleresCercanos)
        talleresCercanosLayout.setOnClickListener {
            val intent = Intent(this, Mapa::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarUsuario() // Recargar información del usuario al regresar
    }

    private fun cargarUsuario() {
        // Recuperar el correo electrónico del usuario almacenado en SharedPreferences
        val email = sharedPreferences.getString("user_email", null)

        if (!email.isNullOrEmpty()) {
            val user = getUserInfo(email)
            if (user != null) {
                // Guardar el user_id
                userId = user.id

                // Mostrar nombre completo
                nombreUsuarioTextView.text = "${user.first} ${user.last}"

                // Cambiar el ícono según el género
                when (user.gender) {
                    "Masculino" -> iconoUsuarioImageView.setImageResource(R.drawable.hombre)
                    "Femenino" -> iconoUsuarioImageView.setImageResource(R.drawable.mujer)
                    else -> iconoUsuarioImageView.setImageResource(R.drawable.circle_background2) // Opcional
                }
            } else {
                nombreUsuarioTextView.text = "Usuario desconocido"
            }
        } else {
            nombreUsuarioTextView.text = "Usuario desconocido"
        }
    }

    private fun getUserInfo(email: String): User? {
        val db = userManager.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${UserManager.COLUMN_ID}, ${UserManager.COLUMN_NAME}, ${UserManager.COLUMN_LASTNAME}, ${UserManager.COLUMN_GENDER} " +
                    "FROM ${UserManager.TABLE_USERS} WHERE ${UserManager.COLUMN_EMAIL} = ?",
            arrayOf(email)
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(UserManager.COLUMN_ID))
            val firstName = cursor.getString(cursor.getColumnIndexOrThrow(UserManager.COLUMN_NAME))
            val lastName = cursor.getString(cursor.getColumnIndexOrThrow(UserManager.COLUMN_LASTNAME))
            val gender = cursor.getString(cursor.getColumnIndexOrThrow(UserManager.COLUMN_GENDER))
            user = User(id, firstName, lastName, gender)
        }
        cursor.close()
        return user
    }

    private fun logout() {
        // Limpiar SharedPreferences pero mantener las credenciales si está marcado "Recordarme"
        val rememberMe = sharedPreferences.getBoolean("rememberMe", false)
        val editor = sharedPreferences.edit()
        if (rememberMe) {
            editor.putBoolean("isLoggedIn", false) // Marcar sesión como cerrada
        } else {
            editor.clear() // Limpiar todas las preferencias
        }
        editor.apply()

        // Ir a la pantalla de inicio de sesión
        val intent = Intent(this, Login::class.java)
        startActivity(intent)
        finish()
    }

    private fun abrirAgregarAuto() {
        val intent = Intent(this, AgregarAuto::class.java)
        intent.putExtra("user_id", userId) // Pasar el ID del usuario al activity AgregarAuto
        startActivity(intent)
    }

    data class User(val id: Int, val first: String, val last: String, val gender: String)
}
