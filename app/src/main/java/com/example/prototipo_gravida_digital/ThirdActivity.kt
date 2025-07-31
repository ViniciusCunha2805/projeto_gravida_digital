package com.example.prototipo_gravida_digital

// Importações necessárias para funcionamento da activity e uso de câmera, permissões, UI, etc.
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.*
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ThirdActivity : AppCompatActivity() {

    // Objeto para capturar imagens da câmera
    private lateinit var imageCapture: ImageCapture
    // Executor usado para operações da câmera em background
    private lateinit var cameraExecutor: ExecutorService
    // Armazenamento de preferências compartilhadas
    private lateinit var sharedPref: SharedPreferences
    // ID do usuário atual
    private var userId: Long = -1
    // ID da seção atual do questionário
    private var idSecaoAtual: Int = 0

    // Componentes da UI
    private lateinit var seekBar: SeekBar
    private lateinit var btnProxima: Button

    // Lançador de solicitação de permissão para usar a câmera
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startCamera() // Se a permissão for concedida, inicia a câmera
        else Toast.makeText(this, "Permissão da câmera negada", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Ativa layout edge-to-edge (modo imersivo)
        setContentView(R.layout.activity_third)

        sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        userId = sharedPref.getLong("user_id", -1)

        // Verifica se o usuário foi identificado corretamente
        if (userId == -1L) {
            Toast.makeText(this, "Usuário não identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Recupera ID da seção atual, ou cria uma nova se não houver
        idSecaoAtual = sharedPref.getInt("current_section_id", 0)
        if (idSecaoAtual == 0) {
            val newSectionId = (System.currentTimeMillis()/1000).toInt()
            sharedPref.edit().putInt("current_section_id", newSectionId).apply()
            idSecaoAtual = newSectionId

            val dataRealizacao = getDataHoraAtual()

            // Registra nova seção no banco local
            DatabaseHelper(this).apply {
                registrarSecao(idSecaoAtual, userId.toInt())
                close()
            }
        }

        // Ajusta os insets do sistema para que o layout não fique encoberto
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializa componentes da UI
        seekBar = findViewById(R.id.seekBarResposta)
        btnProxima = findViewById(R.id.btProxima)

        // Ao clicar em "Próxima", salva a resposta e avança para próxima activity
        btnProxima.setOnClickListener {
            val resposta = seekBar.progress.coerceIn(0..3) // Garante que o valor fique entre 0 e 3

            DatabaseHelper(this).apply {
                salvarResposta(userId.toInt(), 1, resposta, idSecaoAtual)
                close()
            }

            startActivity(Intent(this, FourthActivity::class.java))
        }

        // Executor para operações com câmera
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Verifica permissão da câmera ou solicita
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Retorna a data e hora atual formatada para armazenamento
    private fun getDataHoraAtual(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        return sdf.format(Date())
    }

    // Inicia a câmera e vincula ao ciclo de vida da activity
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, imageCapture)
                takeThreePhotos() // Captura fotos automaticamente após inicialização
            } catch (e: Exception) {
                Log.e("CAMERA_DEBUG", "Erro ao iniciar câmera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Agendamento da captura de 1 foto (nome da função sugere 3, mas atualmente só tira 1)
    private fun takeThreePhotos() {
        val handler = Handler(Looper.getMainLooper())
        val interval = 1500L // Tempo entre fotos (ms)

        repeat(1) { index ->
            handler.postDelayed({
                takeSilentPhoto("selfie_third_${index + 1}")
            }, interval * index)
        }
    }

    // Captura silenciosamente uma foto e salva no diretório privado
    private fun takeSilentPhoto(tag: String) {
        try {
            val dir = File(getExternalFilesDir(null), "Pictures/SelfieThird")
            if (!dir.exists()) dir.mkdirs()

            val fileName = "$tag-${System.currentTimeMillis()}.jpg"
            val photoFile = File(dir, fileName)

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    // Callback de sucesso ao salvar a foto
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Log.d("CAMERA_DEBUG", "Foto $tag salva em: ${photoFile.absolutePath}")

                        val base64 = encodeImageToBase64(photoFile)

                        // Armazena a foto na memória temporária (provavelmente será enviada depois)
                        FotosTempStorage.fotos.add(
                            Foto(
                                activity = "ThirdActivity",
                                caminho = photoFile.absolutePath,
                                base64 = base64
                            )
                        )
                    }

                    // Callback de erro ao capturar foto
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("CAMERA_DEBUG", "Erro ao salvar $tag", exc)
                        Toast.makeText(this@ThirdActivity, "Erro ao salvar foto", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("CAMERA_DEBUG", "Erro geral ao tirar foto", e)
        }
    }

    // Converte a imagem capturada para Base64 para envio ou armazenamento
    private fun encodeImageToBase64(file: File): String {
        val bytes = file.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // Libera o executor da câmera ao destruir a activity
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
