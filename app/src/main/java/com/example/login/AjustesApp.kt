package com.example.login

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AjustesApp : AppCompatActivity() {

    private lateinit var notificacionesSeguro: CheckBox
    private lateinit var notificacionesRevisionTecnica: CheckBox
    private lateinit var notificacionesMantenimiento: CheckBox
    private lateinit var notificacionesPermisoCirculacion: CheckBox
    private lateinit var spinnerDiasAnticipacion: Spinner
    private lateinit var guardarAjustes: Button
    private lateinit var btnPreviewNotification: Button

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val PREFS_NAME = "NotificacionesPreferences"
        const val NOTIFICACION_SEGURO = "notificaciones_seguro"
        const val NOTIFICACION_REVISION = "notificaciones_revision_tecnica"
        const val NOTIFICACION_MANTENIMIENTO = "notificaciones_mantenimiento"
        const val NOTIFICACION_PERMISO_CIRCULACION = "notificaciones_permiso_circulacion"
        const val DIAS_ANTICIPACION = "dias_anticipacion"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes_app)

        // Inicializar vistas
        inicializarVistas()

        // Configurar spinner con las opciones de días de anticipación
        configurarSpinner()

        // Cargar los valores guardados en SharedPreferences
        cargarAjustes()

        // Configurar el botón de guardar
        configurarBotonGuardar()

        // Configurar la vista previa de notificaciones
        configurarVistaPreviaNotificaciones()

        // Configurar botón de retroceso
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Regresa a la actividad anterior
        }
    }

    private fun inicializarVistas() {
        notificacionesSeguro = findViewById(R.id.notificaciones_seguro)
        notificacionesRevisionTecnica = findViewById(R.id.notificaciones_revision_tecnica)
        notificacionesMantenimiento = findViewById(R.id.notificaciones_mantenimiento)
        notificacionesPermisoCirculacion = findViewById(R.id.notificaciones_permiso_circulacion)
        spinnerDiasAnticipacion = findViewById(R.id.spinner_dias_anticipacion)
        guardarAjustes = findViewById(R.id.guardar_ajustes)
        btnPreviewNotification = findViewById(R.id.btn_preview_notification)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    private fun configurarSpinner() {
        val opcionesDias = listOf("7 días", "15 días", "30 días")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcionesDias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDiasAnticipacion.adapter = adapter
    }

    private fun cargarAjustes() {
        // Cargar valores de SharedPreferences
        notificacionesSeguro.isChecked = sharedPreferences.getBoolean(NOTIFICACION_SEGURO, false)
        notificacionesRevisionTecnica.isChecked =
            sharedPreferences.getBoolean(NOTIFICACION_REVISION, false)
        notificacionesMantenimiento.isChecked =
            sharedPreferences.getBoolean(NOTIFICACION_MANTENIMIENTO, false)
        notificacionesPermisoCirculacion.isChecked =
            sharedPreferences.getBoolean(NOTIFICACION_PERMISO_CIRCULACION, false)

        val diasAnticipacion = sharedPreferences.getInt(DIAS_ANTICIPACION, 7)
        val posicionSpinner = when (diasAnticipacion) {
            15 -> 1
            30 -> 2
            else -> 0
        }
        spinnerDiasAnticipacion.setSelection(posicionSpinner)
    }

    private fun configurarBotonGuardar() {
        guardarAjustes.setOnClickListener {
            val editor = sharedPreferences.edit()

            // Guardar valores de los CheckBoxes
            editor.putBoolean(NOTIFICACION_SEGURO, notificacionesSeguro.isChecked)
            editor.putBoolean(NOTIFICACION_REVISION, notificacionesRevisionTecnica.isChecked)
            editor.putBoolean(NOTIFICACION_MANTENIMIENTO, notificacionesMantenimiento.isChecked)
            editor.putBoolean(NOTIFICACION_PERMISO_CIRCULACION, notificacionesPermisoCirculacion.isChecked)

            // Guardar valor del Spinner
            val diasSeleccionados = when (spinnerDiasAnticipacion.selectedItemPosition) {
                1 -> 15
                2 -> 30
                else -> 7
            }
            editor.putInt(DIAS_ANTICIPACION, diasSeleccionados)

            editor.apply()

            // Actualizar alarmas basadas en los nuevos ajustes
            actualizarAlarmas()

            Toast.makeText(this, "Ajustes guardados correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarVistaPreviaNotificaciones() {
        btnPreviewNotification.setOnClickListener {
            // Crear una notificación de ejemplo
            val title = "Vista Previa de Notificación"
            val message = "Este es un ejemplo de cómo se verán tus notificaciones."

            // Enviar la notificación usando la clase MantenimientoNotificacionReceiver
            MantenimientoNotificacionReceiver.sendNotification(
                context = this,
                notificationId = 999, // ID arbitrario para la vista previa
                title = title,
                message = message
            )

            // Mostrar un mensaje para indicar que se envió la vista previa
            Toast.makeText(this, "Vista previa enviada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarAlarmas() {
        val habilitarSeguro = notificacionesSeguro.isChecked
        val habilitarRevisionTecnica = notificacionesRevisionTecnica.isChecked
        val habilitarMantenimiento = notificacionesMantenimiento.isChecked
        val habilitarPermisoCirculacion = notificacionesPermisoCirculacion.isChecked
        val diasAnticipacion = sharedPreferences.getInt(DIAS_ANTICIPACION, 7)

        // Obtener los datos del auto seleccionado
        val userManager = UserManager(this)
        val selectedAutoId = sharedPreferences.getInt("selected_auto_id", -1)
        val autoSeleccionado = userManager.getAutoById(selectedAutoId) ?: emptyMap()

        val mantenimientoAuto = MantenimientoAuto()

        if (habilitarMantenimiento) {
            MantenimientoAuto.configurarAlarmasDeMantenimiento(
                context = this,
                autoSeleccionado = autoSeleccionado,
                mantenimientoAuto = mantenimientoAuto
            )
        } else {
            cancelarNotificacionesDeTipo("mantenimiento")
        }

        if (habilitarSeguro) {
            MantenimientoAuto.configurarRecordatorioVencimiento(
                context = this,
                tipo = "Seguro del Auto",
                fechaVencimiento = autoSeleccionado["vencimiento_seguro"] as? Long ?: 0L,
                diasAnticipacion = diasAnticipacion
            )
        } else {
            cancelarNotificacionesDeTipo("seguro")
        }

        if (habilitarRevisionTecnica) {
            MantenimientoAuto.configurarRecordatorioVencimiento(
                context = this,
                tipo = "Revisión Técnica",
                fechaVencimiento = autoSeleccionado["vencimiento_revision_tecnica"] as? Long ?: 0L,
                diasAnticipacion = diasAnticipacion
            )
        } else {
            cancelarNotificacionesDeTipo("revision_tecnica")
        }

        if (habilitarPermisoCirculacion) {
            MantenimientoAuto.configurarRecordatorioVencimiento(
                context = this,
                tipo = "Permiso de Circulación",
                fechaVencimiento = autoSeleccionado["vencimiento_permiso_circulacion"] as? Long ?: 0L,
                diasAnticipacion = diasAnticipacion
            )
        } else {
            cancelarNotificacionesDeTipo("permiso_circulacion")
        }
    }

    private fun cancelarNotificacionesDeTipo(tipo: String) {
        val notificationId = when (tipo) {
            "mantenimiento" -> 1
            "seguro" -> "Seguro del Auto".hashCode()
            "revision_tecnica" -> "Revisión Técnica".hashCode()
            "permiso_circulacion" -> "Permiso de Circulación".hashCode()
            else -> 0
        }

        if (notificationId != 0) {
            MantenimientoNotificacionReceiver.cancelScheduledNotification(this, notificationId)
        }
    }
}
