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

        // Referências aos componentes da interface
        val editNome = findViewById<EditText>(R.id.editNome)
        val editEmail = findViewById<EditText>(R.id.editEmail)
        val editSenha = findViewById<EditText>(R.id.editSenha)
        val editConfirmaSenha = findViewById<EditText>(R.id.editConfirmaSenha)

        // Ação do botão "Cadastrar"
        findViewById<Button>(R.id.btCadastrar2).setOnClickListener {
            val nome = editNome.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val senha = editSenha.text.toString()
            val confirmaSenha = editConfirmaSenha.text.toString()

            // Validação: Nome não pode estar vazio
            if (nome.isEmpty()) {
                editNome.error = "Digite seu nome completo"
                return@setOnClickListener
            }

            // Validação: E-mail não pode estar vazio e deve ser válido
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editEmail.error = "Digite um e-mail válido"
                return@setOnClickListener
            }

            // Validação: Senha deve ter no mínimo 6 caracteres
            if (senha.isEmpty() || senha.length < 6) {
                editSenha.error = "Senha deve ter no mínimo 6 caracteres"
                return@setOnClickListener
            }

            // Validação: As senhas devem coincidir
            if (senha != confirmaSenha) {
                editConfirmaSenha.error = "As senhas não coincidem"
                return@setOnClickListener
            }

            // Verificação se o e-mail já está cadastrado no banco de dados
            val dbHelper = DatabaseHelper(this)
            if (dbHelper.verificarEmailExistente(email)) {
                editEmail.error = "Este e-mail já está cadastrado"
                return@setOnClickListener
            }

            // Cadastro do novo usuário
            val idUsuario = dbHelper.cadastrarUsuario(nome, email, senha)

            if (idUsuario != -1L) {
                // Cadastro realizado com sucesso
                Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, ThirdActivity::class.java))
                finish()
            } else {
                // Falha ao cadastrar usuário
                Toast.makeText(this, "Erro ao cadastrar usuário", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
