package com.example.prototipo_gravida_digital

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

    private lateinit var sharedPref: SharedPreferences
    private var userId: Long = -1
    private var idSecaoAtual: Int = 0
    private lateinit var checkTermos: CheckBox
    private lateinit var buttonFinalizar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eleventh)

        sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        userId = sharedPref.getLong("user_id", -1)
        idSecaoAtual = sharedPref.getInt("current_section_id", 0)

        checkTermos = findViewById(R.id.checkTermos2)
        buttonFinalizar = findViewById(R.id.btFinalizar)
        buttonFinalizar.isEnabled = false

        checkTermos.setOnCheckedChangeListener { _, isChecked ->
            buttonFinalizar.isEnabled = isChecked
        }

        buttonFinalizar.setOnClickListener {
            if (checkTermos.isChecked) {
                enviarDadosParaBanco()
            } else {
                Toast.makeText(this, "Por favor, aceite os termos para continuar", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun enviarDadosParaBanco() {
        val dbHelper = DatabaseHelper(this)
        val respostas = dbHelper.buscarRespostasPorSecao(idSecaoAtual)
        val usuario = dbHelper.buscarUsuarioPorId(userId.toInt())
        dbHelper.close()

        val json = JSONObject().apply {
            put("id_usuario", userId)
            put("id_secao", idSecaoAtual)
            put("nome", usuario?.nome ?: "")
            put("email", usuario?.email ?: "")

            put("respostas", JSONArray().apply {
                respostas.forEach { (perguntaNum, valor) ->
                    put(JSONObject().apply {
                        put("pergunta_num", perguntaNum)
                        put("valor_resposta", valor)
                    })
                }
            })

            put("fotos", JSONArray().apply {
                FotosTempStorage.fotos.forEach { foto ->
                    put(JSONObject().apply {
                        put("activity", foto.activity)
                        put("base64", foto.base64)
                        put("filename", File(foto.caminho).name)
                    })
                }
            })
        }

        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.0.5:3000/api/enviar-dados")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EleventhActivity, "Falha na conexão: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("API_ERROR", "Falha ao enviar dados", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EleventhActivity, "Dados enviados com sucesso!", Toast.LENGTH_SHORT).show()
                        FotosTempStorage.fotos.clear()

                        sharedPref.edit().remove("current_section_id").apply()

                        val intent = Intent(this@EleventhActivity, ConfirmationActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@EleventhActivity, "Erro no servidor: ${response.code}", Toast.LENGTH_LONG).show()
                        Log.e("API_ERROR", "Erro servidor: ${response.code} - ${response.body?.string()}")
                    }
                }
            }
        })
    }
}
