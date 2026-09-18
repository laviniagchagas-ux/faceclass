package br.com.faceclass.data

import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/**
 * Troca mensagens técnicas de rede por uma explicação que faça sentido para
 * quem está usando o aplicativo. Quando a API envia um "detail", ele é
 * aproveitado para não esconder a causa real do problema.
 */
fun friendlyApiError(error: Throwable): String {
    if (error is IOException) {
        return "Não foi possível conectar ao servidor. Confira se o back-end está ligado e tente novamente."
    }

    if (error is HttpException) {
        val detail = runCatching {
            error.response()?.errorBody()?.string()?.let(::extractDetail)
        }.getOrNull()

        return when (error.code()) {
            401 -> "E-mail ou senha incorretos, ou sua sessão expirou. Faça login novamente."
            403 -> detail ?: "Sua conta não tem permissão para realizar esta ação."
            404 -> detail ?: "Não encontramos a informação solicitada. Atualize a tela e tente novamente."
            409 -> detail ?: "Este registro já existe ou esta presença já foi confirmada."
            422 -> detail ?: "Confira os dados informados e tente novamente."
            500 -> "O servidor encontrou um problema. Espere um instante e tente novamente."
            else -> detail ?: "Não foi possível concluir esta ação agora. Tente novamente."
        }
    }

    return "Ocorreu um problema inesperado. Tente novamente."
}

private fun extractDetail(body: String): String? = runCatching {
    val detail = JSONObject(body).opt("detail")
    when (detail) {
        is String -> detail.takeIf { it.isNotBlank() }
        is JSONArray -> detail.optJSONObject(0)?.optString("msg")?.takeIf { it.isNotBlank() }
        else -> null
    }
}.getOrNull()
