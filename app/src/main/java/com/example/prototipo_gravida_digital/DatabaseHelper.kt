package com.example.prototipo_gravida_digital

// Importações necessárias para manipulação de banco SQLite, contexto, datas e logs
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

// Classe responsável por gerenciar o banco de dados SQLite local
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), AutoCloseable {

    companion object {
        private const val DATABASE_NAME = "gravida_digital.db" // Nome do arquivo do banco
        private const val DATABASE_VERSION = 3 // Versão do banco para controle de upgrade

        // Nomes e colunas da tabela de usuários
        const val TABLE_USUARIOS = "usuarios"
        const val COLUMN_ID = "id"
        const val COLUMN_NOME = "nome"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_SENHA = "senha"

        // Nomes e colunas da tabela de respostas
        const val TABLE_RESPOSTAS = "respostas"
        const val COLUMN_ID_RESPOSTA = "id_resposta"
        const val COLUMN_ID_USUARIO = "id_usuario"
        const val COLUMN_DATA = "data"
        const val COLUMN_PERGUNTA_NUM = "pergunta_numero"
        const val COLUMN_VALOR_RESPOSTA = "valor_resposta"
        const val COLUMN_ID_SECAO = "id_secao"

        // Nomes e colunas da tabela de fotos
        const val TABLE_FOTOS = "fotos"
        const val COLUMN_ID_FOTO = "id_foto"
        const val COLUMN_ACTIVITY = "activity"
        const val COLUMN_CAMINHO = "caminho"
        const val COLUMN_ID_SECAO_FOTO = "id_secao"

        // Nomes e colunas da tabela de seções
        const val TABLE_SECOES = "secoes"
        const val COLUMN_DATA_REALIZACAO = "data_realizacao"
    }

    // Criação das tabelas ao inicializar o banco
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
                $COLUMN_PERGUNTA_NUM INTEGER NOT NULL CHECK ($COLUMN_PERGUNTA_NUM BETWEEN 1 AND 8),
                $COLUMN_VALOR_RESPOSTA INTEGER NOT NULL CHECK ($COLUMN_VALOR_RESPOSTA BETWEEN 0 AND 3),
                $COLUMN_ID_SECAO INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_ID_USUARIO) REFERENCES $TABLE_USUARIOS($COLUMN_ID)
            )
        """)
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
        db.execSQL("""
            CREATE TABLE $TABLE_SECOES (
                $COLUMN_ID_SECAO INTEGER PRIMARY KEY,
                $COLUMN_ID_USUARIO INTEGER NOT NULL,
                $COLUMN_DATA_REALIZACAO TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY ($COLUMN_ID_USUARIO) REFERENCES $TABLE_USUARIOS($COLUMN_ID)
            )
        """)
    }

    // Atualização do banco (apaga tudo e recria)
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FOTOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RESPOSTAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SECOES")
        onCreate(db)
    }

    // Insere novo usuário na tabela
    fun cadastrarUsuario(nome: String, email: String, senha: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NOME, nome)
            put(COLUMN_EMAIL, email)
            put(COLUMN_SENHA, senha)
        }
        return db.insert(TABLE_USUARIOS, null, values)
    }

    // Verifica se existe usuário com email e senha correspondentes (login)
    fun verificarLogin(email: String, senha: String): Long {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT $COLUMN_ID FROM $TABLE_USUARIOS 
            WHERE $COLUMN_EMAIL = ? AND $COLUMN_SENHA = ?
        """, arrayOf(email, senha))
        return cursor.use { if (it.moveToFirst()) it.getLong(0) else -1L }
    }

    // Verifica se já existe usuário com o email fornecido
    fun verificarEmailExistente(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT $COLUMN_ID FROM $TABLE_USUARIOS 
            WHERE $COLUMN_EMAIL = ?
        """, arrayOf(email))
        return cursor.use { it.count > 0 }
    }

    // Salva a resposta de uma pergunta no banco
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

    // Retorna objeto Usuario a partir do ID (ou null se não encontrado)
    fun buscarUsuarioPorId(id: Int): Usuario? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_NOME, $COLUMN_EMAIL FROM $TABLE_USUARIOS WHERE $COLUMN_ID = ?",
            arrayOf(id.toString())
        )

        return if (cursor.moveToFirst()) {
            val nome = cursor.getString(0)
            val email = cursor.getString(1)
            cursor.close()
            Usuario(id, nome, email)
        } else {
            cursor.close()
            null
        }
    }

    // Busca todas as respostas associadas a uma seção e retorna em formato de mapa
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

    // Salva metadados de uma foto no banco
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

    // Registra nova seção com data/hora atual
    fun registrarSecao(idSecao: Int, idUsuario: Int): Long {
        val db = writableDatabase
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        val dataFormatada = sdf.format(Date())

        val values = ContentValues().apply {
            put(COLUMN_ID_SECAO, idSecao)
            put(COLUMN_ID_USUARIO, idUsuario)
            put(COLUMN_DATA_REALIZACAO, dataFormatada)
        }

        return db.insert(TABLE_SECOES, null, values)
    }

    // Fecha o banco (por causa do AutoCloseable)
    override fun close() {
        super.close()
    }
}
