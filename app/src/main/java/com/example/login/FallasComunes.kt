package com.example.login

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import org.jsoup.Jsoup
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class FallasComunes : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fallas_comunes)

        // Copiar la base de datos si es necesario
        copyDatabaseIfNeeded()

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            val intent = Intent(this, Menu::class.java)
            startActivity(intent)
            finish()
        }

        val contenedorFallas = findViewById<LinearLayout>(R.id.contenedor_fallas)

        val sharedPreferences = getSharedPreferences("AutoPreferences", MODE_PRIVATE)
        val autoSeleccionado = UserManager(this).getAutoById(
            sharedPreferences.getInt("selected_auto_id", -1)
        ) ?: mapOf()

        val marca = autoSeleccionado["marca"]?.toString() ?: "Desconocido"
        val modelo = autoSeleccionado["modelo"]?.toString() ?: "Desconocido"

        val loadingCard = crearTextoOrdenado(
            "Cargando fallas específicas para $marca $modelo",
            "Espere un momento..."
        )
        contenedorFallas.addView(loadingCard)

        buscarDiagnosticosEnInternet(marca, modelo) { resultados ->
            contenedorFallas.removeView(loadingCard)

            if (resultados.isNotEmpty()) {
                val tituloEspecificas = TextView(this).apply {
                    text = "Fallas específicas para $marca $modelo"
                    textSize = 20f
                    setPadding(16, 16, 16, 8)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                contenedorFallas.addView(tituloEspecificas)

                resultados.forEach { (titulo, descripcion, url) ->
                    val diagnosticoCard = crearCardDesplegableConEnlace(titulo, descripcion, url)
                    contenedorFallas.addView(diagnosticoCard)
                }
            } else {
                contenedorFallas.addView(
                    crearTextoOrdenado(
                        "Sin resultados específicos",
                        "No se encontraron fallas específicas para este auto."
                    )
                )
            }

            cargarFallasComunes(contenedorFallas)
        }
    }


    private fun copyDatabaseIfNeeded() {
        val databaseName = "Fallas_comunes.db"
        val databasePath = getDatabasePath(databaseName)

        // Si la base de datos ya existe, no hacemos nada
        if (databasePath.exists()) return

        // Crear la carpeta de bases de datos si no existe
        databasePath.parentFile?.mkdirs()

        // Copiar la base de datos desde assets
        try {
            val inputStream: InputStream = assets.open(databaseName)
            val outputStream: OutputStream = FileOutputStream(databasePath)

            val buffer = ByteArray(1024)
            var length: Int

            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            Log.i("Database", "Base de datos copiada exitosamente a: ${databasePath.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DatabaseError", "Error al copiar la base de datos: ${e.message}")
        }
    }


    private fun crearCardDesplegableConEnlace(titulo: String, descripcion: String, url: String): CardView {
        val cardView = CardView(this).apply {
            radius = 16f
            cardElevation = 10f
            setCardBackgroundColor(ContextCompat.getColor(this@FallasComunes, android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val tituloButton = Button(this).apply {
            text = titulo
            textSize = 16f
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 0) }
            setBackgroundResource(android.R.drawable.btn_default)
        }

        val descripcionTextView = TextView(this).apply {
            text = descripcion
            textSize = 14f
            setPadding(16, 8, 16, 16)
            visibility = View.GONE
        }

        val enlaceTextView = TextView(this).apply {
            text = if (url.isNotEmpty() && url != "URL no disponible") "Ver más: $url" else "URL no disponible"
            textSize = 14f
            setPadding(16, 8, 16, 16)
            setTextColor(ContextCompat.getColor(this@FallasComunes, android.R.color.holo_blue_dark))
            visibility = View.GONE

            if (url.isNotEmpty() && url != "URL no disponible") {
                setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("URL_ERROR", "Error al abrir la URL: ${e.message}")
                        android.widget.Toast.makeText(
                            this@FallasComunes,
                            "No se pudo abrir el enlace.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        tituloButton.setOnClickListener {
            val newVisibility = if (descripcionTextView.visibility == View.GONE) View.VISIBLE else View.GONE
            descripcionTextView.visibility = newVisibility
            enlaceTextView.visibility = newVisibility
        }

        contentLayout.addView(tituloButton)
        contentLayout.addView(descripcionTextView)
        contentLayout.addView(enlaceTextView)
        cardView.addView(contentLayout)

        return cardView
    }

    private fun buscarDiagnosticosEnInternet(marca: String, modelo: String, callback: (List<Triple<String, String, String>>) -> Unit) {
        val query = "$marca $modelo fallas comunes problemas frecuentes"
        Thread {
            try {
                val url = "https://www.google.com/search?q=$query"
                val document = Jsoup.connect(url).userAgent("Mozilla/5.0").get()
                val resultados = mutableListOf<Triple<String, String, String>>()

                val elementosTitulos = document.select("h3")
                val elementosDescripciones = document.select("div.BNeawe.s3v9rd.AP7Wnd")
                val elementosEnlaces = document.select("a[href]")

                for (i in 0 until elementosTitulos.size.coerceAtMost(5)) {
                    val titulo = elementosTitulos.getOrNull(i)?.text() ?: "Título no disponible"
                    val descripcion = elementosDescripciones.getOrNull(i)?.text() ?: "Descripción no disponible"
                    val enlace = elementosEnlaces.getOrNull(i)?.attr("href")?.split("/url?q=")?.getOrNull(1)?.split("&")?.getOrNull(0)
                        ?: "URL no disponible"

                    resultados.add(Triple(titulo, descripcion, enlace))
                }

                if (resultados.isEmpty()) {
                    resultados.add(Triple("Información no disponible", "No se encontraron diagnósticos específicos para este vehículo.", ""))
                }

                runOnUiThread { callback(resultados) }
            } catch (e: Exception) {
                runOnUiThread {
                    callback(
                        listOf(
                            Triple(
                                "Error de conexión",
                                "No se pudieron recuperar los diagnósticos: ${e.localizedMessage}",
                                ""
                            )
                        )
                    )
                }
            }
        }.start()
    }

    private fun cargarFallasComunes(contenedor: LinearLayout) {
        val fallas = cargarFallasDesdeBaseDeDatos()

        if (fallas.isNotEmpty()) {
            val tituloComunes = TextView(this).apply {
                text = "Fallas Comunes Generales"
                textSize = 20f
                setPadding(16, 16, 16, 8)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            contenedor.addView(tituloComunes)

            fallas.forEach { falla ->
                val cardView = crearCardFalla(falla)
                contenedor.addView(cardView)
            }
        } else {
            val noDataView = TextView(this).apply {
                text = "No se encontraron datos sobre fallas comunes."
                textSize = 18f
                setPadding(16, 16, 16, 16)
            }
            contenedor.addView(noDataView)
        }
    }

    private fun cargarFallasDesdeBaseDeDatos(): List<Map<String, String>> {
        val fallas = mutableListOf<Map<String, String>>()
        try {
            val dbPath = getDatabasePath("Fallas_comunes.db").absolutePath
            val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT * FROM fallas_comunes", null)

            if (cursor.moveToFirst()) {
                do {
                    val falla = mapOf(
                        "falla_mecanica" to cursor.getString(cursor.getColumnIndexOrThrow("falla_mecanica")),
                        "sintoma" to cursor.getString(cursor.getColumnIndexOrThrow("sintoma")),
                        "causa" to cursor.getString(cursor.getColumnIndexOrThrow("causa")),
                        "solucion" to cursor.getString(cursor.getColumnIndexOrThrow("solucion")),
                        "sugerencia" to cursor.getString(cursor.getColumnIndexOrThrow("sugerencia"))
                    )
                    fallas.add(falla)
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DatabaseError", "Error al cargar datos: ${e.message}")
        }
        return fallas
    }

    private fun crearCardFalla(falla: Map<String, String>): CardView {
        val cardView = CardView(this).apply {
            radius = 16f
            cardElevation = 10f
            setCardBackgroundColor(ContextCompat.getColor(this@FallasComunes, android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val fallaMecanicaView = crearTextoOrdenado(
            titulo = "⚙ Falla",
            contenido = falla["falla_mecanica"] ?: "No disponible",
            isBold = true,
            textSize = 21f
        )

        val sintomaView = crearTextoOrdenado(
            titulo = "💡 Síntoma",
            contenido = falla["sintoma"] ?: "No disponible"
        )

        val causaView = crearTextoOrdenado(
            titulo = "🔍 Causa",
            contenido = falla["causa"] ?: "No disponible"
        )

        val solucionView = crearTextoOrdenado(
            titulo = "🛠 Solución",
            contenido = falla["solucion"] ?: "No disponible"
        )

        val sugerenciaView = crearTextoOrdenado(
            titulo = "📌 Sugerencia",
            contenido = falla["sugerencia"] ?: "No disponible"
        )

        contentLayout.addView(fallaMecanicaView)
        contentLayout.addView(sintomaView)
        contentLayout.addView(causaView)
        contentLayout.addView(solucionView)
        contentLayout.addView(sugerenciaView)

        cardView.addView(contentLayout)
        return cardView
    }

    private fun crearTextoOrdenado(
        titulo: String,
        contenido: String,
        isBold: Boolean = false,
        textSize: Float = 17f
    ): TextView {
        return TextView(this).apply {
            text = "$titulo: $contenido"
            this.textSize = textSize
            setTextColor(ContextCompat.getColor(this@FallasComunes, android.R.color.black))
            setPadding(0, 8, 0, 8)
            if (isBold) {
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }
    }
}
