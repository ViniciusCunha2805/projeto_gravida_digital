package com.example.prototipo_gravida_digital

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast

class EleventhActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Define o layout XML que será usado nesta Activity
        setContentView(R.layout.activity_eleventh)

        // Referência ao CheckBox do layout para aceitar os termos
        val checkTermos = findViewById<CheckBox>(R.id.checkTermos2)
        // Referência ao botão de finalizar
        val buttonFinalizar = findViewById<Button>(R.id.btFinalizar)

        // Listener para habilitar ou desabilitar o botão Finalizar
        // conforme o usuário marca ou desmarca o CheckBox
        checkTermos.setOnCheckedChangeListener { _, isChecked ->
            buttonFinalizar.isEnabled = isChecked
        }

        // Evento de clique no botão Finalizar
        buttonFinalizar.setOnClickListener {
            // Verifica se o CheckBox está marcado
            if (checkTermos.isChecked) {
                // Se marcado, chama função para enviar os dados para o banco
                enviarDadosParaBanco()
            } else {
                // Se não marcado, mostra uma mensagem solicitando aceite dos termos
                Toast.makeText(
                    this,
                    "Por favor, aceite os termos para continuar",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Função simulada para envio dos dados para o banco de dados.
     * Aqui só exibe uma mensagem de sucesso e navega para a próxima tela.
     */
    private fun enviarDadosParaBanco() {
        Toast.makeText(this, "Dados enviados com sucesso!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, ConfirmationActivity::class.java)
        startActivity(intent)
        finish()
    }
}
