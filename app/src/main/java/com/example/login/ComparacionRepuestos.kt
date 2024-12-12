package com.example.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class ComparacionRepuestos : AppCompatActivity() {

    private lateinit var contenedorPaginas: LinearLayout

    private val CX = "d61777209a9e54d67" // Reemplaza con tu CSE ID
    private val API_KEY = "AIzaSyCgkNxA9ABOkixCGgJecMPiuk4TGsn133I" // Reemplaza con tu API Key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comparacion_repuestos)

        // Configurar botón de regreso
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // Inicializar contenedor
        contenedorPaginas = findViewById(R.id.contenedor_paginas_repuestos)

        // Obtener los datos del auto seleccionado
        cargarDatosDelAuto()
    }

    private fun cargarDatosDelAuto() {
        val sharedPreferences = getSharedPreferences("AutoPreferences", MODE_PRIVATE)
        val selectedAutoId = sharedPreferences.getInt("selected_auto_id", -1)

        if (selectedAutoId != -1) {
            val userManager = UserManager(this)
            val auto = userManager.getAutoById(selectedAutoId)
            auto?.let {
                val marca = it["marca"].toString()
                val modelo = it["modelo"].toString()
                val ano = it["ano"].toString()

                // Llamar a la API para obtener los datos
                obtenerDatosDeRepuestos(marca, modelo, ano)
            }
        } else {
            mostrarMensaje("No se ha seleccionado un vehículo.")
        }
    }

    private fun obtenerDatosDeRepuestos(marca: String, modelo: String, ano: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(GoogleCustomSearchAPI::class.java)

        val query = "repuestos $marca $modelo $ano"
        api.buscarTiendas(query, CX, API_KEY).enqueue(object : Callback<CustomSearchResponse> {
            override fun onResponse(
                call: Call<CustomSearchResponse>,
                response: Response<CustomSearchResponse>
            ) {
                if (response.isSuccessful) {
                    val items = response.body()?.items ?: emptyList()
                    if (items.isNotEmpty()) {
                        mostrarResultados(items)
                    } else {
                        Log.e("API Response", "No items found.")
                        mostrarMensaje("No se encontraron tiendas para este vehículo.")
                    }
                } else {
                    Log.e("API Error", "Response code: ${response.code()}, message: ${response.message()}")
                    mostrarMensaje("Error al obtener resultados de búsqueda.")
                }
            }

            override fun onFailure(call: Call<CustomSearchResponse>, t: Throwable) {
                Log.e("API Failure", "Error: ${t.message}")
                mostrarMensaje("Error de conexión: ${t.message}")
            }
        })
    }

    private fun mostrarResultados(items: List<SearchItem>) {
        contenedorPaginas.removeAllViews()

        // Encabezado antes de los resultados
        val headerView = TextView(this).apply {
            text = "A continuación, se entrega un listado de páginas para cotizar y barajar precios para su auto:"
            textSize = 18f // Tamaño del texto estilo párrafo
            setTypeface(null, android.graphics.Typeface.BOLD) // Texto en negrita
            setTextColor(resources.getColor(android.R.color.black))
            setPadding(8, 16, 8, 24) // Márgenes laterales más pequeños
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        contenedorPaginas.addView(headerView)

        items.forEach { item ->
            val card = crearCardResultado(item)
            contenedorPaginas.addView(card)
        }
    }



    private fun crearCardResultado(item: SearchItem): CardView {
        val cardView = CardView(this).apply {
            radius = 24f // Bordes más redondeados
            cardElevation = 10f
            setCardBackgroundColor(resources.getColor(android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 12, 16, 12) // Más márgenes laterales
            }
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24) // Espaciado interno amplio
        }

        // Título de la tienda
        val titleView = TextView(this).apply {
            text = item.title
            textSize = 20f // Título destacado
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(android.R.color.holo_blue_dark))
            setPadding(0, 0, 0, 12)
        }

        // Descripción breve
        val snippetView = TextView(this).apply {
            text = item.snippet
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.darker_gray))
            setPadding(0, 0, 0, 12)
        }

        // Botón de enlace
        val linkView = TextView(this).apply {
            text = "🔗 Visitar tienda"
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.holo_blue_light))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                startActivity(browserIntent)
            }
        }

        contentLayout.addView(titleView)
        contentLayout.addView(snippetView)
        contentLayout.addView(linkView)
        cardView.addView(contentLayout)

        return cardView
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}

data class CustomSearchResponse(
    @SerializedName("items") val items: List<SearchItem>?
)

data class SearchItem(
    @SerializedName("title") val title: String,
    @SerializedName("link") val link: String,
    @SerializedName("snippet") val snippet: String
)

interface GoogleCustomSearchAPI {
    @GET("customsearch/v1")
    fun buscarTiendas(
        @Query("q") query: String,
        @Query("cx") cx: String,
        @Query("key") apiKey: String
    ): Call<CustomSearchResponse>
}
