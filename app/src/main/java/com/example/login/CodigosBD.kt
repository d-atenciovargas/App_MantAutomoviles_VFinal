package com.example.login

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class CodigosBD(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1) {
    companion object {
        private const val DB_NAME = "Codigos_DTC_OBD2.db"
        private var DB_PATH: String = ""
    }

    init {
        DB_PATH = context.applicationInfo.dataDir + "/databases/"
    }

    @Throws(IOException::class)
    fun createDatabase() {
        if (!checkDatabase()) {
            this.readableDatabase
            copyDatabase()
        }
    }

    private fun checkDatabase(): Boolean {
        val dbFile = File(DB_PATH + DB_NAME)
        return dbFile.exists()
    }

    @Throws(IOException::class)
    private fun copyDatabase() {
        val input: InputStream = context.assets.open(DB_NAME)
        val outputFileName = DB_PATH + DB_NAME
        val output = FileOutputStream(outputFileName)

        val buffer = ByteArray(1024)
        var length: Int
        while (input.read(buffer).also { length = it } > 0) {
            output.write(buffer, 0, length)
        }

        output.flush()
        output.close()
        input.close()
    }

    override fun onCreate(db: SQLiteDatabase?) {}

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}
