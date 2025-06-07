package com.example.prototipo_gravida_digital

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editEmail = findViewById<EditText>(R.id.editEmail2)
        val editSenha = findViewById<EditText>(R.id.editSenha2)
        val checkTermos = findViewById<CheckBox>(R.id.checkTermos)
        val btnEntrar = findViewById<Button>(R.id.btEntrar)
        val btnCadastrar = findViewById<Button>(R.id.btCadastrar)

        btnEntrar.setOnClickListener {
            val email = editEmail.text.toString().trim()
            val senha = editSenha.text.toString().trim()

            // Validações básicas
            if (email.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!checkTermos.isChecked) {
                Toast.makeText(this, "Aceite os termos para continuar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificação no banco de dados
            val dbHelper = DatabaseHelper(this)
            val userId = dbHelper.verificarLogin(email, senha) // Agora retorna Long (ID ou -1)

            if (userId != -1L) {
                // Armazena o ID do usuário logado
                val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putLong("user_id", userId)
                    apply()
                }

                Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ThirdActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "E-mail ou senha incorretos", Toast.LENGTH_SHORT).show()
            }
        }

        btnCadastrar.setOnClickListener {
            if (!checkTermos.isChecked) {
                Toast.makeText(this, "Aceite os termos para criar uma conta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, SecondActivity::class.java))
        }
    }
}