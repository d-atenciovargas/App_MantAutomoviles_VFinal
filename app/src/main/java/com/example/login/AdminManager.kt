package com.example.login

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class AdminManager(context: Context, private val dbName: String) : SQLiteOpenHelper(context, dbName, null, 1) {

    companion object {
        private const val TABLE_ADMIN = "Administradores" // Nombre de la tabla
        private const val COLUMN_EMAIL = "email" // Columna para el correo electrónico
        private const val COLUMN_PASSWORD = "password" // Columna para la contraseña
    }

    init {
        copyDatabase(context)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // No es necesario implementar la creación de tablas porque la base de datos ya existe
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // No es necesario implementar actualizaciones para esta base de datos
    }

    /**
     * Copia la base de datos desde la carpeta assets al directorio de bases de datos de la aplicación.
     * Si la base de datos ya existe, no realiza ninguna acción.
     */
    private fun copyDatabase(context: Context) {
        val dbPath = context.getDatabasePath(dbName)
        if (!dbPath.exists()) {
            dbPath.parentFile?.mkdirs()
            try {
                val inputStream: InputStream = context.assets.open(dbName)
                val outputStream: OutputStream = FileOutputStream(dbPath)

                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Verifica si un administrador existe en la tabla "Administradores" con el email y password proporcionados.
     *
     * @param email El correo electrónico del administrador.
     * @param password La contraseña del administrador.
     * @return true si existe, false en caso contrario.
     */
    fun checkAdmin(email: String, password: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_ADMIN WHERE $COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(email, password))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    /**
     * Verifica si un correo electrónico existe en la tabla "Administradores".
     *
     * @param email El correo electrónico a verificar.
     * @return true si el correo existe, false en caso contrario.
     */
}
