package com.example.prototipo_gravida_digital

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/api/enviar-dados")
    fun enviarDados(@Body dados: DadosSecao): Call<RespostaServidor>
}

data class DadosSecao(
    @SerializedName("id_usuario")
    val idUsuario: Int,

    @SerializedName("id_secao")
    val idSecao: Int,

    val respostas: List<Resposta>? = null,
    val fotos: List<Foto>? = null,

    val nome: String? = null,
    val email: String? = null
)

data class Resposta(
    @SerializedName("pergunta_num")
    val perguntaNum: Int,

    @SerializedName("valor_resposta")
    val valorResposta: Int
)

data class Foto(
    val activity: String,
    val caminho: String,
    val base64: String
)

data class Usuario(
    val id: Int,
    val nome: String,
    val email: String
)

data class RespostaServidor(
    val success: Boolean,
    val message: String
)

object RetrofitClient {
    private const val BASE_URL = "http://192.168.0.5:3000" // Troque pelo seu IP se necessário

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}
