package com.example.login

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat

class MantenimientoNotificacionReceiver : BroadcastReceiver() {

    companion object {
        // Crear el canal de notificaciones (llamar solo una vez al inicio)
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(NotificationManager::class.java)
                val channel = NotificationChannel(
                    MantenimientoAuto.CANAL_ID, // Uso de la constante desde MantenimientoAuto
                    MantenimientoAuto.CANAL_NOMBRE, // Uso de la constante desde MantenimientoAuto
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = MantenimientoAuto.CANAL_DESCRIPCION // Uso de la constante desde MantenimientoAuto
                }
                notificationManager?.createNotificationChannel(channel)
            }
        }

        // Programar una notificación con AlarmManager
        fun scheduleNotification(
            context: Context,
            notificationId: Int,
            service: String,
            triggerAtMillis: Long,
            tipoNotificacion: String = "mantenimiento" // Tipo por defecto: "mantenimiento"
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                Log.e("MantenimientoReceiver", "AlarmManager no inicializado.")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e("MantenimientoReceiver", "La app no tiene permiso para programar alarmas exactas.")
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    context.startActivity(intent)
                    return
                }
            }

            val intent = Intent(context, MantenimientoNotificacionReceiver::class.java).apply {
                putExtra("servicio", service)
                putExtra("tipo_notificacion", tipoNotificacion) // Agrega el tipo de notificación
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
                Log.d("MantenimientoReceiver", "Notificación programada para $triggerAtMillis")
            } catch (e: SecurityException) {
                Log.e("MantenimientoReceiver", "Error al programar la notificación: ${e.message}")
            }
        }

        // Cancelar una notificación programada
        fun cancelScheduledNotification(context: Context, notificationId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                Log.e("MantenimientoReceiver", "AlarmManager no inicializado.")
                return
            }

            val intent = Intent(context, MantenimientoNotificacionReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                Log.d("MantenimientoReceiver", "Notificación cancelada con ID $notificationId")
            } else {
                Log.w("MantenimientoReceiver", "No se encontró PendingIntent para ID $notificationId")
            }
        }

        // Enviar una notificación inmediata
        fun sendNotification(context: Context, notificationId: Int, title: String, message: String) {
            // Mostrar notificación en la barra de notificaciones
            val notificationManager =
                context.getSystemService(NotificationManager::class.java)

            val notification = NotificationCompat.Builder(context, MantenimientoAuto.CANAL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build()

            notificationManager?.notify(notificationId, notification)
            Log.d("MantenimientoReceiver", "Notificación enviada con ID $notificationId")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Extraer información del intent
        val servicio = intent.getStringExtra("servicio")
        val tipoNotificacion = intent.getStringExtra("tipo_notificacion") ?: "mantenimiento"
        val mensajePorDefecto = when (tipoNotificacion) {
            "mantenimiento" -> "Mantenimiento pendiente"
            "vencimiento" -> "Vencimiento próximo"
            else -> "Notificación"
        }
        val mensaje = servicio ?: mensajePorDefecto

        // Determinar el título de la notificación basado en el tipo
        val titulo = when (tipoNotificacion) {
            "mantenimiento" -> "Recordatorio de Mantenimiento"
            "vencimiento" -> "Recordatorio de Vencimiento"
            else -> "Notificación General"
        }

        // Generar un ID único para cada tipo de notificación
        val notificationId = when (tipoNotificacion) {
            "mantenimiento" -> 1
            "vencimiento" -> 2
            else -> 3
        }

        // Llama a la función para enviar la notificación
        sendNotification(
            context,
            notificationId = notificationId,
            title = titulo,
            message = mensaje
        )
    }
}
