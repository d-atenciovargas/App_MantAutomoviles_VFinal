package com.example.login

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class UserManager(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "UserManager.db"
        private const val DATABASE_VERSION = 6 // Incrementamos la versión para la tabla de posts
        const val TABLE_USERS = "users"
        const val TABLE_AUTOS = "autos"
        const val TABLE_POSTS = "posts" // Nueva tabla para los posts

        // Columnas para la tabla de usuarios
        const val COLUMN_ID = "id"
        const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password"
        const val COLUMN_NAME = "name"
        const val COLUMN_LASTNAME = "lastname"
        private const val COLUMN_BIRTHDATE = "birthdate"
        private const val COLUMN_PHONE = "phone"
        private const val COLUMN_ROLE = "role"
        const val COLUMN_GENDER = "gender"

        // Columnas para la tabla de autos
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_MARCA = "marca"
        private const val COLUMN_MODELO = "modelo"
        private const val COLUMN_ANO = "año"
        private const val COLUMN_VERSION = "version"
        private const val COLUMN_TRANSMISION = "transmision"
        private const val COLUMN_KILOMETRAJE = "kilometraje"
        private const val COLUMN_PLACA_PATENTE = "placa_patente"

        // Columnas para la tabla de posts
        const val COLUMN_POST_ID = "post_id"
        const val COLUMN_POST_CONTENT = "content"
        const val COLUMN_POST_USER_ID = "user_id"

        //Columnas para la tabla de respuestas
        const val TABLE_RESPONSES = "responses"
        const val COLUMN_RESPONSE_ID = "response_id"
        const val COLUMN_RESPONSE_CONTENT = "content"
        const val COLUMN_RESPONSE_USER_ID = "user_id"
        const val COLUMN_RESPONSE_POST_ID = "post_id"

    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Crear tabla de usuarios
        val createUsersTable = ("CREATE TABLE $TABLE_USERS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_EMAIL TEXT UNIQUE," +
                "$COLUMN_PASSWORD TEXT," +
                "$COLUMN_NAME TEXT," +
                "$COLUMN_LASTNAME TEXT," +
                "$COLUMN_BIRTHDATE TEXT," +
                "$COLUMN_PHONE TEXT," +
                "$COLUMN_ROLE TEXT," +
                "$COLUMN_GENDER TEXT)")
        db?.execSQL(createUsersTable)

        // Crear tabla de autos
        val createAutosTable = ("CREATE TABLE $TABLE_AUTOS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_USER_ID INTEGER," +
                "$COLUMN_MARCA TEXT," +
                "$COLUMN_MODELO TEXT," +
                "$COLUMN_ANO INTEGER," +
                "$COLUMN_VERSION TEXT," +
                "$COLUMN_TRANSMISION TEXT," +
                "$COLUMN_KILOMETRAJE INTEGER," +
                "$COLUMN_PLACA_PATENTE TEXT," +
                "FOREIGN KEY($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID))")
        db?.execSQL(createAutosTable)

        // Crear tabla de posts
        val createPostsTable = ("CREATE TABLE $TABLE_POSTS (" +
                "$COLUMN_POST_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_POST_CONTENT TEXT," +
                "$COLUMN_POST_USER_ID INTEGER," +
                "FOREIGN KEY($COLUMN_POST_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID))")
        db?.execSQL(createPostsTable)

        // Crear tabla de respuestas
        val createResponsesTable = """
        CREATE TABLE IF NOT EXISTS $TABLE_RESPONSES (
            response_id INTEGER PRIMARY KEY AUTOINCREMENT,
            post_id INTEGER,
            user_id INTEGER,
            content TEXT,
            FOREIGN KEY(post_id) REFERENCES $TABLE_POSTS($COLUMN_POST_ID),
            FOREIGN KEY(user_id) REFERENCES $TABLE_USERS($COLUMN_ID)
        )
    """.trimIndent()
        db?.execSQL(createResponsesTable)

        // Crear tabla de historial de mantenimiento
        val createHistorialTable = """
        CREATE TABLE historial_mantenimiento (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            auto_id INTEGER,
            datos_mantenimiento TEXT,
            FOREIGN KEY(auto_id) REFERENCES $TABLE_AUTOS($COLUMN_ID)
        )
    """.trimIndent()
        db?.execSQL(createHistorialTable)
    }



    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Comprobar si la columna 'gender' ya existe antes de agregarla
            val cursor = db?.rawQuery("PRAGMA table_info($TABLE_USERS)", null)
            var columnExists = false
            cursor?.use {
                while (it.moveToNext()) {
                    val columnName = it.getString(it.getColumnIndexOrThrow("name"))
                    if (columnName == COLUMN_GENDER) {
                        columnExists = true
                        break
                    }
                }
            }
            if (!columnExists) {
                db?.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_GENDER TEXT")
            }
        }

        if (oldVersion < 3) {
            // Crear la tabla autos si no existe
            val createAutosTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_AUTOS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER,
                $COLUMN_MARCA TEXT,
                $COLUMN_MODELO TEXT,
                $COLUMN_ANO INTEGER,
                $COLUMN_VERSION TEXT,
                $COLUMN_TRANSMISION TEXT,
                $COLUMN_KILOMETRAJE INTEGER,
                $COLUMN_PLACA_PATENTE TEXT,
                FOREIGN KEY($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID)
            )
        """.trimIndent()
            db?.execSQL(createAutosTable)
        }

        if (oldVersion < 4) {
            // Crear la tabla posts si no existe
            val createPostsTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_POSTS (
                $COLUMN_POST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_POST_CONTENT TEXT,
                $COLUMN_POST_USER_ID INTEGER,
                FOREIGN KEY($COLUMN_POST_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID)
            )
        """.trimIndent()
            db?.execSQL(createPostsTable)
        }

        if (oldVersion < 5) {
            // Crear la tabla responses si no existe
            val createResponsesTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_RESPONSES (
                response_id INTEGER PRIMARY KEY AUTOINCREMENT,
                post_id INTEGER,
                user_id INTEGER,
                content TEXT,
                FOREIGN KEY(post_id) REFERENCES $TABLE_POSTS($COLUMN_POST_ID),
                FOREIGN KEY(user_id) REFERENCES $TABLE_USERS($COLUMN_ID)
            )
        """.trimIndent()
            db?.execSQL(createResponsesTable)
        }

        if (oldVersion < 6) {
            // Crear la tabla historial_mantenimiento si no existe
            val createHistorialTable = """
            CREATE TABLE IF NOT EXISTS historial_mantenimiento (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                auto_id INTEGER,
                datos_mantenimiento TEXT,
                FOREIGN KEY(auto_id) REFERENCES $TABLE_AUTOS($COLUMN_ID)
            )
        """.trimIndent()
            db?.execSQL(createHistorialTable)
        }
    }


    // Función para agregar un usuario
    fun addUser(email: String, password: String, name: String, lastname: String, birthdate: String, phone: String, gender: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_NAME, name)
            put(COLUMN_LASTNAME, lastname)
            put(COLUMN_BIRTHDATE, birthdate)
            put(COLUMN_PHONE, phone)
            put(COLUMN_ROLE, "usuario estandar")
            put(COLUMN_GENDER, gender)
        }
        val id = db.insert(TABLE_USERS, null, values)
        db.close()
        return id
    }

    // Función para verificar si un correo existe
    fun emailExists(email: String): Boolean {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?", arrayOf(email))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // Función para verificar usuario y contraseña
    fun checkUser(email: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor: Cursor = db.query(
            TABLE_USERS, null, "$COLUMN_EMAIL=? AND $COLUMN_PASSWORD=?",
            arrayOf(email, password), null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // Función para actualizar la contraseña
    fun updatePassword(email: String, newPassword: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PASSWORD, newPassword)
        }
        val rowsAffected = db.update(TABLE_USERS, values, "$COLUMN_EMAIL = ?", arrayOf(email))
        db.close()
        return rowsAffected > 0
    }

    // Función para obtener el ID de un usuario por correo
    fun getUserIdByEmail(email: String): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_ID FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?", arrayOf(email))
        var userId = -1
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return userId
    }


    // Método para obtener el correo electrónico por user_id
    fun getEmailByUserId(userId: Int): String? {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_EMAIL FROM $TABLE_USERS WHERE $COLUMN_ID = ?",
            arrayOf(userId.toString())
        )
        var email: String? = null
        if (cursor.moveToFirst()) {
            email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
        }
        cursor.close()
        db.close()
        return email
    }


    // Función para agregar un auto
    fun addAuto(userId: Int, marca: String, modelo: String, ano: Int, version: String, transmision: String, kilometraje: Int, placaPatente: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, userId)
            put(COLUMN_MARCA, marca)
            put(COLUMN_MODELO, modelo)
            put(COLUMN_ANO, ano)
            put(COLUMN_VERSION, version)
            put(COLUMN_TRANSMISION, transmision)
            put(COLUMN_KILOMETRAJE, kilometraje)
            put(COLUMN_PLACA_PATENTE, placaPatente)
        }
        val id = db.insert(TABLE_AUTOS, null, values)
        db.close()
        return id
    }

    // Función para obtener los autos de un usuario
    fun getAutosByUserId(userId: Int): List<Map<String, Any>> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_AUTOS WHERE $COLUMN_USER_ID = ?", arrayOf(userId.toString()))
        val autos = mutableListOf<Map<String, Any>>()

        if (cursor.moveToFirst()) {
            do {
                val auto = mapOf(
                    "id" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    "marca" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARCA)),
                    "modelo" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MODELO)),
                    "ano" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ANO)),
                    "version" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VERSION)),
                    "transmision" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSMISION)),
                    "kilometraje" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_KILOMETRAJE)),
                    "placa_patente" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLACA_PATENTE))
                )
                autos.add(auto)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return autos
    }

    // Método para obtener un auto por su ID
    fun getAutoById(autoId: Int): Map<String, Any>? {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_MARCA, $COLUMN_MODELO, $COLUMN_ANO, $COLUMN_VERSION, $COLUMN_TRANSMISION, $COLUMN_KILOMETRAJE, $COLUMN_PLACA_PATENTE FROM $TABLE_AUTOS WHERE $COLUMN_ID = ?",
            arrayOf(autoId.toString())
        )
        var auto: Map<String, Any>? = null
        if (cursor.moveToFirst()) {
            auto = mapOf(
                "marca" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARCA)),
                "modelo" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MODELO)),
                "ano" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ANO)),
                "version" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VERSION)),
                "transmision" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSMISION)),
                "kilometraje" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_KILOMETRAJE)),
                "placa_patente" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLACA_PATENTE))
            )
        }
        cursor.close()
        db.close()
        return auto
    }

    // Nueva función para obtener datos del usuario por email
    fun getUserByEmail(email: String): Map<String, String>? {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            """
        SELECT $COLUMN_NAME, 
               $COLUMN_LASTNAME, 
               $COLUMN_EMAIL, 
               $COLUMN_BIRTHDATE, 
               $COLUMN_PHONE, 
               $COLUMN_ROLE, 
               $COLUMN_GENDER
        FROM $TABLE_USERS 
        WHERE $COLUMN_EMAIL = ?
        """, arrayOf(email)
        )

        var user: Map<String, String>? = null
        if (cursor.moveToFirst()) {
            user = mapOf(
                "name" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: "No especificado"),
                "lastname" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LASTNAME)) ?: "No especificado"),
                "email" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)) ?: "No especificado"),
                "birthdate" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIRTHDATE)) ?: "No especificado"),
                "phone" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)) ?: "No especificado"),
                "role" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE)) ?: "No especificado"),
                "gender" to (cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER)) ?: "No especificado")
            )
        }
        cursor.close()
        db.close()
        return user
    }


    // Nueva función para agregar un post
    fun addPost(userId: Int, content: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_POST_USER_ID, userId)
            put(COLUMN_POST_CONTENT, content)
        }
        val id = db.insert(TABLE_POSTS, null, values)
        db.close()
        return id
    }

    // Nueva función para obtener todos los posts
    // Recuperar todos los posts
    fun getAllPosts(): List<Map<String, Any>> {
        val db = this.readableDatabase
        val query = """
        SELECT posts.post_id, posts.content, posts.user_id, users.name, users.lastname, users.email, users.gender
        FROM posts
        JOIN users ON posts.user_id = users.id
    """.trimIndent()
        val cursor = db.rawQuery(query, null)
        val posts = mutableListOf<Map<String, Any>>()

        if (cursor.moveToFirst()) {
            do {
                val post = mapOf(
                    "post_id" to cursor.getInt(cursor.getColumnIndexOrThrow("post_id")),
                    "content" to cursor.getString(cursor.getColumnIndexOrThrow("content")),
                    "user_id" to cursor.getInt(cursor.getColumnIndexOrThrow("user_id")), // Aseguramos user_id
                    "name" to cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    "lastname" to cursor.getString(cursor.getColumnIndexOrThrow("lastname")),
                    "email" to cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    "gender" to cursor.getString(cursor.getColumnIndexOrThrow("gender"))
                )
                posts.add(post)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return posts
    }

    fun getPostById(postId: Int): Map<String, Any>? {
        val db = this.readableDatabase
        val query = """
        SELECT posts.post_id, posts.content, posts.user_id, users.name, users.lastname, users.email, users.gender
        FROM posts
        JOIN users ON posts.user_id = users.id
        WHERE posts.post_id = ?
    """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(postId.toString()))

        var post: Map<String, Any>? = null
        if (cursor.moveToFirst()) {
            post = mapOf(
                "post_id" to cursor.getInt(cursor.getColumnIndexOrThrow("post_id")),
                "content" to cursor.getString(cursor.getColumnIndexOrThrow("content")),
                "user_id" to cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
                "name" to cursor.getString(cursor.getColumnIndexOrThrow("name")),
                "lastname" to cursor.getString(cursor.getColumnIndexOrThrow("lastname")),
                "email" to cursor.getString(cursor.getColumnIndexOrThrow("email")),
                "gender" to cursor.getString(cursor.getColumnIndexOrThrow("gender"))
            )
        }
        cursor.close()
        db.close()
        return post
    }

    // Nueva función para eliminar un post por su ID
    fun deletePost(postId: Int): Int {
        val db = this.writableDatabase
        val rowsDeleted = db.delete(TABLE_POSTS, "$COLUMN_POST_ID = ?", arrayOf(postId.toString()))
        db.close()
        return rowsDeleted
    }

    // Método para agregar una respuesta
    fun addResponse(postId: Int, userId: Int, content: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("post_id", postId)
            put("user_id", userId)
            put("content", content)
        }
        val responseId = db.insert("responses", null, values)
        db.close()
        return responseId
    }

    // Método para obtener las respuestas de un post
    fun getResponsesByPostId(postId: Int): List<Map<String, Any>> {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            """
        SELECT responses.response_id, responses.content, responses.user_id, users.name, users.lastname, users.email, users.gender
        FROM responses
        JOIN users ON responses.user_id = users.id
        WHERE responses.post_id = ?
        """, arrayOf(postId.toString())
        )

        val responses = mutableListOf<Map<String, Any>>()
        if (cursor.moveToFirst()) {
            do {
                val response = mapOf(
                    "response_id" to cursor.getInt(cursor.getColumnIndexOrThrow("response_id")),
                    "content" to cursor.getString(cursor.getColumnIndexOrThrow("content")),
                    "user_id" to cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
                    "name" to cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    "lastname" to cursor.getString(cursor.getColumnIndexOrThrow("lastname")),
                    "email" to cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    "gender" to cursor.getString(cursor.getColumnIndexOrThrow("gender"))
                )
                responses.add(response)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return responses
    }

    fun getAllResponses(): List<Map<String, String>> {
        val db = this.readableDatabase
        val query = """
        SELECT responses.response_id, responses.content, users.name, users.lastname
        FROM responses
        JOIN users ON responses.user_id = users.id
    """.trimIndent()
        val cursor = db.rawQuery(query, null)
        val respuestas = mutableListOf<Map<String, String>>()

        if (cursor.moveToFirst()) {
            do {
                val respuesta = mapOf(
                    "response_id" to cursor.getInt(cursor.getColumnIndexOrThrow("response_id")).toString(),
                    "content" to cursor.getString(cursor.getColumnIndexOrThrow("content")),
                    "name" to cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    "lastname" to cursor.getString(cursor.getColumnIndexOrThrow("lastname"))
                )
                respuestas.add(respuesta)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return respuestas
    }


    // Método para eliminar una respuesta
    fun deleteResponse(responseId: Int): Int {
        val db = this.writableDatabase
        val rowsDeleted = db.delete("responses", "response_id = ?", arrayOf(responseId.toString()))
        db.close()
        return rowsDeleted
    }

    fun deleteAutoById(autoId: Int): Boolean {
        val db = this.writableDatabase
        val rowsAffected = db.delete(TABLE_AUTOS, "$COLUMN_ID = ?", arrayOf(autoId.toString()))
        db.close()
        return rowsAffected > 0
    }

    fun actualizarKilometraje(autoId: Int, nuevoKilometraje: Int): Boolean {
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put("kilometraje", nuevoKilometraje)
        }
        val rowsUpdated = db.update("autos", contentValues, "id = ?", arrayOf(autoId.toString()))
        db.close()
        return rowsUpdated > 0
    }

    fun guardarHistorial(autoId: Int, datosMantenimiento: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("auto_id", autoId)
            put("datos_mantenimiento", datosMantenimiento)
        }
        val id = db.insert("historial_mantenimiento", null, values)
        db.close()
        return id
    }

    fun obtenerHistorialPorAutoId(autoId: Int): List<Map<String, String>> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM historial_mantenimiento WHERE auto_id = ?", arrayOf(autoId.toString()))
        val historial = mutableListOf<Map<String, String>>()

        if (cursor.moveToFirst()) {
            do {
                val registro = mapOf(
                    "id" to cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString(),
                    "auto_id" to cursor.getInt(cursor.getColumnIndexOrThrow("auto_id")).toString(),
                    "datos_mantenimiento" to cursor.getString(cursor.getColumnIndexOrThrow("datos_mantenimiento"))
                )
                historial.add(registro)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return historial
    }

    fun getHistorialCompleto(): List<Map<String, String>> {
        val db = this.readableDatabase
        val query = """
        SELECT h.id AS historial_id, 
               h.datos_mantenimiento,
               a.marca AS auto_marca, 
               a.modelo AS auto_modelo, 
               u.name AS user_name, 
               u.lastname AS user_lastname
        FROM historial_mantenimiento h
        JOIN autos a ON h.auto_id = a.id
        JOIN users u ON a.user_id = u.id
    """
        val cursor = db.rawQuery(query, null)
        val historial = mutableListOf<Map<String, String>>()

        if (cursor.moveToFirst()) {
            do {
                val registro = mapOf(
                    "historial_id" to cursor.getInt(cursor.getColumnIndexOrThrow("historial_id")).toString(),
                    "datos_mantenimiento" to cursor.getString(cursor.getColumnIndexOrThrow("datos_mantenimiento")),
                    "auto_marca" to cursor.getString(cursor.getColumnIndexOrThrow("auto_marca")),
                    "auto_modelo" to cursor.getString(cursor.getColumnIndexOrThrow("auto_modelo")),
                    "user_name" to cursor.getString(cursor.getColumnIndexOrThrow("user_name")),
                    "user_lastname" to cursor.getString(cursor.getColumnIndexOrThrow("user_lastname"))
                )
                historial.add(registro)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return historial
    }


    fun eliminarRegistroMantenimiento(registroId: Int): Boolean {
        val db = writableDatabase
        return try {
            db.delete("historial_mantenimiento", "id = ?", arrayOf(registroId.toString()))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.close()
        }
    }

    fun getAllHistorial(): List<Map<String, String>> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM historial_mantenimiento", null)
        val historial = mutableListOf<Map<String, String>>()

        if (cursor.moveToFirst()) {
            do {
                val registro = mapOf(
                    "id" to cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString(),
                    "auto_id" to cursor.getInt(cursor.getColumnIndexOrThrow("auto_id")).toString(),
                    "datos_mantenimiento" to cursor.getString(cursor.getColumnIndexOrThrow("datos_mantenimiento"))
                )
                historial.add(registro)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return historial
    }


    fun getAllUsers(): List<Map<String, String>> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS", null)
        val usuarios = mutableListOf<Map<String, String>>()

        if (cursor.moveToFirst()) {
            do {
                val usuario = mapOf(
                    "id" to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)).toString(),
                    "email" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                    "name" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    "lastname" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LASTNAME)),
                    "birthdate" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIRTHDATE)),
                    "phone" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                    "role" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE)),
                    "gender" to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER))
                )
                usuarios.add(usuario)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return usuarios
    }

    fun deleteUserById(userId: Int): Boolean {
        val db = this.writableDatabase
        val rowsAffected = db.delete(TABLE_USERS, "$COLUMN_ID = ?", arrayOf(userId.toString()))
        db.close()
        return rowsAffected > 0
    }

    fun getAllAutos(): List<Map<String, String>> {
        val db = this.readableDatabase
        val query = """
        SELECT autos.id, autos.marca, autos.modelo, autos.año AS ano, autos.version, 
               autos.transmision, autos.kilometraje, autos.placa_patente,
               users.name AS ownerName, users.lastname AS ownerLastname
        FROM autos
        JOIN users ON autos.user_id = users.id
    """
        val cursor = db.rawQuery(query, null)
        val autos = mutableListOf<Map<String, String>>()

        if (cursor.moveToFirst()) {
            do {
                val auto = mapOf(
                    "id" to cursor.getInt(cursor.getColumnIndexOrThrow("id")).toString(),
                    "marca" to cursor.getString(cursor.getColumnIndexOrThrow("marca")),
                    "modelo" to cursor.getString(cursor.getColumnIndexOrThrow("modelo")),
                    "ano" to cursor.getInt(cursor.getColumnIndexOrThrow("ano")).toString(),
                    "version" to cursor.getString(cursor.getColumnIndexOrThrow("version")),
                    "transmision" to cursor.getString(cursor.getColumnIndexOrThrow("transmision")),
                    "kilometraje" to cursor.getInt(cursor.getColumnIndexOrThrow("kilometraje")).toString(),
                    "placa_patente" to cursor.getString(cursor.getColumnIndexOrThrow("placa_patente")),
                    "ownerName" to cursor.getString(cursor.getColumnIndexOrThrow("ownerName")),
                    "ownerLastname" to cursor.getString(cursor.getColumnIndexOrThrow("ownerLastname"))
                )
                autos.add(auto)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return autos
    }


}