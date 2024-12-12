package com.example.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.login.databinding.ActivityRestablecerContrasenaBinding
import kotlin.random.Random

class restablecerContrasena : AppCompatActivity() {

    private lateinit var binding: ActivityRestablecerContrasenaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar View Binding
        binding = ActivityRestablecerContrasenaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = UserManager(this) // Instancia de la base de datos

        binding.RestContrasena.setOnClickListener {
            val email = binding.Email.text.toString().trim()

            if (email.isNotEmpty() && isValidEmail(email)) {
                if (db.emailExists(email)) {
                    val verificationCode = generateVerificationCode() // Generar un código dinámico
                    val intent = Intent(this, codigoVerificacion::class.java)
                    intent.putExtra("email", email) // Pasar el correo al siguiente layout
                    intent.putExtra("verification_code", verificationCode) // Pasar el código
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "El correo no está registrado", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Ingrese un correo electrónico válido", Toast.LENGTH_SHORT).show()
            }
        }

        binding.regresar2.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun generateVerificationCode(): String {
        return Random.nextInt(100000, 999999).toString() // Generar un número de 6 dígitos
    }
}
