package com.example.login

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView


class MantenimientoAuto : AppCompatActivity() {

    private lateinit var mantenimientoContainer: LinearLayout
    private lateinit var autoSeleccionado: Map<String, Any>

    companion object {
        // Configuración del canal de notificaciones
        const val CANAL_ID = "MANTENIMIENTO"
        const val CANAL_NOMBRE = "Recordatorios de Mantenimiento"
        const val CANAL_DESCRIPCION = "Canal para notificaciones de mantenimiento del auto."

        // Frecuencia de mantenimiento en kilómetros
        const val CAMBIO_ACEITE = 15000
        const val FILTRO_AIRE = 15000
        const val REVISION_FRENOS = 20000
        const val CAMBIO_BUJIAS = 40000

        // Mantenimientos programados con sus frecuencias
        val MANTENIMIENTOS_PROGRAMADOS = listOf(
            "Cambio de aceite" to CAMBIO_ACEITE,
            "Revisión del filtro de aire" to FILTRO_AIRE,
            "Revisión de frenos" to REVISION_FRENOS,
            "Cambio de bujías" to CAMBIO_BUJIAS
        )

        // Precios referenciales de mantenimiento (en CLP)
        val COSTOS_REFERENCIALES = mapOf(
            "Cambio de aceite" to 60000,
            "Revisión del filtro de aire" to 30000,
            "Revisión de frenos" to 80000,
            "Cambio de bujías" to 50000
        )

        // Lista de tareas comunes de mantenimiento preventivo
        val TAREAS_MANTENIMIENTO = listOf(
            "Revisar presión de neumáticos",
            "Verificar niveles de aceite",
            "Chequear estado del sistema de frenos",
            "Revisar el estado de las luces",
            "Inspeccionar el sistema de suspensión",
            "Controlar niveles del líquido refrigerante",
            "Verificar batería y conexiones eléctricas",
            "Comprobar el estado de las correas",
            "Inspeccionar fugas debajo del vehículo"
        )

        // Claves para SharedPreferences
        const val PREFS_AUTO = "AutoPreferences"
        const val PREFS_SELECTED_AUTO_ID = "selected_auto_id"
        const val PREFS_LAST_MAINTENANCE_DATE = "last_maintenance_date"
        const val PREFS_LAST_MAINTENANCE_KM = "last_maintenance_km"
        const val PREFS_TIPO_ACEITE = "tipo_aceite"

        // Promedio mensual de kilometraje (para cálculos de fechas estimadas)
        const val PROMEDIO_MENSUAL_KM = 1000

        // Configurar alarmas de mantenimiento
        fun configurarAlarmasDeMantenimiento(
            context: Context,
            autoSeleccionado: Map<String, Any>,
            mantenimientoAuto: MantenimientoAuto
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    context.startActivity(intent)
                }
            }

            // Acceder a los ajustes de notificaciones desde SharedPreferences
            val sharedPreferences = context.getSharedPreferences(AjustesApp.PREFS_NAME, Context.MODE_PRIVATE)

            // Verificar si las notificaciones están habilitadas antes de configurar alarmas
            val notificarSeguro = sharedPreferences.getBoolean(AjustesApp.NOTIFICACION_SEGURO, false)
            val notificarRevisionTecnica = sharedPreferences.getBoolean(AjustesApp.NOTIFICACION_REVISION, false)
            val notificarMantenimiento = sharedPreferences.getBoolean(AjustesApp.NOTIFICACION_MANTENIMIENTO, false)
            val notificarPermisoCirculacion = sharedPreferences.getBoolean("notificaciones_permiso_circulacion", false)

            val diasAnticipacion = sharedPreferences.getInt(AjustesApp.DIAS_ANTICIPACION, 7)

            // Configurar notificaciones para próximos servicios de mantenimiento si están habilitadas
            if (notificarMantenimiento) {
                val kilometraje = autoSeleccionado["kilometraje"] as? Int ?: 0
                val proximosServicios = mantenimientoAuto.calcularProximosServicios(kilometraje)

                proximosServicios.forEachIndexed { index, servicio ->
                    val triggerTime = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, diasAnticipacion)
                    }.timeInMillis

