package com.example.prototipo_gravida_digital

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), AutoCloseable {

    companion object {
        private const val DATABASE_NAME = "gravida_digital.db"
        private const val DATABASE_VERSION = 2

        // Tabela de usuários
        const val TABLE_USUARIOS = "usuarios"
        const val COLUMN_ID = "id"
        const val COLUMN_NOME = "nome"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_SENHA = "senha"

        // Tabela de respostas
        const val TABLE_RESPOSTAS = "respostas"
        const val COLUMN_ID_RESPOSTA = "id_resposta"
        const val COLUMN_ID_USUARIO = "id_usuario"
        const val COLUMN_DATA = "data"
        const val COLUMN_PERGUNTA_NUM = "pergunta_numero"
        const val COLUMN_VALOR_RESPOSTA = "valor_resposta"
        const val COLUMN_ID_SECAO = "id_secao"

        // Tabela de fotos
        const val TABLE_FOTOS = "fotos"
        const val COLUMN_ID_FOTO = "id_foto"
        const val COLUMN_ACTIVITY = "activity"
        const val COLUMN_CAMINHO = "caminho"
        const val COLUMN_ID_SECAO_FOTO = "id_secao"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Tabela de usuários
        db.execSQL("""
            CREATE TABLE $TABLE_USUARIOS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_SENHA TEXT NOT NULL
            )
        """)

        // 2. Tabela de respostas (com id_secao)
        db.execSQL("""
            CREATE TABLE $TABLE_RESPOSTAS (
                $COLUMN_ID_RESPOSTA INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ID_USUARIO INTEGER NOT NULL,
                $COLUMN_DATA TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_PERGUNTA_NUM INTEGER NOT NULL CHECK ($COLUMN_PERGUNTA_NUM BETWEEN 1 AND 8),
                $COLUMN_VALOR_RESPOSTA INTEGER NOT NULL CHECK ($COLUMN_VALOR_RESPOSTA BETWEEN 0 AND 3),
                $COLUMN_ID_SECAO INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_ID_USUARIO) REFERENCES $TABLE_USUARIOS($COLUMN_ID)
            )
        """)

        // 3. Tabela de fotos (com id_secao)
        db.execSQL("""
            CREATE TABLE $TABLE_FOTOS (
                $COLUMN_ID_FOTO INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ID_USUARIO INTEGER NOT NULL,
                $COLUMN_ACTIVITY TEXT NOT NULL,
                $COLUMN_CAMINHO TEXT NOT NULL,
                $COLUMN_ID_SECAO_FOTO INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_ID_USUARIO) REFERENCES $TABLE_USUARIOS($COLUMN_ID)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RESPOSTAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FOTOS")
        onCreate(db)
    }

    // Métodos para usuários
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
            if (it.moveToFirst()) it.getLong(0) else -1L
        }
    }

    fun verificarEmailExistente(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT $COLUMN_ID FROM $TABLE_USUARIOS 
            WHERE $COLUMN_EMAIL = ?
        """, arrayOf(email))
        return cursor.use { it.count > 0 }
    }

    // Métodos para respostas
    fun salvarResposta(idUsuario: Int, perguntaNum: Int, valor: Int, idSecao: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID_USUARIO, idUsuario)
            put(COLUMN_PERGUNTA_NUM, perguntaNum)
            put(COLUMN_VALOR_RESPOSTA, valor)
            put(COLUMN_ID_SECAO, idSecao)
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

    fun buscarRespostasPorSecao(idSecao: Int): Map<Int, Int> {
        val respostas = mutableMapOf<Int, Int>()
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT $COLUMN_PERGUNTA_NUM, $COLUMN_VALOR_RESPOSTA 
            FROM $TABLE_RESPOSTAS 
            WHERE $COLUMN_ID_SECAO = ?
        """, arrayOf(idSecao.toString()))

        while (cursor.moveToNext()) {
            respostas[cursor.getInt(0)] = cursor.getInt(1)
        }
        cursor.close()
        return respostas
    }

    // Métodos para fotos
    fun salvarFoto(idUsuario: Int, activity: String, caminho: String, idSecao: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID_USUARIO, idUsuario)
            put(COLUMN_ACTIVITY, activity)
            put(COLUMN_CAMINHO, caminho)
            put(COLUMN_ID_SECAO_FOTO, idSecao)
        }
        return db.insert(TABLE_FOTOS, null, values)
    }

    fun buscarFotosPorSecao(idSecao: Int): List<Pair<String, String>> {
        val fotos = mutableListOf<Pair<String, String>>()
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT $COLUMN_ACTIVITY, $COLUMN_CAMINHO 
            FROM $TABLE_FOTOS 
            WHERE $COLUMN_ID_SECAO_FOTO = ?
        """, arrayOf(idSecao.toString()))

        while (cursor.moveToNext()) {
            fotos.add(Pair(cursor.getString(0), cursor.getString(1)))
        }
        cursor.close()
        return fotos
    }

    override fun close() {
        super.close()
    }
}