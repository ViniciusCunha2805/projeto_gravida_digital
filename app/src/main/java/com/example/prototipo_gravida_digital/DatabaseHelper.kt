package com.example.prototipo_gravida_digital

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "gravida_digital.db"
        private const val DATABASE_VERSION = 1  // Versão reiniciada

        // Tabela de usuários
        const val TABLE_USUARIOS = "usuarios"
        const val COLUMN_ID = "id"
        const val COLUMN_NOME = "nome"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_SENHA = "senha"

        // Tabela de respostas (nova estrutura)
        const val TABLE_RESPOSTAS = "respostas"
        const val COLUMN_ID_RESPOSTA = "id_resposta"
        const val COLUMN_ID_USUARIO = "id_usuario"
        const val COLUMN_DATA = "data"
        const val COLUMN_PERGUNTA_NUM = "pergunta_numero"
        const val COLUMN_VALOR_RESPOSTA = "valor_resposta"

        // Tabela de fotos
        const val TABLE_FOTOS = "fotos"
        const val COLUMN_ID_FOTO = "id_foto"
        const val COLUMN_ACTIVITY = "activity"
        const val COLUMN_CAMINHO = "caminho"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Tabela de usuários (inalterada)
        db.execSQL("""
            CREATE TABLE $TABLE_USUARIOS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_SENHA TEXT NOT NULL
            )
        """)

        // 2. NOVA tabela de respostas (sem colunas P1-P8)
        db.execSQL("""
            CREATE TABLE $TABLE_RESPOSTAS (
                $COLUMN_ID_RESPOSTA INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ID_USUARIO INTEGER NOT NULL,
                $COLUMN_DATA TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_PERGUNTA_NUM INTEGER NOT NULL CHECK ($COLUMN_PERGUNTA_NUM BETWEEN 1 AND 8),
                $COLUMN_VALOR_RESPOSTA INTEGER NOT NULL CHECK ($COLUMN_VALOR_RESPOSTA BETWEEN 0 AND 3),
                FOREIGN KEY ($COLUMN_ID_USUARIO) REFERENCES $TABLE_USUARIOS($COLUMN_ID)
            )
        """)

        // 3. Tabela de fotos (inalterada)
        db.execSQL("""
            CREATE TABLE $TABLE_FOTOS (
                $COLUMN_ID_FOTO INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ID_USUARIO INTEGER NOT NULL,
                $COLUMN_ACTIVITY TEXT NOT NULL,
                $COLUMN_CAMINHO TEXT NOT NULL,
                FOREIGN KEY ($COLUMN_ID_USUARIO) REFERENCES $TABLE_USUARIOS($COLUMN_ID)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Remove tudo e recria (para versões futuras)
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RESPOSTAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FOTOS")
        onCreate(db)
    }

    // Métodos para usuários (inalterados) ------------------------------------
    fun cadastrarUsuario(nome: String, email: String, senha: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NOME, nome)
            put(COLUMN_EMAIL, email)
            put(COLUMN_SENHA, senha)
        }
        return db.insert(TABLE_USUARIOS, null, values)
    }

    fun verificarLogin(email: String, senha: String): Long {
        val db = readableDatabase
        val cursor = db.rawQuery("""
        SELECT $COLUMN_ID FROM $TABLE_USUARIOS 
        WHERE $COLUMN_EMAIL = ? AND $COLUMN_SENHA = ?
    """, arrayOf(email, senha))

        return cursor.use {
            if (it.moveToFirst()) it.getLong(0) else (-1L)  // Corrige todos os erros
        }
    }

    fun verificarEmailExistente(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_USUARIOS 
            WHERE $COLUMN_EMAIL = ?
        """, arrayOf(email))
        return cursor.count > 0.also { cursor.close() }
    }

    // NOVOS métodos para respostas -------------------------------------------
    fun salvarResposta(idUsuario: Int, perguntaNum: Int, valor: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID_USUARIO, idUsuario)
            put(COLUMN_PERGUNTA_NUM, perguntaNum)
            put(COLUMN_VALOR_RESPOSTA, valor)
        }
        return db.insert(TABLE_RESPOSTAS, null, values)
    }

    fun buscarRespostas(idUsuario: Int): Map<Int, Int> {
        val respostas = mutableMapOf<Int, Int>()
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT $COLUMN_PERGUNTA_NUM, $COLUMN_VALOR_RESPOSTA 
            FROM $TABLE_RESPOSTAS 
            WHERE $COLUMN_ID_USUARIO = ?
        """, arrayOf(idUsuario.toString()))

        while (cursor.moveToNext()) {
            respostas[cursor.getInt(0)] = cursor.getInt(1)
        }
        cursor.close()
        return respostas
    }

    // Métodos para fotos (inalterados) --------------------------------------
    fun salvarFoto(idUsuario: Int, activity: String, caminho: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID_USUARIO, idUsuario)
            put(COLUMN_ACTIVITY, activity)
            put(COLUMN_CAMINHO, caminho)
        }
        return db.insert(TABLE_FOTOS, null, values)
    }
}