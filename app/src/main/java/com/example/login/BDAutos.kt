package com.example.login

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.FileOutputStream
import java.io.IOException

class BDAutos(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    init {
        copyDatabaseIfNeeded(context)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // No se necesita implementar nada aquí porque la base de datos se copia desde assets.
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Manejo de actualizaciones de base de datos, si fuera necesario.
    }

    // Copiar la base de datos desde assets si no existe
    private fun copyDatabaseIfNeeded(context: Context) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (dbFile.exists()) {
            Log.d("BDAutos", "La base de datos ya existe en ${dbFile.path}")
            return
        }

        try {
            val inputStream = context.assets.open(DATABASE_NAME)
            val outputStream = FileOutputStream(dbFile)

            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            Log.d("BDAutos", "Base de datos copiada correctamente a ${dbFile.path}")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("BDAutos", "Error copiando la base de datos: ${e.message}")
        }
    }

    // Obtener marcas
    fun obtenerMarcas(): List<String> {
        val listaMarcas = mutableListOf<String>()
        val db = this.readableDatabase

        Log.d("BDAutos", "Intentando acceder a la tabla 'Marcas'")

        val cursorTableCheck = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='Marcas'", null)
        if (cursorTableCheck.count > 0) {
            Log.d("BDAutos", "La tabla 'Marcas' existe.")
            val cursor = db.rawQuery("SELECT nombre FROM Marcas", null)
            if (cursor.moveToFirst()) {
                do {
                    listaMarcas.add(cursor.getString(0))
                } while (cursor.moveToNext())
            }
            cursor.close()
        } else {
            Log.e("BDAutos", "La tabla 'Marcas' no existe.")
            throw IllegalStateException("La tabla 'Marcas' no existe en la base de datos.")
        }
        cursorTableCheck.close()
        db.close()
        return listaMarcas
    }

    // Obtener el ID de una marca por nombre
    fun obtenerIdMarca(nombreMarca: String): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT id FROM Marcas WHERE nombre = ?", arrayOf(nombreMarca))
        var id = -1

        if (cursor.moveToFirst()) {
            id = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return id
    }

    // Obtener modelos por marca
    fun obtenerModelosPorMarca(marcaId: Int): List<String> {
        val listaModelos = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT modelo FROM Modelos WHERE marca_id = ?", arrayOf(marcaId.toString()))

        if (cursor.moveToFirst()) {
            do {
                listaModelos.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return listaModelos
    }

    // Obtener el ID de un modelo por nombre y marca
    fun obtenerIdModelo(nombreModelo: String, marcaId: Int): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT id FROM Modelos WHERE modelo = ? AND marca_id = ?", arrayOf(nombreModelo, marcaId.toString()))
        var id = -1

        if (cursor.moveToFirst()) {
            id = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return id
    }

    // Obtener años por modelo
    fun obtenerAniosPorModelo(modeloId: Int): List<Int> {
        val listaAnios = mutableListOf<Int>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT año FROM Años WHERE modelo_id = ?", arrayOf(modeloId.toString()))

        if (cursor.moveToFirst()) {
            do {
                listaAnios.add(cursor.getInt(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return listaAnios
    }

    // Obtener versiones por modelo y año
    fun obtenerVersionesPorModeloYAnio(modeloId: Int, anio: Int): List<String> {
        val listaVersiones = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT version FROM Versiones WHERE modelo_id = ? AND año = ?", arrayOf(modeloId.toString(), anio.toString()))

        if (cursor.moveToFirst()) {
            do {
                listaVersiones.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return listaVersiones
    }

    // Obtener autos por usuario
    fun obtenerAutosPorUsuario(userId: Int): List<Map<String, Any>> {
        val listaAutos = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM autos WHERE user_id = ?", arrayOf(userId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val auto = mapOf(
                    "marca" to cursor.getString(cursor.getColumnIndexOrThrow("marca")),
                    "modelo" to cursor.getString(cursor.getColumnIndexOrThrow("modelo")),
                    "año" to cursor.getInt(cursor.getColumnIndexOrThrow("año")),
                    "version" to cursor.getString(cursor.getColumnIndexOrThrow("version")),
                    "transmision" to cursor.getString(cursor.getColumnIndexOrThrow("transmision")),
                    "kilometraje" to cursor.getInt(cursor.getColumnIndexOrThrow("kilometraje")),
                    "placa_patente" to cursor.getString(cursor.getColumnIndexOrThrow("placa_patente"))
                )
                listaAutos.add(auto)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return listaAutos
    }

    companion object {
        private const val DATABASE_NAME = "BD_Autos.db"
        private const val DATABASE_VERSION = 1
    }
}
