package com.example.prototipo_gravida_digital

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "gravida_digital.db"
        private const val DATABASE_VERSION = 1

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
        const val COLUMN_P1 = "pergunta1"
        const val COLUMN_P2 = "pergunta2"
        const val COLUMN_P3 = "pergunta3"
        const val COLUMN_P4 = "pergunta4"
        const val COLUMN_P5 = "pergunta5"
        const val COLUMN_P6 = "pergunta6"
        const val COLUMN_P7 = "pergunta7"
        const val COLUMN_P8 = "pergunta8"

        // Tabela de fotos
        const val TABLE_FOTOS = "fotos"
        const val COLUMN_ID_FOTO = "id_foto"
        const val COLUMN_ACTIVITY = "activity"
        const val COLUMN_CAMINHO = "caminho"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_USUARIOS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_SENHA TEXT NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_RESPOSTAS (
                $COLUMN_ID_RESPOSTA INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ID_USUARIO INTEGER NOT NULL,
                $COLUMN_DATA TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_P1 INTEGER,
                $COLUMN_P2 INTEGER,
                $COLUMN_P3 INTEGER,
                $COLUMN_P4 INTEGER,
                $COLUMN_P5 INTEGER,
                $COLUMN_P6 INTEGER,
                $COLUMN_P7 INTEGER,
                $COLUMN_P8 INTEGER,
                FOREIGN KEY ($COLUMN_ID_USUARIO) REFERENCES $TABLE_USUARIOS($COLUMN_ID)
            )
        """)

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
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RESPOSTAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FOTOS")
        onCreate(db)
    }

    // Método para cadastrar usuário
    fun cadastrarUsuario(nome: String, email: String, senha: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NOME, nome)
            put(COLUMN_EMAIL, email)
            put(COLUMN_SENHA, senha)
        }
        return db.insert(TABLE_USUARIOS, null, values)
    }

    // Método para verificar login (COM CURSOR CORRETAMENTE FECHADO)
    fun verificarLogin(email: String, senha: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT * FROM $TABLE_USUARIOS 
            WHERE $COLUMN_EMAIL = ? AND $COLUMN_SENHA = ?
        """, arrayOf(email, senha))

        val existe = cursor.count > 0
        cursor.close() // Fechando o cursor para liberar recursos
        return existe
    }

    // Método para verificar se e-mail já está cadastrado (COM CURSOR CORRETAMENTE FECHADO)
    fun verificarEmailExistente(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("""
        SELECT * FROM $TABLE_USUARIOS 
        WHERE $COLUMN_EMAIL = ?
    """, arrayOf(email))

        val existe = cursor.count > 0
        cursor.close() // Fechando o cursor para liberar recursos
        return existe
    }

    // Método para salvar respostas
    fun salvarRespostaUnica(idUsuario: Int, numeroPergunta: Int, valor: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID_USUARIO, idUsuario)
            when(numeroPergunta) {
                1 -> put(COLUMN_P1, valor)
                2 -> put(COLUMN_P2, valor)
                3 -> put(COLUMN_P3, valor)
                4 -> put(COLUMN_P4, valor)
                5 -> put(COLUMN_P5, valor)
                6 -> put(COLUMN_P6, valor)
                7 -> put(COLUMN_P7, valor)
                8 -> put(COLUMN_P8, valor)
            }
        }
        return db.insert(TABLE_RESPOSTAS, null, values)
    }

    // Método para salvar fotos
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