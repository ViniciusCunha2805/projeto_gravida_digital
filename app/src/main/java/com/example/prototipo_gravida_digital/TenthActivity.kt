package com.example.prototipo_gravida_digital

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TenthActivity : AppCompatActivity() {

    // Variável para controle do recurso de captura de imagem da câmera
    private lateinit var imageCapture: ImageCapture

    // Executor para gerenciar operações da câmera em thread separada
    private lateinit var cameraExecutor: ExecutorService

    // Referência para SharedPreferences para recuperar dados salvos (como ID do usuário)
    private lateinit var sharedPref: SharedPreferences

    // Armazena o ID do usuário logado, padrão -1 (não encontrado)
    private var userId: Long = -1

    // Referências para componentes da interface gráfica
    private lateinit var seekBar: SeekBar
    private lateinit var btnProxima: Button

    // Registrador para solicitar permissão da câmera e lidar com o resultado
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Se permissão concedida, inicia a câmera
            startCamera()
        } else {
            // Caso permissão negada, informa usuário
            Toast.makeText(this, "Permissão da câmera negada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ativa modo edge-to-edge para conteúdo ocupar toda a tela, respeitando áreas do sistema
        enableEdgeToEdge()

        // Define o layout XML da tela
        setContentView(R.layout.activity_tenth)

        // Recupera SharedPreferences para acessar dados persistidos
        sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Obtém o ID do usuário, padrão -1 indica não encontrado
        userId = sharedPref.getLong("user_id", -1)

        // Verifica se ID do usuário é válido, caso contrário fecha a activity com aviso
        if (userId == -1L) {
            Toast.makeText(this, "Usuário não identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Ajusta o padding da view principal para evitar conteúdo escondido atrás de barras do sistema (status, navegação)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Liga variáveis às views pelo ID definido no XML
        seekBar = findViewById(R.id.seekBarResposta8)
        btnProxima = findViewById(R.id.btProxima8)

        // Configuração do botão "Próxima" - MUDANÇA PRINCIPAL AQUI
        btnProxima.setOnClickListener {
            // Salva a resposta da pergunta no banco de dados (nova forma)
            DatabaseHelper(this).apply {
                salvarResposta(
                    idUsuario = userId.toInt(), // ID do usuário logado
                    perguntaNum = 8, // Número da pergunta
                    valor = seekBar.progress // Valor da resposta (0-3)
                )
                close()
            }

            // Avança para a próxima Activity
            startActivity(Intent(this, EleventhActivity::class.java))
        }

        // Inicializa executor para operações da câmera em segundo plano
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Verifica se app já tem permissão para usar a câmera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            // Caso positivo, inicia câmera imediatamente
            startCamera()
        } else {
            // Caso contrário, solicita permissão para o usuário
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Configura e inicia o uso da câmera frontal para capturar imagens.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Quando o provider estiver pronto, configura a câmera
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Cria objeto ImageCapture para tirar fotos
            imageCapture = ImageCapture.Builder().build()

            try {
                // Desvincula todas as câmeras vinculadas para evitar conflitos
                cameraProvider.unbindAll()

                // Vincula a câmera frontal ao ciclo de vida da activity, para iniciar preview e captura
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageCapture
                )

                // Após iniciar a câmera, dispara sequência de fotos automáticas
                takeThreePhotos()

            } catch (e: Exception) {
                // Caso ocorra erro, registra log para ajudar na depuração
                Log.e("CAMERA_DEBUG", "Erro ao iniciar câmera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Tira três fotos em sequência com intervalo de 1.5 segundos entre elas.
     */
    private fun takeThreePhotos() {
        val handler = Handler(Looper.getMainLooper())
        val interval = 1500L // Intervalo em milissegundos (1,5 segundos)

        repeat(3) { index ->
            // Agenda a captura de cada foto com atraso progressivo
            handler.postDelayed({
                takeSilentPhoto("selfie_tenth_${index + 1}")
            }, interval * index)
        }
    }

    /**
     * Captura uma foto silenciosa e salva no armazenamento interno.
     *
     * @param tag Nome para identificar a foto e nomear o arquivo.
     */
    private fun takeSilentPhoto(tag: String) {
        try {
            // Diretório onde as fotos serão salvas (interno do app)
            val dir = File(getExternalFilesDir(null), "Pictures/SelfieTenth")

            // Cria a pasta caso não exista
            if (!dir.exists()) dir.mkdirs()

            // Gera nome único para o arquivo com timestamp
            val fileName = "$tag-${System.currentTimeMillis()}.jpg"
            val photoFile = File(dir, fileName)

            // Configura opções de saída para salvar a imagem no arquivo
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            // Tira a foto usando a API da câmera
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        // Foto salva com sucesso, registra caminho e log
                        Log.d("CAMERA_DEBUG", "Foto $tag salva em: ${photoFile.absolutePath}")

                        // Salva caminho da foto no banco de dados para referência futura
                        DatabaseHelper(this@TenthActivity).apply {
                            salvarFoto(
                                idUsuario = userId.toInt(),
                                activity = "TenthActivity",
                                caminho = photoFile.absolutePath
                            )
                            close()
                        }
                    }

                    override fun onError(exc: ImageCaptureException) {
                        // Caso haja erro ao salvar a foto, mostra mensagem e loga erro
                        Log.e("CAMERA_DEBUG", "Erro ao salvar $tag", exc)
                        Toast.makeText(
                            this@TenthActivity,
                            "Erro ao salvar foto",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        } catch (e: Exception) {
            // Captura qualquer erro inesperado na captura da foto
            Log.e("CAMERA_DEBUG", "Erro geral em $tag", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Encerra o executor da câmera para liberar recursos ao fechar a activity
        cameraExecutor.shutdown()
    }
}
