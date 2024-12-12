package com.example.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.login.databinding.ActivityNuevaContrasenaBinding

class nuevaContrasena : AppCompatActivity() {

    private lateinit var binding: ActivityNuevaContrasenaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar View Binding
        binding = ActivityNuevaContrasenaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = UserManager(this) // Instancia de la base de datos
        val email = intent.getStringExtra("email") // Obtener el correo del Intent

        binding.regresar2.setOnClickListener {
            val nuevaPassword = binding.nuevaContrasena.text.toString().trim()
            val confirmePassword = binding.confirmeContrasena.text.toString().trim()

            // Validar que los campos no estén vacíos
            if (nuevaPassword.isEmpty() || confirmePassword.isEmpty()) {
                Toast.makeText(this, "Ambos campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar que las contraseñas coincidan
            if (nuevaPassword != confirmePassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Actualizar la contraseña en la base de datos
            if (email != null) {
                val isUpdated = db.updatePassword(email, nuevaPassword)
                if (isUpdated) {
                    Toast.makeText(this, "Contraseña actualizada con éxito", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error al actualizar la contraseña", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Ocurrió un error. Intente nuevamente.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.regresar4.setOnClickListener {
            // Regresar al activity anterior
            finish()
        }
    }
}
