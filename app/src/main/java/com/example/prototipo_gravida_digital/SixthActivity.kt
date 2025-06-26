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

class SixthActivity : AppCompatActivity() {

    // Controle da câmera
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var sharedPref: SharedPreferences
    private var userId: Long = -1

    // Componentes de interface
    private lateinit var seekBar: SeekBar
    private lateinit var btnProxima: Button

    // Solicitação de permissão para acesso à câmera
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startCamera()
        else Toast.makeText(this, "Permissão da câmera negada", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sixth)

        // Obtém ID do usuário logado
        sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        userId = sharedPref.getLong("user_id", -1)

        if (userId == -1L) {
            Toast.makeText(this, "Usuário não identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Ajuste visual para considerar as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicialização dos componentes de UI
        seekBar = findViewById(R.id.seekBarResposta4)
        btnProxima = findViewById(R.id.btProxima4)

        // Ação do botão "Próxima"
        // Configuração do botão "Próxima" - MUDANÇA PRINCIPAL AQUI
        btnProxima.setOnClickListener {
            // Salva a resposta da pergunta no banco de dados (nova forma)
            DatabaseHelper(this).apply {
                salvarResposta(
                    idUsuario = userId.toInt(), // ID do usuário logado
                    perguntaNum = 4, // Número da pergunta
                    valor = seekBar.progress // Valor da resposta (0-3)
                )
                close()
            }

            // Avança para a próxima Activity
            startActivity(Intent(this, SeventhActivity::class.java))
        }

        // Inicialização do executor da câmera
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Solicita permissão ou inicia a câmera se já estiver concedida
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Inicia a câmera e prepara para capturar imagens
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageCapture
                )
                takeThreePhotos()
            } catch (e: Exception) {
                Log.e("CAMERA_DEBUG", "Erro ao iniciar câmera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Tira 3 fotos em sequência com intervalo entre elas
    private fun takeThreePhotos() {
        val handler = Handler(Looper.getMainLooper())
        val interval = 1500L // Intervalo de 1.5 segundos entre fotos

        repeat(3) { index ->
            handler.postDelayed({
                takeSilentPhoto("selfie_sixth_${index + 1}")
            }, interval * index)
        }
    }

    // Tira uma foto silenciosamente e salva no banco de dados
    private fun takeSilentPhoto(tag: String) {
        try {
            val dir = File(getExternalFilesDir(null), "Pictures/SelfieSixth")
            if (!dir.exists()) dir.mkdirs()

            val fileName = "$tag-${System.currentTimeMillis()}.jpg"
            val photoFile = File(dir, fileName)

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Log.d("CAMERA_DEBUG", "Foto $tag salva em: ${photoFile.absolutePath}")

                        // Salva o caminho da foto no banco de dados
                        DatabaseHelper(this@SixthActivity).apply {
                            salvarFoto(
                                idUsuario = userId.toInt(),
                                activity = "SixthActivity",
                                caminho = photoFile.absolutePath
                            )
                            close()
                        }
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e("CAMERA_DEBUG", "Erro ao salvar $tag", exc)
                        Toast.makeText(
                            this@SixthActivity,
                            "Erro ao salvar foto",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("CAMERA_DEBUG", "Erro geral em $tag", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
