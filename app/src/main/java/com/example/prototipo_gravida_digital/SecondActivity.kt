package com.example.prototipo_gravida_digital

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val editNome = findViewById<EditText>(R.id.editNome)
        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editSenha = findViewById<EditText>(R.id.editSenha)
        val editConfirmaSenha = findViewById<EditText>(R.id.editConfirmaSenha)
        val btnCadastrar = findViewById<Button>(R.id.btCadastrar2)

        btnCadastrar.setOnClickListener {
            val nome = editNome.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val senha = editSenha.text.toString()
            val confirmaSenha = editConfirmaSenha.text.toString()

            if (nome.isEmpty()) {
                editNome.error = "Digite seu nome completo"
                return@setOnClickListener
            }

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editEmail.error = "Digite um e-mail válido"
                return@setOnClickListener
            }

            if (senha.isEmpty() || senha.length < 6) {
                editSenha.error = "Senha deve ter no mínimo 6 caracteres"
                return@setOnClickListener
            }

            if (senha != confirmaSenha) {
                editConfirmaSenha.error = "As senhas não coincidem"
                return@setOnClickListener
            }

            DatabaseHelper(this).use { dbHelper ->
                if (dbHelper.verificarEmailExistente(email)) {
                    editEmail.error = "Este e-mail já está cadastrado"
                    return@setOnClickListener
                }

                val idUsuario = dbHelper.cadastrarUsuario(nome, email, senha)

                if (idUsuario > 0) {
                    Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()

                    // ⬇️ Envia os dados para o servidor Flask
                    sincronizarUsuario(nome, email, senha)

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Erro ao cadastrar usuário", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sincronizarUsuario(nome: String, email: String, senha: String) {
        val client = OkHttpClient()

        val json = JSONObject().apply {
            put("nome", nome)
            put("email", email)
            put("senha", senha)
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://192.168.0.3:5000/sync_usuario") // troque para o IP do seu servidor Flask
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Apenas loga o erro, não impede o cadastro
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    println("Usuário sincronizado com sucesso")
                } else {
                    println("Erro ao sincronizar usuário: ${response.code}")
                }
            }
        })
    }
}

