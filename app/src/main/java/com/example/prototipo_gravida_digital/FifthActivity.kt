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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FifthActivity : AppCompatActivity() {

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var sharedPref: SharedPreferences
    private var userId: Long = -1
    private var idSecaoAtual: Int = 0

    private lateinit var seekBar: SeekBar
    private lateinit var btnProxima: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startCamera()
        else Toast.makeText(this, "Permissão da câmera negada", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fifth)

        // Recupera SharedPreferences e IDs
        sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        userId = sharedPref.getLong("user_id", -1)
        idSecaoAtual = sharedPref.getInt("current_section_id", 0)

        if (userId == -1L) {
            Toast.makeText(this, "Usuário não identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (idSecaoAtual == 0) {
            Toast.makeText(this, "Erro: Sessão do questionário não iniciada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        seekBar = findViewById(R.id.seekBarResposta3)
        btnProxima = findViewById(R.id.btProxima3)

        btnProxima.setOnClickListener {
            val resposta = seekBar.progress.coerceIn(0..3)

            DatabaseHelper(this).apply {
                salvarResposta(
                    idUsuario = userId.toInt(),
                    perguntaNum = 3,
                    valor = resposta,
                    idSecao = idSecaoAtual
                )
                close()
            }

            startActivity(Intent(this, SixthActivity::class.java))
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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

    private fun takeThreePhotos() {
        val handler = Handler(Looper.getMainLooper())
        val interval = 1500L

        repeat(3) { index ->
            handler.postDelayed({
                takeSilentPhoto("selfie_fifth_${index + 1}")
            }, interval * index)
        }
    }

    private fun takeSilentPhoto(tag: String) {
        try {
            val dir = File(getExternalFilesDir(null), "Pictures/SelfieFifth")
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
                        Log.d("DEBUG_ID", "Salvando com userId = $userId, idSecao = $idSecaoAtual")

                        DatabaseHelper(this@FifthActivity).apply {
                            salvarFoto(
                                idUsuario = userId.toInt(),
                                activity = "FifthActivity",
                                caminho = photoFile.absolutePath,
                                idSecao = idSecaoAtual
                            )
                            close()
                        }
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e("CAMERA_DEBUG", "Erro ao salvar $tag", exc)
                        Toast.makeText(
                            this@FifthActivity,
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
