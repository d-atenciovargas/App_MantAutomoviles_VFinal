package com.example.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException

class ListaCodigos : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var codigosBD: CodigosBD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_codigos)

        // Ajustar el título automáticamente debajo del notch
        val titleContainer = findViewById<FrameLayout>(R.id.titleContainer)
        val titleCodigos = findViewById<TextView>(R.id.titleCodigos)
        adjustTitleForNotch(titleContainer, titleCodigos)

        // Configurar el botón de flecha para regresar a Menus
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            val intent = Intent(this, Menu::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish() // Finalizar esta actividad para evitar acumulación en el stack
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        codigosBD = CodigosBD(this)
        try {
            codigosBD.createDatabase()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        loadCodigos()
    }

    private fun loadCodigos() {
        val codigos = mutableListOf<String>()
        val db = codigosBD.readableDatabase

        val cursor = db.rawQuery("SELECT codigo FROM codigos_dtc_obd2", null)
        if (cursor.moveToFirst()) {
            do {
                codigos.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()

        val adapter = CodigoAdapter(codigos) { codigo ->
            val intent = Intent(this@ListaCodigos, DetallesCodigos::class.java)
            intent.putExtra("codigo", codigo)
            intent.putExtra("source", "ListaCodigos") // Pasar el origen
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }

    private fun adjustTitleForNotch(titleContainer: FrameLayout, titleCodigos: TextView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            titleContainer.setOnApplyWindowInsetsListener { view, insets ->
                val topInset = insets.systemWindowInsetTop
                // Ajusta el padding del contenedor para que el fondo cubra el notch
                view.setPadding(
                    view.paddingLeft,
                    topInset,
                    view.paddingRight,
                    view.paddingBottom
                )
                // Ajusta el padding del texto para que quede debajo del notch
                titleCodigos.setPadding(
                    titleCodigos.paddingLeft,
                    topInset + 16, // Espaciado adicional debajo del notch
                    titleCodigos.paddingRight,
                    titleCodigos.paddingBottom
                )
                insets
            }
        }
    }
}
