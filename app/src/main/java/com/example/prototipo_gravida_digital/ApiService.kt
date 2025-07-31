package com.example.prototipo_gravida_digital

// Importações necessárias para uso do Retrofit e GSON
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// Interface que define os endpoints da API utilizando Retrofit
interface ApiService {
    // Endpoint POST para enviar os dados da seção
    @POST("/api/enviar-dados")
    fun enviarDados(@Body dados: DadosSecao): Call<RespostaServidor>
}

// Data class que representa o corpo da requisição enviado ao backend
data class DadosSecao(
    @SerializedName("id_usuario")
    val idUsuario: Int, // ID do usuário

    @SerializedName("id_secao")
    val idSecao: Int, // ID da seção do questionário

    val respostas: List<Resposta>? = null, // Lista de respostas (opcional)
    val fotos: List<Foto>? = null,         // Lista de fotos (opcional)

    val nome: String? = null,              // Nome do usuário (opcional)
    val email: String? = null              // Email do usuário (opcional)
)

// Data class que representa uma única resposta no questionário
data class Resposta(
    @SerializedName("pergunta_num")
    val perguntaNum: Int, // Número da pergunta

    @SerializedName("valor_resposta")
    val valorResposta: Int // Valor selecionado como resposta
)

// Data class que representa uma foto capturada durante a seção
data class Foto(
    val activity: String, // Activity onde a foto foi tirada
    val caminho: String,  // Caminho local da foto no dispositivo
    val base64: String    // Representação da imagem codificada em Base64
)

// Data class que representa um usuário registrado no app
data class Usuario(
    val id: Int,          // ID do usuário
    val nome: String,     // Nome do usuário
    val email: String     // Email do usuário
)

// Data class que representa a resposta enviada pelo servidor após o POST
data class RespostaServidor(
    val success: Boolean, // Indica se a operação foi bem-sucedida
    val message: String   // Mensagem retornada pelo servidor
)

// Objeto singleton que cria uma instância configurada do Retrofit
object RetrofitClient {
    private const val BASE_URL = "http://192.168.0.5:3000" // Endereço base da API (IP local)

    // Instância única do ApiService, criada de forma preguiçosa (lazy)
    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL) // Define a URL base
            .addConverterFactory(GsonConverterFactory.create()) // Usa GSON para conversão JSON
            .build()
        retrofit.create(ApiService::class.java) // Cria implementação da interface da API
    }
}
