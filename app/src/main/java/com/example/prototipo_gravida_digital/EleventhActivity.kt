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
        setContentView(R.layout.activity_eleventh)

        val checkTermos = findViewById<CheckBox>(R.id.checkTermos2)
        val buttonFinalizar = findViewById<Button>(R.id.btFinalizar)

        checkTermos.setOnCheckedChangeListener { _, isChecked ->
            buttonFinalizar.isEnabled = isChecked
        }

        buttonFinalizar.setOnClickListener {
            if (checkTermos.isChecked) {
                enviarDadosParaBanco()
            } else {
                Toast.makeText(
                    this,
                    "Por favor, aceite os termos para continuar",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun enviarDadosParaBanco() {
        Toast.makeText(this, "Dados enviados com sucesso!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, ConfirmationActivity::class.java)
        startActivity(intent)
        finish()
    }
}