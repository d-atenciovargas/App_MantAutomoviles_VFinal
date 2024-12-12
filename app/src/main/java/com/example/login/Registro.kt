package com.example.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.login.databinding.ActivityRegistroBinding

class Registro : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar View Binding
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mantener funcionalidad original para formatear fecha
        binding.fechaNacimiento.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isFormatting) return

                isFormatting = true
                val input = s.toString().replace("/", "")
                val formattedInput = StringBuilder()

                for (i in input.indices) {
                    formattedInput.append(input[i])
                    if ((i == 1 || i == 3) && i < 8) {
                        formattedInput.append("/")
                    }
                }

                if (formattedInput.length > 10) {
                    formattedInput.setLength(10)
                }

                binding.fechaNacimiento.setText(formattedInput)
                binding.fechaNacimiento.setSelection(formattedInput.length)
                isFormatting = false
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Configuración inicial para el teléfono
        binding.telefono.setText("+56 ")
        binding.telefono.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isFormatting) return

                isFormatting = true
                val input = s.toString().replace("+56", "").replace(" ", "")

                // Forzar exactamente 9 números
                if (input.length > 9) {
                    binding.telefono.setText("+56 ${input.substring(0, 9)}")
                    binding.telefono.setSelection(binding.telefono.text!!.length)
                } else {
                    binding.telefono.setText("+56 $input")
                    binding.telefono.setSelection(binding.telefono.text!!.length)
                }

                isFormatting = false
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Configurar OnClickListener para el botón "Regresar"
        binding.regresar.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        // Configurar OnClickListener para el botón "Finalizar"
        binding.finalizar.setOnClickListener {
            val name = binding.nombres.text.toString().trim()
            val lastname = binding.apellidos.text.toString().trim()
            val birthdate = binding.fechaNacimiento.text.toString().trim()
            val phone = binding.telefono.text.toString().replace("+56", "").trim()
            val email = binding.registroEmail.text.toString().trim()
            val password = binding.registroContrasena.text.toString().trim()
            val gender = binding.spinnerGenero.selectedItem.toString()

            // Validar campos obligatorios
            if (name.isEmpty() || lastname.isEmpty() || birthdate.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar género
            if (gender == "Seleccionar género") {
                Toast.makeText(this, "Debe seleccionar un género válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar que el teléfono tenga exactamente 9 números
            if (!isValidPhone(phone)) {
                Toast.makeText(this, "El número de teléfono debe tener exactamente 9 dígitos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = UserManager(this)

            // Verificar si el correo ya existe
            if (db.emailExists(email)) {
                Toast.makeText(this, "El correo electrónico ya se encuentra en uso", Toast.LENGTH_SHORT).show()
            } else {
                val id = db.addUser(email, password, name, lastname, birthdate, phone, gender)
                if (id != -1L) {
                    Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error al registrar el usuario", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.length == 9 // Validar que tenga exactamente 9 dígitos
    }
}
