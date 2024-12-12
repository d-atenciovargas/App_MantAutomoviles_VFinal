package com.example.login

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.login.databinding.ActivityCodigoVerificacionBinding

class codigoVerificacion : AppCompatActivity() {

    private lateinit var binding: ActivityCodigoVerificacionBinding
    private lateinit var verificationCode: String
    private val handler = Handler()
    private var isCodeValid = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar View Binding
        binding = ActivityCodigoVerificacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuración para permitir solo números y limitar la longitud del EditText
        binding.codVerificacion.apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER // Permitir solo números
            filters = arrayOf(android.text.InputFilter.LengthFilter(6)) // Limitar a 6 caracteres
        }

        // Obtener el correo y el código de verificación del intent
        val email = intent.getStringExtra("email")
        verificationCode = intent.getStringExtra("verification_code") ?: ""

        // Mostrar el código de verificación al usuario
        showVerificationCodeFor10Seconds(verificationCode)

        // Configurar el botón "Continuar"
        binding.continuar.setOnClickListener {
            val inputCode = binding.codVerificacion.text.toString().trim()

            if (inputCode.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese el código", Toast.LENGTH_SHORT).show()
            } else if (isCodeValid && inputCode == verificationCode) {
                // Código correcto, redirigir al activity_nueva_contrasena
                val intent = Intent(this, nuevaContrasena::class.java)
                intent.putExtra("email", email)
                startActivity(intent)
                finish()
            } else if (!isCodeValid) {
                Toast.makeText(this, "El código ya no es válido. Reenvíe el código.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "El código ingresado es incorrecto", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar el botón "Reenviar código"
        binding.reenviarCodigo.setOnClickListener {
            // Generar un nuevo código de verificación
            verificationCode = generateVerificationCode()
            isCodeValid = true // Marcar el nuevo código como válido
            showVerificationCodeFor10Seconds(verificationCode)
        }

        // Configurar el botón "Regresar"
        binding.regresar3.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish() // Finalizar la actividad actual
        }
    }

    private fun showVerificationCodeFor10Seconds(code: String) {
        binding.notificationBar.text = "Código de verificación: $code"
        binding.notificationBar.visibility = View.VISIBLE

        // Deshabilitar el código después de 10 segundos
        handler.postDelayed({
            binding.notificationBar.visibility = View.GONE
            isCodeValid = false
        }, 10000)
    }

    private fun generateVerificationCode(): String {
        return kotlin.random.Random.nextInt(100000, 999999).toString() // Generar un número de 6 dígitos
    }
}
