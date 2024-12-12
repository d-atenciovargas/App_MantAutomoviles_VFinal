package com.example.login

import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DetallesCodigos : AppCompatActivity() {
    private lateinit var codigosBD: CodigosBD
    private lateinit var textDescripcion: TextView
    private lateinit var textPosiblesCausas: TextView
    private lateinit var textGuiasReparacion: TextView
    private lateinit var textNotasTecnicas: TextView
    private lateinit var textPosiblesCostos: TextView
    private lateinit var textCuandoSeDetecta: TextView
    private lateinit var textPosiblesSintomas: TextView
    private lateinit var titleDetail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalles_codigo)

        // Obtener referencias del contenedor del título y del título
        val titleContainer = findViewById<FrameLayout>(R.id.titleContainer)
        val titleDetail = findViewById<TextView>(R.id.titleDetail)

        // Ajustar el título dinámicamente debajo del notch
        adjustTitleForNotch(titleContainer, titleDetail)

        // Configurar el botón de flecha para regresar
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val source = intent.getStringExtra("source")

        btnBack.setOnClickListener {
            when (source) {
                "ListaCodigos" -> {
                    val intent = Intent(this, ListaCodigos::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                }
                "SimuladorOBD" -> {
                    val intent = Intent(this, SimuladorOBD::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                }
                else -> {
                    finish() // Si no se especifica el origen, simplemente cerrar la actividad
                }
            }
        }

        // Inicializar los TextViews del contenido
        textDescripcion = findViewById(R.id.textDescripcion)
        textPosiblesCausas = findViewById(R.id.textPosiblesCausas)
        textGuiasReparacion = findViewById(R.id.textGuiasReparacion)
        textNotasTecnicas = findViewById(R.id.textNotasTecnicas)
        textPosiblesCostos = findViewById(R.id.textPosiblesCostos)
        textCuandoSeDetecta = findViewById(R.id.textCuandoSeDetecta)
        textPosiblesSintomas = findViewById(R.id.textPosiblesSintomas)

        // Configurar la base de datos
        codigosBD = CodigosBD(this)

        // Cargar los detalles del código recibido
        val codigo = intent.getStringExtra("codigo")
        if (codigo != null) {
            loadDetails(codigo)
        }
    }

    private fun adjustTitleForNotch(titleContainer: FrameLayout, titleDetail: TextView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            titleContainer.setOnApplyWindowInsetsListener { view, insets ->
                val topInset = insets.systemWindowInsetTop
                // Ajusta el padding del contenedor para cubrir el notch
                view.setPadding(
                    view.paddingLeft,
                    topInset,
                    view.paddingRight,
                    view.paddingBottom
                )
                // Ajusta el padding del texto para que quede debajo del notch
                titleDetail.setPadding(
                    titleDetail.paddingLeft,
                    topInset + 16, // Espaciado adicional para alinearlo correctamente
                    titleDetail.paddingRight,
                    titleDetail.paddingBottom
                )
                insets
            }
        }
    }

    private fun loadDetails(codigo: String) {
        val db: SQLiteDatabase = codigosBD.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT descripcion, posibles_causas, guias_reparacion, notas_tecnicas, posibles_costos, cuando_se_detecta, posibles_sintomas FROM codigos_dtc_obd2 WHERE codigo = ?",
            arrayOf(codigo)
        )

        if (cursor.moveToFirst()) {
            // Recuperar datos de la base de datos y asignarlos a los TextViews
            textDescripcion.text = cursor.getString(cursor.getColumnIndexOrThrow("descripcion"))
            textPosiblesCausas.text = cursor.getString(cursor.getColumnIndexOrThrow("posibles_causas"))
            textGuiasReparacion.text = cursor.getString(cursor.getColumnIndexOrThrow("guias_reparacion"))
            textNotasTecnicas.text = cursor.getString(cursor.getColumnIndexOrThrow("notas_tecnicas"))
            textPosiblesCostos.text = cursor.getString(cursor.getColumnIndexOrThrow("posibles_costos"))
            textCuandoSeDetecta.text = cursor.getString(cursor.getColumnIndexOrThrow("cuando_se_detecta"))
            textPosiblesSintomas.text = cursor.getString(cursor.getColumnIndexOrThrow("posibles_sintomas"))
        }
        cursor.close()
    }
}
