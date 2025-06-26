package com.example.prototipo_gravida_digital

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val editNome = findViewById<EditText>(R.id.editNome)
        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editSenha = findViewById<EditText>(R.id.editSenha)
        val editConfirmaSenha = findViewById<EditText>(R.id.editConfirmaSenha)

        findViewById<Button>(R.id.btCadastrar2).setOnClickListener {
            val nome = editNome.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val senha = editSenha.text.toString()
            val confirmaSenha = editConfirmaSenha.text.toString()

            // Validações básicas
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

            // Cadastro no banco de dados
            val dbHelper = DatabaseHelper(this)

            if (dbHelper.verificarEmailExistente(email)) {
                editEmail.error = "Este e-mail já está cadastrado"
                return@setOnClickListener
            }

            val idUsuario = dbHelper.cadastrarUsuario(nome, email, senha)

            if (idUsuario != -1L) {
                Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Erro ao cadastrar usuário", Toast.LENGTH_SHORT).show()
            }
        }
    }
}