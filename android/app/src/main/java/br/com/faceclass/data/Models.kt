package br.com.faceclass.data

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val user_id: Int,
    val role: String,
)

data class CurrentUser(
    val id_usuario: Int,
    val nome: String,
    val email: String,
    val perfil: String,
)

data class Lesson(
    val id_aula: Int,
    val id_turma: Int,
    val id_professor: Int,
    val disciplina: String,
    val inicio_previsto: String,
    val fim_previsto: String,
    val ativa: Boolean,
)

data class CreateLessonRequest(
    val id_turma: Int,
    val disciplina: String,
)

data class CheckInRequest(
    val id_aula: Int,
    val face_verification_token: String,
    val wifi_ssid: String? = null,
)

/** Assinatura geométrica normalizada do rosto; nenhuma foto é enviada ao banco. */
data class FaceSignatureRequest(val assinatura: List<Float>)

data class FaceCheckInRequest(
    val id_aula: Int,
    val assinatura: List<Float>,
    val wifi_ssid: String? = null,
)

data class FaceStatus(
    val cadastrada: Boolean,
    val face_cadastrada_em: String? = null,
)

data class Attendance(
    val id_presenca: Int,
    val id_aluno: Int,
    val id_aula: Int?,
    val dia_hora: String,
    val tipo_presenca: Boolean,
    val status: String,
    val minutos_atraso: Int,
    val percentual_atraso_adicionado: Int,
    val faltas_geradas_por_atraso: Int,
    val origem: String,
    val observacao: String?,
)

data class CheckInResponse(val message: String, val attendance: Attendance)

data class LatenessSummary(
    val periodo: String,
    val percentual_acumulado: Int,
    val faltas_convertidas: Int,
    val atrasos_no_mes: Int,
    val minutos_de_atraso_no_mes: Int,
    val tolerancia_inicial_minutos: Int,
    val bloco_atraso_minutos: Int,
    val percentual_por_bloco: Int,
)

data class StudentDashboardRow(
    val id_aluno: Int,
    val nome_aluno: String,
    val ra: String?,
    val presentes: Int,
    val atrasos: Int,
    val faltas: Int,
    val faltas_por_atraso: Int,
    val percentual_atraso_atual: Int,
    val ultima_presenca: String?,
)

data class ClassDashboard(
    val id_turma: Int,
    val turma: String,
    val ano_letivo: Int,
    val alunos: List<StudentDashboardRow>,
)
