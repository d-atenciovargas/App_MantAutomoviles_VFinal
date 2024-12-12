package com.example.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.login.databinding.ActivityLoginBinding

class Login : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar View Binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("LoginPreferences", MODE_PRIVATE)

        // Verificar si la sesión está activa
        verificarSesionActiva()

        // Cargar credenciales si están guardadas
        loadSavedCredentials()

        // Configurar listener para el botón "Ingresar"
        binding.botonIngresar.setOnClickListener {
            val email = binding.Email.text.toString().trim()
            val password = binding.contrasena.text.toString().trim()

            if (EmailNoValido(email) && ContrasenaNoValida(password)) {
                val userManager = UserManager(this)
                val adminManager = AdminManager(this, "Administradores.db")

                // Verificar si es un administrador
                if (adminManager.checkAdmin(email, password)) {
                    saveSessionAndCredentials(email, password, isAdmin = true)
                    val intent = Intent(this, MenuAdministrador::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                }
                // Verificar si es un usuario normal
                else if (userManager.checkUser(email, password)) {
                    saveSessionAndCredentials(email, password, isAdmin = false)
                    val intent = Intent(this, Menu::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Correo electrónico o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Correo electrónico o contraseña no válidos", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar listener para el botón "Registro"
        binding.botonRegistro.setOnClickListener {
            val intent = Intent(this, Registro::class.java)
            startActivity(intent)
        }
    }

    private fun verificarSesionActiva() {
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        val savedEmail = sharedPreferences.getString("user_email", null)
        val isAdmin = sharedPreferences.getBoolean("isAdmin", false)

        if (isLoggedIn && !savedEmail.isNullOrEmpty()) {
            val intent = if (isAdmin) {
                Intent(this, MenuAdministrador::class.java)
            } else {
                Intent(this, Menu::class.java)
            }
            intent.putExtra("email", savedEmail)
            startActivity(intent)
            finish()
        }
    }

    private fun loadSavedCredentials() {
        val isRemembered = sharedPreferences.getBoolean("rememberMe", false)
        if (isRemembered) {
            val savedEmail = sharedPreferences.getString("savedEmail", "")
            val savedPassword = sharedPreferences.getString("savedPassword", "")
            binding.Email.setText(savedEmail)
            binding.contrasena.setText(savedPassword)
            binding.rememberMeCheckBox.isChecked = true
        }
    }

    private fun saveSessionAndCredentials(email: String, password: String, isAdmin: Boolean) {
        val editor = sharedPreferences.edit()
        val rememberMe = binding.rememberMeCheckBox.isChecked

        // Guardar sesión activa
        editor.putBoolean("isLoggedIn", true)
        editor.putString("user_email", email)
        editor.putBoolean("isAdmin", isAdmin)

        // Guardar credenciales si se selecciona "Recordarme"
        if (rememberMe) {
            editor.putBoolean("rememberMe", true)
            editor.putString("savedEmail", email)
            editor.putString("savedPassword", password)
        } else {
            editor.putBoolean("rememberMe", false)
            editor.remove("savedEmail")
            editor.remove("savedPassword")
        }
        editor.apply()
    }

    private fun EmailNoValido(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun ContrasenaNoValida(password: String): Boolean {
        return password.isNotEmpty() && password.length >= 6
    }

    fun forgotPasswordClicked(view: android.view.View) {
        val intent = Intent(this, restablecerContrasena::class.java)
        startActivity(intent)
    }
}
