package com.example.prototipo_gravida_digital

// Importações necessárias para funcionamento da activity, UI, rede e manipulação de dados
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class EleventhActivity : AppCompatActivity() {

    // Preferências compartilhadas para acessar dados persistidos (ex: id do usuário)
    private lateinit var sharedPref: SharedPreferences
    private var userId: Long = -1 // ID do usuário recuperado do SharedPreferences
    private var idSecaoAtual: Int = 0 // ID da seção atual do questionário

    // Componentes da UI
    private lateinit var checkTermos: CheckBox
    private lateinit var buttonFinalizar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eleventh) // Layout da activity

        // Inicialização das preferências compartilhadas e recuperação de dados
        sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        userId = sharedPref.getLong("user_id", -1)
        idSecaoAtual = sharedPref.getInt("current_section_id", 0)

        // Inicializa elementos da interface
        checkTermos = findViewById(R.id.checkTermos2)
        buttonFinalizar = findViewById(R.id.btFinalizar)
        buttonFinalizar.isEnabled = false // Botão só é ativado se o termo for aceito

        // Ativa/desativa botão com base na caixa de aceite dos termos
        checkTermos.setOnCheckedChangeListener { _, isChecked ->
            buttonFinalizar.isEnabled = isChecked
        }

        // Ao clicar no botão, verifica se os termos foram aceitos e envia dados
        buttonFinalizar.setOnClickListener {
            if (checkTermos.isChecked) {
                enviarDadosParaBanco()
            } else {
                Toast.makeText(this, "Por favor, aceite os termos para continuar", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Função que monta os dados locais e envia via API para o backend
    private fun enviarDadosParaBanco() {
        val dbHelper = DatabaseHelper(this)

        // Recupera respostas e dados do usuário do banco local SQLite
        val respostas = dbHelper.buscarRespostasPorSecao(idSecaoAtual)
        val usuario = dbHelper.buscarUsuarioPorId(userId.toInt())
        dbHelper.close()

        // Monta objeto JSON com os dados
        val json = JSONObject().apply {
            put("id_usuario", userId)
            put("id_secao", idSecaoAtual)
            put("nome", usuario?.nome ?: "")
            put("email", usuario?.email ?: "")

            // Array com as respostas do questionário
            put("respostas", JSONArray().apply {
                respostas.forEach { (perguntaNum, valor) ->
                    put(JSONObject().apply {
                        put("pergunta_num", perguntaNum)
                        put("valor_resposta", valor)
                    })
                }
            })

            // Array com as fotos capturadas e armazenadas temporariamente
            put("fotos", JSONArray().apply {
                FotosTempStorage.fotos.forEach { foto ->
                    put(JSONObject().apply {
                        put("activity", foto.activity)
                        put("base64", foto.base64)
                        put("filename", File(foto.caminho).name) // Nome do arquivo da foto
                    })
                }
            })
        }

        // Configura client HTTP com tipo de conteúdo JSON
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)

        // Define a requisição HTTP POST
        val request = Request.Builder()
            .url("http://192.168.0.5:3000/api/enviar-dados") // URL da API (local)
            .post(requestBody)
            .build()

        // Envia a requisição de forma assíncrona
        client.newCall(request).enqueue(object : Callback {
            // Caso falhe a conexão (timeout, rede, etc)
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EleventhActivity, "Falha na conexão: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("API_ERROR", "Falha ao enviar dados", e)
                }
            }

            // Resposta recebida com sucesso (ou erro do servidor)
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        // Dados foram aceitos pelo servidor
                        Toast.makeText(this@EleventhActivity, "Dados enviados com sucesso!", Toast.LENGTH_SHORT).show()

                        // Limpa as fotos armazenadas temporariamente
                        FotosTempStorage.fotos.clear()

                        // Remove o ID da seção salva nas preferências
                        sharedPref.edit().remove("current_section_id").apply()

                        // Redireciona para a tela de confirmação, limpando o histórico
                        val intent = Intent(this@EleventhActivity, ConfirmationActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Servidor respondeu com erro (ex: 400, 500)
                        Toast.makeText(this@EleventhActivity, "Erro no servidor: ${response.code}", Toast.LENGTH_LONG).show()
                        Log.e("API_ERROR", "Erro servidor: ${response.code} - ${response.body?.string()}")
                    }
                }
            }
        })
    }
}
