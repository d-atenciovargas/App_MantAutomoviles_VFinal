package com.example.login

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.picasso.Picasso
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class Mapa : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userPlacedMarker: Marker? = null // Marcador colocado por el usuario
    private var searchLocationMarker: Marker? = null // Marcador de la dirección seleccionada
    private val currentPolylines = mutableListOf<Polyline>() // Lista de polilíneas para las rutas
    private val workshopMarkers = mutableListOf<Marker>() // Lista para rastrear marcadores de talleres
    private var transportMode: String = "driving"

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val AUTOCOMPLETE_REQUEST_CODE = 2
        private const val RADIUS = 5000 // Radio en metros
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        findViewById<TextView>(R.id.searchLocation).setOnClickListener {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }

        findViewById<ImageButton>(R.id.btnMyLocation).setOnClickListener {
            showCurrentLocation()
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        googleMap.isMyLocationEnabled = true
        showCurrentLocation()

        // Deshabilitar el botón predeterminado de ubicación
        googleMap.uiSettings.isMyLocationButtonEnabled = false

        // Configurar clic en el mapa para agregar marcador del usuario
        googleMap.setOnMapClickListener { latLng ->
            // Eliminar marcador azul de searchLocation si existe
            searchLocationMarker?.remove()
            searchLocationMarker = null

            // Eliminar marcador del usuario previo si existe
            userPlacedMarker?.remove()

            // Eliminar rutas actuales al agregar un nuevo marcador
            currentPolylines.forEach { it.remove() }
            currentPolylines.clear()

            // Agregar nuevo marcador verde del usuario
            userPlacedMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Ubicación seleccionada")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)) // Marcador verde
            )

            // Buscar talleres mecánicos cerca de la ubicación seleccionada
            fetchNearbyWorkshops(latLng)
        }

        // Configurar clic en marcadores para manejar lógica según el marcador
        googleMap.setOnMarkerClickListener { marker ->
            when {
                marker == userPlacedMarker -> {
                    // Eliminar el marcador del usuario si se pulsa
                    marker.remove()
                    userPlacedMarker = null

                    // Eliminar todos los marcadores de talleres mecánicos
                    workshopMarkers.forEach { it.remove() }
                    workshopMarkers.clear()

                    // Eliminar todas las rutas dibujadas
                    currentPolylines.forEach { it.remove() }
                    currentPolylines.clear()

                    Toast.makeText(this, "Marcador eliminado, talleres y rutas borrados", Toast.LENGTH_SHORT).show()
                    true
                }
                workshopMarkers.contains(marker) -> {
                    // Manejar clic en un marcador de taller mecánico
                    val placeId = marker.tag as? String
                    if (placeId != null) {
                        // Llama a fetchPlaceDetails para obtener y mostrar detalles del taller
                        fetchPlaceDetails(placeId)
                    } else {
                        // Si no hay placeId, mostrar detalles básicos
                        showWorkshopDetails(marker)
                    }

                    // Registrar este marcador como destino seleccionado
                    searchLocationMarker = marker

                    // Generar ruta hacia el taller desde el origen actual
                    val origin = when {
                        userPlacedMarker != null -> userPlacedMarker!!.position // Marcador verde (usuario)
                        else -> null // Origen no definido aún
                    }

                    if (origin != null) {
                        fetchRoutes(origin, marker.position)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                val currentLocation = LatLng(location.latitude, location.longitude)
                                fetchRoutes(currentLocation, marker.position)
                            } else {
                                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener {
                            Toast.makeText(this, "Error al obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                else -> false // No hacer nada si el marcador no coincide
            }
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                val place = Autocomplete.getPlaceFromIntent(data!!)
                val latLng = place.latLng

                if (latLng != null) {
                    // Eliminar marcador previo de searchLocation si existe
                    searchLocationMarker?.remove()

                    // Centrar el mapa en la ubicación seleccionada
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                    // Agregar marcador azul para la dirección seleccionada
                    searchLocationMarker = googleMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(place.name)
                            .snippet(place.address)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)) // Marcador azul
                    )

                    // Buscar talleres mecánicos cerca de la dirección seleccionada
                    fetchNearbyWorkshops(latLng)
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Mapa", "Error al procesar la dirección seleccionada: ${e.message}")
                Toast.makeText(this, "Error al procesar la dirección seleccionada", Toast.LENGTH_SHORT).show()
            }
        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            val status = Autocomplete.getStatusFromIntent(data!!)
            Log.e("Mapa", "Error al buscar dirección: ${status.statusMessage}")
            Toast.makeText(this, "Error al buscar dirección", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location: Location? ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)

                googleMap.clear() // Limpia el mapa antes de agregar nuevos elementos
                val circle = googleMap.addCircle(
                    CircleOptions()
                        .center(latLng)
                        .radius(30.0)
                        .strokeColor(android.graphics.Color.BLUE)
                        .strokeWidth(5f)
                        .fillColor(0x3300BFFF)
                )

                pulseCircle(circle) // Llama al método para animar el círculo
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                // Busca talleres mecánicos cerca de la ubicación actual
                fetchNearbyWorkshops(latLng)
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pulseCircle(circle: com.google.android.gms.maps.model.Circle) {
        val animator = ValueAnimator.ofFloat(30f, 50f)
        animator.duration = 1000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.REVERSE

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            circle.radius = animatedValue.toDouble() // Actualiza el radio del círculo dinámicamente
        }

        animator.start()
    }


    private fun fetchNearbyWorkshops(location: LatLng) {
        val apiKey = getString(R.string.google_maps_key)
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=${location.latitude},${location.longitude}" +
                "&radius=$RADIUS" +
                "&type=car_repair" +
                "&key=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        // Elimina los marcadores de talleres mecánicos previamente añadidos
        workshopMarkers.forEach { it.remove() }
        workshopMarkers.clear()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Mapa, "Error al buscar talleres", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    val json = JSONObject(responseData)
                    val results = json.getJSONArray("results")
                    runOnUiThread {
                        for (i in 0 until results.length()) {
                            val place = results.getJSONObject(i)
                            val lat = place.getJSONObject("geometry").getJSONObject("location").getDouble("lat")
                            val lng = place.getJSONObject("geometry").getJSONObject("location").getDouble("lng")
                            val name = place.getString("name")
                            val address = place.optString("vicinity", "Sin dirección")
                            val placeId = place.optString("place_id", "") // Obtener el place_id
                            val targetLocation = LatLng(lat, lng)

                            // Validar si está dentro del rango de 5 km
                            if (isWithinRange(location, targetLocation, RADIUS)) {
                                val marker = googleMap.addMarker(
                                    MarkerOptions()
                                        .position(targetLocation)
                                        .title(name)
                                        .snippet(address)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)) // Marcador rojo para talleres
                                )
                                marker?.tag = placeId // Asignar el place_id como tag del marcador
                                if (marker != null) {
                                    workshopMarkers.add(marker)
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if (result and 1 != 0) {
                (result shr 1).inv()
            } else {
                result shr 1
            }
            lat += dLat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if (result and 1 != 0) {
                (result shr 1).inv()
            } else {
                result shr 1
            }
            lng += dLng

            val p = LatLng((lat / 1E5), (lng / 1E5))
            poly.add(p)
        }

        return poly
    }

    private fun fetchRoutes(origin: LatLng, destination: LatLng) {
        val apiKey = getString(R.string.google_maps_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&alternatives=true" + // Solicita rutas alternativas
                "&key=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Mapa, "Error al obtener rutas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    val json = JSONObject(responseData)
                    val routes = json.getJSONArray("routes")
                    runOnUiThread {
                        // Elimina las rutas previas
                        currentPolylines.forEach { it.remove() }
                        currentPolylines.clear()

                        // Si no hay rutas, muestra un mensaje
                        if (routes.length() == 0) {
                            Toast.makeText(this@Mapa, "No se encontraron rutas disponibles", Toast.LENGTH_SHORT).show()
                            return@runOnUiThread
                        }

                        val boundsBuilder = LatLngBounds.Builder()

                        // Dibuja todas las rutas
                        for (i in 0 until routes.length()) {
                            val route = routes.getJSONObject(i)
                            val overviewPolyline = route.getJSONObject("overview_polyline").getString("points")
                            val decodedPath = decodePolyline(overviewPolyline)

                            // Define colores para las rutas
                            val color = when (i) {
                                0 -> android.graphics.Color.BLUE // Ruta principal (azul sólido)
                                else -> android.graphics.Color.argb(128, 30, 144, 255) // Alternativas (azul traslúcido)
                            }

                            val polylineOptions = PolylineOptions()
                                .addAll(decodedPath)
                                .width(if (i == 0) 10f else 8f) // Ruta principal más gruesa
                                .color(color)
                                .clickable(true) // Permite que la ruta sea clickeable

                            // Añade todos los puntos de la ruta a los límites
                            decodedPath.forEach { boundsBuilder.include(it) }

                            // Agregar la polilínea al mapa y rastrear en currentPolylines
                            val polyline = googleMap.addPolyline(polylineOptions)
                            currentPolylines.add(polyline)
                        }

                        // Ajustar el zoom y centrar el mapa en las rutas
                        try {
                            val bounds = boundsBuilder.build()
                            val padding = 100 // Espacio adicional en los bordes del mapa (en píxeles)
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                        } catch (e: IllegalStateException) {
                            Log.e("Mapa", "Error al ajustar los límites: ${e.message}")
                        }

                        // Configurar manejador de clics para las rutas
                        googleMap.setOnPolylineClickListener { selectedPolyline ->
                            currentPolylines.forEach { polyline ->
                                // Mantén las rutas opcionales traslúcidas y la seleccionada sólida
                                polyline.color = if (polyline == selectedPolyline) {
                                    android.graphics.Color.BLUE // Ruta seleccionada sólida
                                } else {
                                    android.graphics.Color.argb(128, 30, 144, 255) // Rutas opcionales traslúcidas
                                }
                            }

                            // Obtener información de la ruta seleccionada
                            val selectedIndex = currentPolylines.indexOf(selectedPolyline)
                            if (selectedIndex >= 0 && selectedIndex < routes.length()) {
                                val selectedRoute = routes.getJSONObject(selectedIndex)
                                calculateRouteDistanceAndTime(selectedRoute) // Llama a la nueva función
                            }
                        }
                    }
                }
            }
        })
    }


    private fun showPlaceDetails(
        name: String,
        rating: Double,
        address: String,
        phone: String,
        openingHours: JSONArray?,
        photos: JSONArray?,
        reviews: JSONArray? = null,
        website: String? = null
    ) {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_layout, null)

        val locationTitle: TextView = bottomSheetView.findViewById(R.id.location_title)
        val locationDetails: TextView = bottomSheetView.findViewById(R.id.location_details)
        val ratingBar: RatingBar = bottomSheetView.findViewById(R.id.rating_bar)
        val ratingText: TextView = bottomSheetView.findViewById(R.id.rating_text)
        val openingHoursText: TextView = bottomSheetView.findViewById(R.id.opening_hours)
        val phoneNumberText: TextView = bottomSheetView.findViewById(R.id.phone_number)
        val websiteUrlText: TextView = bottomSheetView.findViewById(R.id.website_url)
        val imageContainer: LinearLayout = bottomSheetView.findViewById(R.id.image_container)
        val reviewsLabel: TextView = bottomSheetView.findViewById(R.id.reviews_label)
        val reviewsContainer: LinearLayout = bottomSheetView.findViewById(R.id.reviews_container)
        val callButton: Button = bottomSheetView.findViewById(R.id.btn_call)
        val websiteButton: Button = bottomSheetView.findViewById(R.id.btn_website)

        // Setear datos generales
        locationTitle.text = name
        locationDetails.text = address
        ratingBar.numStars = 5
        ratingBar.stepSize = 0.1f
        ratingBar.setProgress((rating * 10).toInt())
        ratingBar.progressTintList = resources.getColorStateList(android.R.color.holo_orange_light, null) // Estrellas en amarillo
        ratingText.text = "Rating: $rating"
        openingHoursText.text = openingHours?.join("\n") ?: "Horario no disponible"
        phoneNumberText.text = "📞 $phone"
        websiteUrlText.text = if (!website.isNullOrEmpty()) "🌐 $website" else "🌐 Página web no disponible"

        // Limpieza del contenedor de imágenes y llenado con nuevas fotos
        imageContainer.removeAllViews()
        photos?.let {
            for (i in 0 until it.length()) {
                val photoReference = it.getJSONObject(i).getString("photo_reference")
                val imageUrl =
                    "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=$photoReference&key=${getString(R.string.google_maps_key)}"

                val imageView = ImageView(this)
                imageView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    300 // Altura fija para las imágenes
                )
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                Picasso.get().load(imageUrl).into(imageView)
                imageContainer.addView(imageView)
            }
        }

        // Limpieza y llenado del contenedor de opiniones
        reviewsContainer.removeAllViews()
        reviews?.let {
            for (i in 0 until it.length()) {
                val review = it.getJSONObject(i)
                val reviewerName = review.optString("author_name", "Anónimo")
                val reviewText = review.optString("text", "Sin comentario")
                val ratingValue = review.optDouble("rating", 0.0)

                val reviewLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 8, 16, 8)
                    setBackgroundResource(R.drawable.review_background)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 8, 0, 8) }
                }

                val nameView = TextView(this).apply {
                    text = reviewerName
                    textSize = 16f
                    setTextColor(resources.getColor(android.R.color.black, null))
                }

                val ratingView = RatingBar(this).apply {
                    numStars = 5
                    stepSize = 0.1f
                    setIsIndicator(true)
                    setProgress((ratingValue * 10).toInt()) // Progreso basado en el valor de la calificación
                    scaleX = 0.8f
                    scaleY = 0.8f
                    progressTintList = resources.getColorStateList(android.R.color.holo_orange_light, null) // Estrellas en amarillo
                }

                val commentView = TextView(this).apply {
                    text = reviewText
                    textSize = 14f
                    setTextColor(resources.getColor(android.R.color.black, null))
                }

                reviewLayout.addView(nameView)
                reviewLayout.addView(ratingView)
                reviewLayout.addView(commentView)
                reviewsContainer.addView(reviewLayout)
            }
        }

        // Ocultar inicialmente las opiniones
        reviewsContainer.visibility = View.GONE

        // Manejo del clic en el encabezado de Opiniones
        reviewsLabel.setOnClickListener {
            if (reviewsContainer.visibility == View.GONE) {
                reviewsContainer.visibility = View.VISIBLE
            } else {
                reviewsContainer.visibility = View.GONE
            }
        }

        // Manejo de los botones de acción
        callButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phone") }
            startActivity(intent)
        }

        if (!website.isNullOrEmpty()) {
            websiteButton.visibility = View.VISIBLE
            websiteButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(website) }
                startActivity(intent)
            }
        } else {
            websiteButton.visibility = View.GONE
        }

        val startNavigationButton: Button = bottomSheetView.findViewById(R.id.btn_start_navigation)
        startNavigationButton.setOnClickListener {
            val destinationLatLng = searchLocationMarker?.position
            if (destinationLatLng != null) {
                val gmmIntentUri = Uri.parse("google.navigation:q=${destinationLatLng.latitude},${destinationLatLng.longitude}&mode=d")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    Toast.makeText(this, "Google Maps no está disponible", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No se ha seleccionado un destino", Toast.LENGTH_SHORT).show()
            }
        }


        // Mostrar el BottomSheet
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }


    // Detectar si el país es hispanohablante
    private fun isSpanishSpeakingCountry(address: String): Boolean {
        val spanishSpeakingCountries = listOf("España", "México", "Argentina", "Chile", "Colombia", "Perú", "Ecuador", "Venezuela", "Guatemala")
        return spanishSpeakingCountries.any { address.contains(it, ignoreCase = true) }
    }

    // Método para traducir al español
    private fun translateToSpanish(text: String): String {
        // Simula traducción por ahora; implementa con la API de Google Translate para producción.
        return "Traducción (ES): $text"
    }


    private fun fetchPlaceDetails(placeId: String) {
        val apiKey = getString(R.string.google_maps_key)
        val url = "https://maps.googleapis.com/maps/api/place/details/json" +
                "?place_id=$placeId" +
                "&fields=name,rating,formatted_address,formatted_phone_number,opening_hours,photos,website,reviews" +
                "&key=$apiKey" // Nota: He agregado "reviews" al parámetro fields para obtenerlas desde la API.

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Mapa, "Error al obtener detalles del lugar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    val json = JSONObject(responseData)
                    val result = json.getJSONObject("result")

                    val name = result.optString("name", "Sin nombre")
                    val rating = result.optDouble("rating", 0.0)
                    val address = result.optString("formatted_address", "Sin dirección")
                    val phone = result.optString("formatted_phone_number", "Sin teléfono")
                    val openingHours = result.optJSONObject("opening_hours")?.optJSONArray("weekday_text")
                    val photos = result.optJSONArray("photos")
                    val reviews = result.optJSONArray("reviews") // Extraer las opiniones
                    val website = result.optString("website", "")

                    runOnUiThread {
                        showPlaceDetails(
                            name = name,
                            rating = rating,
                            address = address,
                            phone = phone,
                            openingHours = openingHours,
                            photos = photos,
                            reviews = reviews, // Pasar las opiniones aquí
                            website = website
                        )
                    }
                }
            }
        })
    }



    private fun showWorkshopDetails(marker: Marker) {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_layout, null)
        val locationTitle: TextView = bottomSheetView.findViewById(R.id.location_title)
        val locationDetails: TextView = bottomSheetView.findViewById(R.id.location_details)
        val callButton: Button = bottomSheetView.findViewById(R.id.btn_call)

        locationTitle.text = marker.title
        locationDetails.text = marker.snippet
        callButton.setOnClickListener {
            val phone = marker.tag as? String ?: "Sin teléfono"
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }
            startActivity(intent)
        }

        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    private fun isWithinRange(currentLocation: LatLng, targetLocation: LatLng, maxDistance: Int): Boolean {
        val result = FloatArray(1)
        Location.distanceBetween(
            currentLocation.latitude, currentLocation.longitude,
            targetLocation.latitude, targetLocation.longitude,
            result
        )
        return result[0] <= maxDistance
    }

    private fun calculateRouteDistanceAndTime(route: JSONObject) {
        try {
            val legs = route.getJSONArray("legs").getJSONObject(0)
            val distance = legs.getJSONObject("distance").getString("text")
            val duration = legs.getJSONObject("duration").getString("text")

            // Muestra un mensaje con los detalles de la ruta
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Distancia: $distance, Tiempo estimado: $duration",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e("Mapa", "Error al calcular distancia y tiempo: ${e.message}")
        }
    }



    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}