package com.example.login

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ForoComunitario : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val posts: MutableList<Map<String, Any>> = mutableListOf()
    private lateinit var userManager: UserManager
    private lateinit var currentUserEmail: String
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_foro_comunitario)

        // Inicializar UserManager
        userManager = UserManager(this)

        // Obtener el correo del usuario actual desde SharedPreferences
        val sharedPreferences = getSharedPreferences("LoginPreferences", MODE_PRIVATE)
        currentUserEmail = sharedPreferences.getString("user_email", null) ?: ""
        currentUserId = userManager.getUserIdByEmail(currentUserEmail)

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PostAdapter(posts, ::eliminarPost, ::agregarRespuesta, ::eliminarRespuesta, currentUserId)

        // Configuración de clics en la barra de navegación
        val backArrow = findViewById<ImageView>(R.id.nav_back_arrow)
        backArrow.setOnClickListener {
            val intent = Intent(this, Menu::class.java)
            startActivity(intent)
            finish() // Cierra la actividad actual
        }

        val addPost = findViewById<ImageView>(R.id.fab_add_post)
        addPost.setOnClickListener { mostrarDialogAgregarPost() }

        val whatsappSupport = findViewById<ImageView>(R.id.fab_whatsapp)
        whatsappSupport.setOnClickListener {
            val phoneNumber = "+56999373603" // Número de soporte técnico
            try {
                // Crear la URL para el chat de WhatsApp
                val url = "https://wa.me/${phoneNumber.replace("+", "")}?text=" +
                        Uri.encode("¡Bienvenido al Soporte Técnico de DriveDuty! ¿En qué puedo ayudarte?")

                // Intent para abrir WhatsApp
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }

        // Cargar publicaciones iniciales
        cargarPosts()
    }

    private fun cargarPosts() {
        posts.clear()
        val allPosts = userManager.getAllPosts()
        for (post in allPosts) {
            val postId = post["post_id"] as Int
            val responses = userManager.getResponsesByPostId(postId) // Ya incluye el correo
            val postWithResponses = post.toMutableMap()
            postWithResponses["responses"] = responses
            posts.add(postWithResponses)
        }
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun mostrarDialogAgregarPost() {
        val dialogBuilder = AlertDialog.Builder(this)

        // Crear un LinearLayout para el diseño
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 30)
            setBackgroundColor(resources.getColor(android.R.color.white))
        }

        // Título del diálogo
        val titleText = TextView(this).apply {
            text = "Nueva Publicación"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(android.R.color.black))
            setPadding(0, 0, 0, 24)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        // Campo de texto para escribir la publicación
        val editText = EditText(this).apply {
            hint = "Escribe tu publicación aquí..."
            setPadding(24, 24, 24, 24)
            setBackgroundResource(android.R.drawable.edit_text)
            setTextColor(resources.getColor(android.R.color.black))
            setHintTextColor(resources.getColor(android.R.color.darker_gray))
            textSize = 16f
        }

        // Contenedor para los botones
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            weightSum = 2f
            setPadding(0, 24, 0, 0)
        }

        // Botón "Cancelar"
        val cancelButton = Button(this).apply {
            text = "Cancelar"
            setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
            setTextColor(resources.getColor(android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 8, 0)
            }
            setPadding(16, 8, 16, 8)
        }

        // Botón "Publicar"
        val publishButton = Button(this).apply {
            text = "Publicar"
            setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
            setTextColor(resources.getColor(android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(8, 0, 0, 0)
            }
            setPadding(16, 8, 16, 8)
        }

        // Añadir vistas al contenedor principal
        dialogLayout.addView(titleText)
        dialogLayout.addView(editText)
        buttonContainer.addView(cancelButton)
        buttonContainer.addView(publishButton)
        dialogLayout.addView(buttonContainer)

        // Crear y mostrar el diálogo
        val dialog = dialogBuilder.setView(dialogLayout).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // Configurar acciones de los botones
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        publishButton.setOnClickListener {
            val contenido = editText.text.toString()
            if (contenido.isNotEmpty()) {
                agregarPost(contenido)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "La publicación no puede estar vacía", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogResponder(postId: Int, onResponderClick: (Int, String) -> Unit) {
        val dialogBuilder = AlertDialog.Builder(this)

        // Crear un LinearLayout para el diseño
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 30)
            setBackgroundColor(resources.getColor(android.R.color.white))
        }

        // Título del diálogo
        val titleText = TextView(this).apply {
            text = "Responder"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD) // Texto en negrita
            setTextColor(resources.getColor(android.R.color.black))
            setPadding(0, 0, 0, 24)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        // Campo de texto para escribir la respuesta
        val editText = EditText(this).apply {
            hint = "Escribe tu respuesta aquí..."
            setPadding(24, 24, 24, 24)
            setBackgroundResource(android.R.drawable.edit_text)
            setTextColor(resources.getColor(android.R.color.black))
            setHintTextColor(resources.getColor(android.R.color.darker_gray))
            textSize = 16f
        }

        // Contenedor para los botones
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            weightSum = 2f
            setPadding(0, 24, 0, 0)
        }

        // Botón "Cancelar"
        val cancelButton = Button(this).apply {
            text = "Cancelar"
            setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
            setTextColor(resources.getColor(android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 8, 0) // Margen a la derecha
            }
            setPadding(16, 8, 16, 8)
        }

        // Botón "Responder"
        val respondButton = Button(this).apply {
            text = "Responder"
            setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
            setTextColor(resources.getColor(android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(8, 0, 0, 0) // Margen a la izquierda
            }
            setPadding(16, 8, 16, 8)
        }

        // Añadir vistas al contenedor principal
        dialogLayout.addView(titleText)
        dialogLayout.addView(editText)
        buttonContainer.addView(cancelButton)
        buttonContainer.addView(respondButton)
        dialogLayout.addView(buttonContainer)

        // Crear y mostrar el diálogo
        val dialog = dialogBuilder.setView(dialogLayout).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        // Configurar acciones de los botones
        cancelButton.setOnClickListener {
            dialog.dismiss() // Cierra el diálogo al presionar "Cancelar"
        }
        respondButton.setOnClickListener {
            val contenido = editText.text.toString()
            if (contenido.isNotEmpty()) {
                onResponderClick(postId, contenido)
                dialog.dismiss() // Cierra el diálogo al responder
            } else {
                Toast.makeText(this, "La respuesta no puede estar vacía", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun agregarPost(contenido: String) {
        val postId = userManager.addPost(currentUserId, contenido)
        if (postId != -1L) {
            val nuevoPost = userManager.getPostById(postId.toInt())
            if (nuevoPost != null) {
                posts.add(0, nuevoPost)
                recyclerView.adapter?.notifyItemInserted(0)
                recyclerView.scrollToPosition(0)
                Toast.makeText(this, "Publicación agregada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error al cargar el post", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Error al agregar la publicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun agregarRespuesta(postId: Int, contenido: String) {
        val usuario = userManager.getUserByEmail(currentUserEmail)
        if (usuario != null) {
            val respuestaId = userManager.addResponse(postId, currentUserId, contenido)
            if (respuestaId != -1L) {
                val respuesta = mapOf(
                    "response_id" to respuestaId.toInt(),
                    "content" to contenido,
                    "user_id" to currentUserId,
                    "name" to (usuario["name"] ?: "Usuario"),
                    "lastname" to (usuario["lastname"] ?: ""),
                    "email" to (usuario["email"] ?: ""),
                    "gender" to (usuario["gender"] ?: "desconocido")
                )

                val postIndex = posts.indexOfFirst { it["post_id"] == postId }
                if (postIndex != -1) {
                    val post = posts[postIndex].toMutableMap()
                    val responses = (post["responses"] as? MutableList<Map<String, Any>>) ?: mutableListOf()
                    responses.add(respuesta)
                    post["responses"] = responses
                    posts[postIndex] = post

                    recyclerView.adapter?.notifyItemChanged(postIndex)
                    Toast.makeText(this, "Respuesta agregada", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Error al agregar la respuesta", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Error al obtener información del usuario", Toast.LENGTH_SHORT).show()
        }
    }

    private fun eliminarPost(postId: Int) {
        userManager.deletePost(postId)
        val index = posts.indexOfFirst { it["post_id"] == postId }
        if (index != -1) {
            posts.removeAt(index)
            recyclerView.adapter?.notifyItemRemoved(index)
            Toast.makeText(this, "Publicación eliminada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun eliminarRespuesta(postId: Int, responseId: Int) {
        userManager.deleteResponse(responseId)
        val postIndex = posts.indexOfFirst { it["post_id"] == postId }
        if (postIndex != -1) {
            val post = posts[postIndex].toMutableMap()
            val responses = (post["responses"] as? MutableList<Map<String, Any>>) ?: mutableListOf()
            val responseIndex = responses.indexOfFirst { it["response_id"] == responseId }
            if (responseIndex != -1) {
                responses.removeAt(responseIndex)
                post["responses"] = responses
                posts[postIndex] = post

                recyclerView.adapter?.notifyItemChanged(postIndex)
                Toast.makeText(this, "Respuesta eliminada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class PostAdapter(
        private val posts: List<Map<String, Any>>,
        private val onEliminarClick: (Int) -> Unit,
        private val onResponderClick: (Int, String) -> Unit,
        private val onEliminarRespuestaClick: (Int, Int) -> Unit,
        private val currentUserId: Int
    ) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

        inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val avatarImageView: ImageView = view.findViewById(R.id.avatar_image)
            val nameTextView: TextView = view.findViewById(R.id.name_text)
            val emailTextView: TextView = view.findViewById(R.id.email_text)
            val contentTextView: TextView = view.findViewById(R.id.content_text)
            val deleteButton: Button = view.findViewById(R.id.delete_button)
            val respondButton: Button = view.findViewById(R.id.respond_button)
            val responsesContainer: LinearLayout = view.findViewById(R.id.responses_container)
            val toggleResponsesButton: TextView =
                view.findViewById(R.id.toggle_responses_button) // Botón para minimizar/expandir respuestas
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_post, parent, false)
            return PostViewHolder(view)
        }

        override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
            val post = posts[position]
            holder.nameTextView.text =
                "${post["name"]} ${post["lastname"]}" ?: "Usuario Desconocido"
            holder.emailTextView.text = post["email"] as? String ?: "Correo no disponible"
            holder.contentTextView.text = post["content"] as? String ?: "Sin contenido"

            val gender = post["gender"] as? String ?: "default"
            val avatarResource = when (gender.lowercase()) {
                "masculino" -> R.drawable.hombre
                "femenino" -> R.drawable.mujer
                else -> R.drawable.circle_background2
            }
            holder.avatarImageView.setImageResource(avatarResource)

            val postUserId = post["user_id"] as? Int ?: -1
            holder.deleteButton.visibility =
                if (postUserId == currentUserId) View.VISIBLE else View.GONE

            holder.deleteButton.setOnClickListener {
                onEliminarClick(post["post_id"] as Int)
            }

            // Aquí integramos tu método mostrarDialogResponder
            holder.respondButton.setOnClickListener {
                (holder.itemView.context as ForoComunitario).mostrarDialogResponder(
                    post["post_id"] as Int
                ) { postId, contenido ->
                    onResponderClick(postId, contenido)
                }
            }

            // Manejar respuestas
            holder.responsesContainer.removeAllViews()
            val responses = post["responses"] as? List<Map<String, Any>> ?: emptyList()

            // Estado inicial: Ocultar respuestas
            holder.responsesContainer.visibility = View.GONE
            holder.toggleResponsesButton.text = "Ver respuestas (${responses.size})"

            // Alternar visibilidad al presionar el botón
            holder.toggleResponsesButton.setOnClickListener {
                if (holder.responsesContainer.visibility == View.VISIBLE) {
                    holder.responsesContainer.visibility = View.GONE
                    holder.toggleResponsesButton.text = "Ver respuestas (${responses.size})"
                } else {
                    holder.responsesContainer.visibility = View.VISIBLE
                    holder.toggleResponsesButton.text = "Ocultar respuestas"
                }
            }

            // Añadir respuestas al contenedor
            for (respuesta in responses) {
                val responseView = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_response, holder.responsesContainer, false)

                val responseAvatar =
                    responseView.findViewById<ImageView>(R.id.response_avatar_image)
                val responseName = responseView.findViewById<TextView>(R.id.response_name_text)
                val responseEmail = responseView.findViewById<TextView>(R.id.response_email_text)
                val responseContent =
                    responseView.findViewById<TextView>(R.id.response_content_text)
                val deleteResponseButton =
                    responseView.findViewById<Button>(R.id.delete_response_button)

                val responseGender = respuesta["gender"] as? String ?: "default"
                val responseAvatarResource = when (responseGender.lowercase()) {
                    "masculino" -> R.drawable.hombre
                    "femenino" -> R.drawable.mujer
                    else -> R.drawable.circle_background2
                }
                responseAvatar.setImageResource(responseAvatarResource)

                responseName.text = "${respuesta["name"]} ${respuesta["lastname"]}"
                responseEmail.text = respuesta["email"] as? String ?: "Correo no disponible"
                responseContent.text = respuesta["content"] as? String ?: "Respuesta vacía"

                val responseUserId = respuesta["user_id"] as? Int ?: -1
                deleteResponseButton.visibility =
                    if (responseUserId == currentUserId) View.VISIBLE else View.GONE

                deleteResponseButton.setOnClickListener {
                    val responseId = respuesta["response_id"] as? Int ?: return@setOnClickListener
                    onEliminarRespuestaClick(post["post_id"] as Int, responseId)
                }

                holder.responsesContainer.addView(responseView)
            }
        }

        override fun getItemCount(): Int = posts.size
    }
}