                    MantenimientoNotificacionReceiver.scheduleNotification(
                        context = context,
                        notificationId = index,
                        service = servicio,
                        triggerAtMillis = triggerTime
                    )
                }
            }

            // Configurar notificaciones para vencimientos críticos
            if (notificarSeguro) {
                val fechaVencimientoSeguro = autoSeleccionado["vencimiento_seguro"] as? Long ?: 0L
                configurarRecordatorioVencimiento(
                    context = context,
                    tipo = "Seguro del Auto",
                    fechaVencimiento = fechaVencimientoSeguro,
                    diasAnticipacion = diasAnticipacion
                )
            }

            if (notificarRevisionTecnica) {
                val fechaVencimientoRevisionTecnica = autoSeleccionado["vencimiento_revision_tecnica"] as? Long ?: 0L
                configurarRecordatorioVencimiento(
                    context = context,
                    tipo = "Revisión Técnica",
                    fechaVencimiento = fechaVencimientoRevisionTecnica,
                    diasAnticipacion = diasAnticipacion
                )
            }

            if (notificarPermisoCirculacion) {
                val fechaVencimientoPermisoCirculacion = autoSeleccionado["vencimiento_permiso_circulacion"] as? Long ?: 0L
                configurarRecordatorioVencimiento(
                    context = context,
                    tipo = "Permiso de Circulación",
                    fechaVencimiento = fechaVencimientoPermisoCirculacion,
                    diasAnticipacion = diasAnticipacion
                )
            }
        }

        // Configurar recordatorios para vencimientos críticos
        fun configurarRecordatorioVencimiento(
            context: Context,
            tipo: String,
            fechaVencimiento: Long,
            diasAnticipacion: Int = 7
        ) {
            if (fechaVencimiento <= 0) {
                if (context is MantenimientoAuto) {
                    context.mantenimientoContainer.addView(
                        context.crearCard(
                            listOf("Por favor, registra la fecha de vencimiento del $tipo."),
                            "Vencimiento de $tipo"
                        )
                    )
                }
                return
            }

            val diasRestantes = (fechaVencimiento - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
            val mensaje = if (diasRestantes > 0) {
                "Faltan $diasRestantes días para el vencimiento del $tipo."
            } else {
                "El $tipo ya está vencido."
            }

            if (context is MantenimientoAuto) {
                context.mantenimientoContainer.addView(
                    context.crearCard(listOf(mensaje), "Vencimiento de $tipo")
                )
            }

            if (diasRestantes > diasAnticipacion) {
                MantenimientoNotificacionReceiver.scheduleNotification(
                    context = context,
                    notificationId = tipo.hashCode(),
                    service = "Recordatorio: Renovación de $tipo",
                    triggerAtMillis = fechaVencimiento - (diasAnticipacion * 24 * 60 * 60 * 1000)
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mantenimiento_auto)

        // Inicializar las vistas principales
        inicializarVistas()

        // Crear el canal de notificaciones si no existe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MantenimientoNotificacionReceiver.createNotificationChannel(this)
        }

        // Cargar la información del vehículo seleccionado desde los datos guardados
        cargarDatosDelAutoSeleccionado()

        // Actualizar la interfaz con los datos cargados
        actualizarInterfaz()

        // Nota: Se elimina esta línea para evitar la duplicación
        // mostrarListaDeTareas()
    }


    private fun inicializarVistas() {
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        mantenimientoContainer = findViewById(R.id.mantenimientoContainer)
    }

    private fun actualizarInterfaz() {
        // Limpiar el contenedor de mantenimiento antes de actualizar la interfaz
        mantenimientoContainer.removeAllViews()

        // Mostrar información del vehículo seleccionado
        mostrarInformacionDelAuto()

        // Calcular y mostrar los próximos servicios de mantenimiento
        calcularYMostrarProximosServicios()

        // Mostrar cronograma visual basado en el kilometraje
        autoSeleccionado["kilometraje"]?.toString()?.toIntOrNull()?.let { kilometraje ->
            mostrarCronogramaVisual(kilometraje)

            // Configurar alertas relacionadas con el kilometraje
            configurarAlertas(kilometraje)
        }

        // Generar y mostrar sugerencias de mantenimiento preventivo
        autoSeleccionado["kilometraje"]?.toString()?.toIntOrNull()?.let { kilometraje ->
            autoSeleccionado["ano"]?.toString()?.toIntOrNull()?.let { ano ->
                generarSugerenciasMantenimientoPreventivo(kilometraje, ano)
            }
        }

        // Mostrar la lista de tareas comunes de mantenimiento
        mostrarListaDeTareas()

        // Mostrar presupuesto de mantenimiento basado en servicios
        mostrarPresupuestoDeMantenimiento()

        // Mostrar el estado de los componentes del vehículo
        mostrarEstadoComponentes() // Sección agregada para seguimiento de componentes

        // Mostrar el formulario para actualizar los datos del vehículo (penúltimo)
        mostrarFormularioActualizacion()

        // Agregar botón al final para registrar el mantenimiento realizado (último)
        agregarBotonRegistrarMantenimiento()
    }


    private fun configurarAlertas(kilometraje: Int) {
        configurarAlertasPorKilometraje(kilometraje)
        configurarAlertasPorTiempo()
    }

    private fun cargarDatosDelAutoSeleccionado() {
        val sharedPreferences = getSharedPreferences(PREFS_AUTO, MODE_PRIVATE)
        val selectedAutoId = sharedPreferences.getInt(PREFS_SELECTED_AUTO_ID, -1)

        if (selectedAutoId != -1) {
            val userManager = UserManager(this)
            autoSeleccionado = userManager.getAutoById(selectedAutoId) ?: emptyMap()
        } else {
            autoSeleccionado = emptyMap()
        }
    }

    private fun actualizarDatosVehiculo(kilometraje: Int, tipoAceite: String) {
        val sharedPreferences = getSharedPreferences("AutoPreferences", MODE_PRIVATE)
        val selectedAutoId = sharedPreferences.getInt("selected_auto_id", -1)

        if (selectedAutoId == -1) {
            Log.e("MantenimientoAuto", "No se pudo actualizar el kilometraje porque el ID del auto no está definido.")
            return
        }

        // Actualizar kilometraje en la base de datos
        val userManager = UserManager(this)
        val isUpdated = userManager.actualizarKilometraje(selectedAutoId, kilometraje)

        if (isUpdated) {
            // Guardar tipo de aceite temporalmente en SharedPreferences
            sharedPreferences.edit().putString("tipo_aceite", tipoAceite).apply()

            // Refrescar la vista
            mantenimientoContainer.removeAllViews()
            cargarDatosDelAutoSeleccionado()
            actualizarInterfaz()

            android.widget.Toast.makeText(
                this,
                "Datos actualizados correctamente.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            android.widget.Toast.makeText(
                this,
                "Error al actualizar los datos.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun mostrarFormularioActualizacion() {
        val sharedPreferences = getSharedPreferences("AutoPreferences", MODE_PRIVATE)
        val selectedAutoId = sharedPreferences.getInt("selected_auto_id", -1)
        val fechaKey = "last_maintenance_date_$selectedAutoId"
        val ultimaFechaMantenimiento = sharedPreferences.getLong(fechaKey, 0L)

        val formContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(32, 48, 32, 48) // Márgenes amplios alrededor del formulario
            }
            setPadding(24, 24, 24, 24) // Padding interno
            setBackgroundResource(android.R.color.background_light) // Fondo claro para resaltar el formulario
            elevation = 8f // Sombra para mejorar la estética
        }

        // Campo para el kilometraje actual
        val kilometrajeInput = TextView(this).apply {
            text = "Kilometraje Actual"
            textSize = 20f
            setPadding(0, 16, 0, 8)
            setTextColor(resources.getColor(android.R.color.black, null))
        }
        val kilometrajeEditText = android.widget.EditText(this).apply {
            hint = "Ingrese el nuevo kilometraje"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            textSize = 18f
            setPadding(16, 16, 16, 16)
            setBackgroundResource(android.R.drawable.edit_text) // Fondo con borde
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 24) // Espaciado entre campos
            }

            // Limitar el número máximo de caracteres a 6
            filters = arrayOf(android.text.InputFilter.LengthFilter(6))

            // Validar que no supere 999999
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    s?.toString()?.toIntOrNull()?.let {
                        if (it > 999999) {
                            setText("999999")
                            setSelection(length()) // Colocar el cursor al final
                        }
                    }
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }

        // Campo para el tipo de aceite
        val tipoAceiteInput = TextView(this).apply {
            text = "Tipo de Aceite"
            textSize = 20f
            setPadding(0, 16, 0, 8)
            setTextColor(resources.getColor(android.R.color.black, null))
        }

        // Spinner para el tipo de aceite
        val tiposDeAceite = listOf(
            "Selecciona el tipo de aceite",
            "0W-20 Sintético",
            "5W-30 Sintético",
            "10W-40 Semisintético",
            "15W-40 Mineral",
            "15W-40 Semisintético",
            "20W-50 Mineral",
            "20W-50 Semisintético",
            "5W-40 Sintético",
            "10W-30 Semisintético"
        )
        val tipoAceiteSpinner = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(
                this@MantenimientoAuto,
                android.R.layout.simple_spinner_item,
                tiposDeAceite
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 24) // Espaciado entre campos
            }
        }

        // Campo para la fecha de mantenimiento
        val fechaInput = TextView(this).apply {
            text = "Fecha de Último Mantenimiento"
            textSize = 20f
            setPadding(0, 16, 0, 8)
            setTextColor(resources.getColor(android.R.color.black, null))
        }
        val fechaButton = Button(this).apply {
            text = if (ultimaFechaMantenimiento > 0) {
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(ultimaFechaMantenimiento)
            } else {
                "Seleccionar Fecha"
            }
            textSize = 18f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 24) // Espaciado entre campos
            }
            setBackgroundResource(android.R.drawable.btn_default)
            setOnClickListener {
                val datePicker = android.app.DatePickerDialog(this@MantenimientoAuto)
                datePicker.setOnDateSetListener { _, year, month, dayOfMonth ->
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(year, month, dayOfMonth)
                    val fechaSeleccionada = calendar.timeInMillis
                    text = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(fechaSeleccionada)
                    sharedPreferences.edit().putLong(fechaKey, fechaSeleccionada).apply()
                }
                datePicker.show()
            }
        }

        // Botón para actualizar
        val actualizarButton = Button(this).apply {
            text = "Actualizar Datos"
            textSize = 18f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 24, 0, 0) // Espaciado extra para el botón final
            }
            setBackgroundResource(android.R.drawable.btn_default)
            setOnClickListener {
                val kilometraje = kilometrajeEditText.text.toString().toIntOrNull()
                val tipoAceiteSeleccionado = tipoAceiteSpinner.selectedItem.toString()

                if (kilometraje != null && tipoAceiteSeleccionado != "Selecciona el tipo de aceite") {
                    actualizarDatosVehiculo(kilometraje, tipoAceiteSeleccionado)
                } else {
                    android.widget.Toast.makeText(
                        this@MantenimientoAuto,
                        "Por favor complete todos los campos correctamente.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Agregar los elementos al contenedor
        formContainer.addView(kilometrajeInput)
        formContainer.addView(kilometrajeEditText)
        formContainer.addView(tipoAceiteInput)
        formContainer.addView(tipoAceiteSpinner)
        formContainer.addView(fechaInput)
        formContainer.addView(fechaButton)
        formContainer.addView(actualizarButton)

        mantenimientoContainer.addView(formContainer)
    }


    private fun crearCard(contenido: List<String>, titulo: String? = null): CardView {
        val card = CardView(this).apply {
            radius = 16f
            cardElevation = 8f
            setContentPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(16, 16, 16, 24) } // Mayor margen inferior
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        titulo?.let {
            val titleView = TextView(this).apply {
                text = it
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 0, 0, 16)
            }
            contentLayout.addView(titleView)
        }

        contenido.forEach { texto ->
            val textView = TextView(this).apply {
                text = texto
                textSize = 16f
                setPadding(0, 8, 0, 8)
            }
            contentLayout.addView(textView)
        }

        card.addView(contentLayout)
        return card
    }

    private fun mostrarInformacionDelAuto() {
        if (autoSeleccionado.isNotEmpty()) {
            val labels = mapOf(
                "marca" to "Marca: ",
                "modelo" to "Modelo: ",
                "ano" to "Año: ",
                "version" to "Versión: ",
                "transmision" to "Transmisión: ",
                "kilometraje" to "Kilometraje: ",
                "placa_patente" to "Placa Patente: "
            )

            val contenido = autoSeleccionado.map { (key, value) ->
                val label = labels[key] ?: "$key: "
                "$label$value"
            }

            mantenimientoContainer.addView(crearCard(contenido))
        }
    }

    private fun mostrarCronogramaVisual(kilometraje: Int) {
        val cronograma = calcularCronogramaVisual(kilometraje)
        val cronogramaFiltrado = cronograma.filterNot { it.contains("(VENCIDO)") && kilometraje == 0 }

        if (cronogramaFiltrado.isNotEmpty()) {
            mantenimientoContainer.addView(
                crearCard(cronogramaFiltrado, "Cronograma de Mantenimiento")
            )
        }
    }

    private fun calcularYMostrarProximosServicios() {
        val kilometraje = autoSeleccionado["kilometraje"] as? Int ?: 0
        val proximosServicios = calcularProximosServicios(kilometraje)

        mantenimientoContainer.addView(
            crearCard(proximosServicios, "Próximos Servicios")
        )
    }

    private fun calcularProximosServicios(kilometraje: Int): List<String> {
        val promedioMensualKm = PROMEDIO_MENSUAL_KM // Usar constante
        val servicios = MANTENIMIENTOS_PROGRAMADOS

        return servicios.map { (servicio, frecuencia) ->
            val restante = frecuencia - (kilometraje % frecuencia)
            val proximoKilometraje = kilometraje + restante
            val mesesFaltantes = restante / promedioMensualKm
            val fechaEstimada = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(
                java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.MONTH, mesesFaltantes)
                }.time
            )

            when {
                restante <= 0 -> "$servicio: $proximoKilometraje km (VENCIDO)"
                restante < 1000 -> "$servicio: $proximoKilometraje km (PRÓXIMO) - Fecha estimada: $fechaEstimada"
                else -> "$servicio: $proximoKilometraje km (FUTURO) - Fecha estimada: $fechaEstimada"
            }
        }
    }

    private fun calcularCronogramaVisual(kilometraje: Int): List<String> {
        val promedioMensualKm = PROMEDIO_MENSUAL_KM // Usar constante
        return MANTENIMIENTOS_PROGRAMADOS.map { (servicio, frecuencia) ->
            val restante = kilometraje % frecuencia
            val proximoKilometraje = kilometraje + (frecuencia - restante)
            val mesesFaltantes = (proximoKilometraje - kilometraje) / promedioMensualKm
            val fechaEstimada = java.text.SimpleDateFormat("MM/yyyy", java.util.Locale.getDefault()).format(
                java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.MONTH, mesesFaltantes)
                }.time
            )
            val estado = when {
                restante == 0 -> "(VENCIDO)"
                restante < 1000 -> "(PRÓXIMO)"
                else -> "(FUTURO)"
            }
            "$servicio: $proximoKilometraje km ($fechaEstimada) $estado"
        }
    }

    private fun generarSugerenciasMantenimientoPreventivo(kilometraje: Int, ano: Int) {
        val recomendaciones = mutableListOf<String>()
        val anoActual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val antiguedad = anoActual - ano

        // Sugerencias basadas en el kilometraje
        when {
            kilometraje < 50000 -> recomendaciones.add("Revisión general de frenos y neumáticos.")
            kilometraje in 50000..100000 -> {
                recomendaciones.add("Cambio de aceite (entre los 15,000 y 20,000 km recorridos desde el último cambio).")
                recomendaciones.add("Revisión del sistema de refrigeración.")
                recomendaciones.add("Revisión del filtro de aire.")
            }
            kilometraje in 100001..150000 -> {
                recomendaciones.add("Reemplazo de bujías y cables.")
                recomendaciones.add("Inspección de la batería y sistema eléctrico.")
                recomendaciones.add("Cambio de aceite (entre los 15,000 y 20,000 km recorridos desde el último cambio).")
            }
            kilometraje > 150000 -> {
                recomendaciones.add("Cambio de correas de distribución y accesorios.")
                recomendaciones.add("Revisión detallada de la suspensión.")
                recomendaciones.add("Cambio de aceite (entre los 15,000 y 20,000 km recorridos desde el último cambio).")
            }
        }

        // Sugerencias adicionales basadas en la antigüedad del vehículo
        if (antiguedad >= 10) {
            recomendaciones.add("Revisión exhaustiva del chasis por corrosión.")
            recomendaciones.add("Inspección de sellos y juntas para evitar fugas.")
        }

        // Mostrar recomendaciones con crearCard
        if (recomendaciones.isNotEmpty()) {
            mantenimientoContainer.addView(
                crearCard(recomendaciones, "Sugerencias de Mantenimiento Preventivo")
            )
        } else {
            mantenimientoContainer.addView(
                crearCard(listOf("No hay recomendaciones de mantenimiento preventivo por ahora."))
            )
        }
    }

    private fun mostrarPresupuestoDeMantenimiento() {
        val presupuesto = MANTENIMIENTOS_PROGRAMADOS.mapNotNull { (servicio, _) ->
            COSTOS_REFERENCIALES[servicio]?.let { costo ->
                "$servicio: $costo CLP"
            }
        }

        if (presupuesto.isNotEmpty()) {
            mantenimientoContainer.addView(
                crearCard(presupuesto, "Presupuesto de Mantenimiento")
            )
        } else {
            mantenimientoContainer.addView(
                crearCard(listOf("No hay costos referenciales disponibles."), "Presupuesto de Mantenimiento")
            )
        }
    }


    private fun configurarAlertasPorKilometraje(kilometraje: Int) {
        val alertas = mutableListOf<String>()

        if (kilometraje % CAMBIO_ACEITE in 14000..14999) {
            alertas.add("El cambio de aceite está próximo: (${kilometraje + (CAMBIO_ACEITE - kilometraje % CAMBIO_ACEITE)} km).")
        }
        if (kilometraje % REVISION_FRENOS in 19000..19999) {
            alertas.add("La revisión de frenos está próxima: (${kilometraje + (REVISION_FRENOS - kilometraje % REVISION_FRENOS)} km).")
        }
        if (kilometraje % FILTRO_AIRE in 14000..14999) {
            alertas.add("La revisión del filtro de aire está próxima: (${kilometraje + (FILTRO_AIRE - kilometraje % FILTRO_AIRE)} km).")
        }
        if (kilometraje % CAMBIO_BUJIAS in 39000..39999) {
            alertas.add("El cambio de bujías está próximo: (${kilometraje + (CAMBIO_BUJIAS - kilometraje % CAMBIO_BUJIAS)} km).")
        }

        if (alertas.isNotEmpty()) {
            mantenimientoContainer.addView(
                crearCard(alertas, "Alertas por Kilometraje")
            )
        }
    }

    private fun configurarAlertasPorTiempo() {
        val sharedPreferences = getSharedPreferences("AutoPreferences", MODE_PRIVATE)
        val selectedAutoId = sharedPreferences.getInt("selected_auto_id", -1)
        val lastMaintenanceDateKey = "last_maintenance_date_$selectedAutoId"
        val lastMaintenanceDate = sharedPreferences.getLong(lastMaintenanceDateKey, 0L)

        if (lastMaintenanceDate == 0L) {
            mantenimientoContainer.addView(
                crearCard(
                    listOf("Por favor, registra la fecha de tu último mantenimiento para este auto."),
                    "Alertas por Tiempo"
                )
            )
            return
        }

        val currentTime = System.currentTimeMillis()
        val diasDesdeUltimoMantenimiento = (currentTime - lastMaintenanceDate) / (1000 * 60 * 60 * 24)
        val alertas = mutableListOf<String>()

        // Crear el canal de notificaciones si no existe
        MantenimientoNotificacionReceiver.createNotificationChannel(this)

        // Alerta si han pasado más de un año
        if (diasDesdeUltimoMantenimiento >= 365) {
            val mensaje = "¡URGENTE! Han pasado más de un año desde el último mantenimiento de este auto. Realiza uno lo antes posible."
            alertas.add(mensaje)

            // Enviar notificación urgente
            MantenimientoNotificacionReceiver.sendNotification(
                context = this,
                notificationId = "mantenimiento_urgente_$selectedAutoId".hashCode(),
                title = "Mantenimiento Atrasado",
                message = mensaje
            )
        }
        // Alerta si han pasado más de 6 meses pero menos de un año
        else if (diasDesdeUltimoMantenimiento >= 180) {
            val mensaje = "Han pasado más de 6 meses desde el último mantenimiento de este auto. Considera realizar uno pronto."
            alertas.add(mensaje)

            // Enviar notificación de advertencia
            MantenimientoNotificacionReceiver.sendNotification(
                context = this,
                notificationId = "mantenimiento_advertencia_$selectedAutoId".hashCode(),
                title = "Recordatorio de Mantenimiento",
                message = mensaje
            )
        }

        // Estimar próximo mantenimiento
        val proximaFecha = lastMaintenanceDate + (180L * 24 * 60 * 60 * 1000) // Próximo mantenimiento en 6 meses
        if (currentTime > proximaFecha) {
            val mensaje = "¡URGENTE! El próximo mantenimiento estimado era para ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(proximaFecha)}. Estás fuera de la fecha recomendada."
            alertas.add(mensaje)

            // Enviar notificación de mantenimiento vencido
            MantenimientoNotificacionReceiver.sendNotification(
                context = this,
                notificationId = "mantenimiento_vencido_$selectedAutoId".hashCode(),
                title = "Mantenimiento Vencido",
                message = mensaje
            )
        } else {
            val fechaEstimada = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(proximaFecha)
            alertas.add("Próximo mantenimiento estimado: $fechaEstimada.")
        }

        if (alertas.isNotEmpty()) {
            mantenimientoContainer.addView(
                crearCard(alertas, "Alertas por Tiempo")
            )
        }
    }

    private fun obtenerEstadoComponentes(): String {
        val sharedPreferences = getSharedPreferences("ComponentesEstado", MODE_PRIVATE)
        val componentes = listOf("Llantas", "Batería", "Luces", "Frenos", "Suspensión")

        return componentes.joinToString("\n") { componente ->
            val estado = sharedPreferences.getString(componente, "No especificado")
            "$componente: $estado"
        }
    }


    private fun registrarMantenimientoRealizado() {
        val sharedPreferences = getSharedPreferences("AutoPreferences", MODE_PRIVATE)
        val lastMaintenanceKm = sharedPreferences.getInt("last_maintenance_km", -1)
        val currentKm = autoSeleccionado["kilometraje"] as? Int ?: 0

        if (currentKm == lastMaintenanceKm) {
            // Evitar doble registro si ya se registró el mantenimiento para este kilometraje
            return
        }

        val editor = sharedPreferences.edit()
        editor.putInt("last_maintenance_km", currentKm)
        editor.putLong("last_maintenance_date", System.currentTimeMillis()) // Registrar la fecha actual
        editor.apply()

        // Preparar los datos para el historial
        val tipoAceite = sharedPreferences.getString("tipo_aceite", "No especificado")
        val fechaUltimaMantencion = System.currentTimeMillis().let {
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(it)
        }
        val estadoComponentes = obtenerEstadoComponentes()
        val proximosServicios = calcularProximosServicios(currentKm).joinToString("\n")
        val cronogramaMantenimiento = calcularCronogramaVisual(currentKm).joinToString("\n")

        // Registrar en la base de datos
        val userManager = UserManager(this)
        val selectedAutoId = sharedPreferences.getInt("selected_auto_id", -1)

        if (selectedAutoId != -1) {
            val datosMantenimiento = """
            Kilometraje Antiguo: $lastMaintenanceKm
            Kilometraje Actual: $currentKm
            Tipo de Aceite: $tipoAceite
            Fecha Última Mantención: $fechaUltimaMantencion
            Estado de Componentes:
            $estadoComponentes

            Próximos Servicios:
            $proximosServicios

            Cronograma de Mantenimiento:
            $cronogramaMantenimiento
        """.trimIndent()

            val resultado = userManager.guardarHistorial(selectedAutoId, datosMantenimiento)
            if (resultado != -1L) {
                android.widget.Toast.makeText(
                    this,
                    "Mantenimiento registrado con éxito.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.widget.Toast.makeText(
                    this,
                    "Error al registrar el mantenimiento.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            android.widget.Toast.makeText(
                this,
                "No se pudo registrar el mantenimiento, ID del auto no encontrado.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        // Lanza la actividad de HistorialMantenimiento con los datos
        val intent = Intent(this, HistorialMantenimiento::class.java).apply {
            putExtra("kilometrajeAntiguo", lastMaintenanceKm)
            putExtra("kilometrajeActual", currentKm)
            putExtra("tipoAceite", tipoAceite)
            putExtra("fechaUltimaMantencion", fechaUltimaMantencion)
            putExtra("estadoComponentes", estadoComponentes)
            putExtra("proximosServicios", proximosServicios)
            putExtra("cronogramaMantenimiento", cronogramaMantenimiento)
        }
        startActivity(intent)

        // Recalcula los mantenimientos
        mantenimientoContainer.removeAllViews()
        actualizarInterfaz()
    }



    private fun agregarBotonRegistrarMantenimiento() {
        val button = Button(this).apply {
            text = "Registrar Mantenimiento Realizado"
            textSize = 18f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 24, 0, 0) // Espaciado extra para el botón final
            }
            setBackgroundResource(android.R.drawable.btn_default)
            setOnClickListener {
                registrarMantenimientoRealizado()
            }
        }

        mantenimientoContainer.addView(button)
    }

    private fun mostrarListaDeTareas() {
        val titulo = TextView(this).apply {
            text = "Tareas Comunes de Mantenimiento"
            textSize = 20f
            setPadding(16, 16, 16, 8)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val tareasContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 24)
            }
            setBackgroundResource(android.R.color.background_light) // Fondo claro
            elevation = 8f
        }

        // Agregar tareas como elementos de texto
        TAREAS_MANTENIMIENTO.forEach { tarea ->
            val tareaTextView = TextView(this).apply {
                text = "- $tarea"
                textSize = 16f
                setPadding(8, 8, 8, 8)
            }
            tareasContainer.addView(tareaTextView)
        }

        // Agregar título y contenedor de tareas al layout principal
        mantenimientoContainer.addView(titulo)
        mantenimientoContainer.addView(tareasContainer)
    }

    private fun mostrarEstadoComponentes() {
        // Lista de componentes a monitorear
        val componentes = listOf(
            "Llantas",
            "Batería",
            "Luces",
            "Frenos",
            "Suspensión"
        )

        // Mapeo de estados y colores para visualización
        val estados = mapOf(
            "Bueno" to android.R.color.holo_green_light,
            "Regular" to android.R.color.holo_orange_light,
            "Malo" to android.R.color.holo_red_light
        )

        val sharedPreferences = getSharedPreferences("ComponentesEstado", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Contenedor de componentes dentro del CardView
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        componentes.forEach { componente ->
            // Layout para cada componente (Texto + Spinner)
            val componenteLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 8, 8, 8)
                }
            }

            // Título del componente
            val textView = TextView(this).apply {
                text = componente
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            // Spinner para seleccionar el estado del componente
            val spinner = android.widget.Spinner(this).apply {
                adapter = android.widget.ArrayAdapter(
                    this@MantenimientoAuto,
                    android.R.layout.simple_spinner_item,
                    estados.keys.toList()
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
            }

            // Recuperar estado guardado para este componente
            val estadoGuardado = sharedPreferences.getString(componente, "Bueno")
            spinner.setSelection(estados.keys.toList().indexOf(estadoGuardado))

            // Aplicar color inicial al fondo según el estado guardado
            val colorInicial = estados[estadoGuardado] ?: android.R.color.white
            componenteLayout.setBackgroundColor(resources.getColor(colorInicial, null))

            // Listener para cambios en el Spinner
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    val estadoSeleccionado = estados.keys.toList()[position]
                    val color = estados[estadoSeleccionado] ?: android.R.color.white
                    componenteLayout.setBackgroundColor(resources.getColor(color, null))

                    // Guardar estado seleccionado en SharedPreferences
                    editor.putString(componente, estadoSeleccionado).apply()

                    // Mostrar advertencia si el estado es "Malo"
                    if (estadoSeleccionado == "Malo") {
                        android.widget.Toast.makeText(
                            this@MantenimientoAuto,
                            "¡Atención! El estado del componente \"$componente\" es $estadoSeleccionado. Considere reemplazarlo pronto.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

            // Agregar texto y spinner al layout del componente
            componenteLayout.addView(textView)
            componenteLayout.addView(spinner, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(32, 0, 0, 0)
            })

            // Agregar el layout del componente al contenedor
            container.addView(componenteLayout)
        }

        // Crear un CardView principal para "Estado de Componentes"
        val estadoComponentesCard = CardView(this@MantenimientoAuto).apply {
            radius = 12f
            cardElevation = 8f
            setContentPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }

            // Título y descripción dentro del CardView
            val cardHeader = TextView(this@MantenimientoAuto).apply {
                text = "Estado de Componentes"
                textSize = 20f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(8, 8, 8, 16)
            }

            val cardDescription = TextView(this@MantenimientoAuto).apply {
                text = "A continuación, se muestra el estado de los principales componentes del vehículo."
                textSize = 16f
                setPadding(8, 0, 8, 16)
            }

            // Contenedor para el header y los componentes
            val cardContent = LinearLayout(this@MantenimientoAuto).apply {
                orientation = LinearLayout.VERTICAL
                addView(cardHeader)
                addView(cardDescription)
                addView(container)
            }

            addView(cardContent)
        }

        // Agregar el CardView principal al contenedor principal de la actividad
        mantenimientoContainer.addView(estadoComponentesCard)
    }

}