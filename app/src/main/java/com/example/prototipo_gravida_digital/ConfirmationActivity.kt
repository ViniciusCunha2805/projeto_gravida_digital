package com.example.prototipo_gravida_digital

// Importações necessárias para funcionamento da activity e manipulação de dados
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

// Variáveis globais para armazenar preferências e IDs relevantes
private lateinit var sharedPref: SharedPreferences
private var userId: Long = -1             // ID do usuário logado
private var idSecaoAtual: Int = 0         // ID da seção do questionário atual

class ConfirmationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation) // Define o layout da tela de confirmação

        // Recupera dados salvos em SharedPreferences
        sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        userId = sharedPref.getLong("user_id", -1)
        idSecaoAtual = sharedPref.getInt("current_section_id", 0)

        // Referência ao botão "Voltar ao Início"
        val btnVoltarInicio = findViewById<Button>(R.id.btnVoltarInicio)

        // Define ação ao clicar no botão
        btnVoltarInicio.setOnClickListener {
            // Cria um Intent para retornar à MainActivity (tela inicial)
            val intent = Intent(this, MainActivity::class.java)
            // Limpa a pilha de activities anteriores e inicia uma nova tarefa
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish() // Encerra a ConfirmationActivity
        }
    }
}
