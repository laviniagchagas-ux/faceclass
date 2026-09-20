package br.com.faceclass

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.faceclass.data.Attendance
import br.com.faceclass.data.ClassDashboard
import br.com.faceclass.data.Lesson
import br.com.faceclass.data.LatenessSummary
import br.com.faceclass.ui.screens.FaceClassLoginScreen
import br.com.faceclass.ui.screens.FaceClassParentHome
import br.com.faceclass.ui.screens.FaceClassStudentHome
import br.com.faceclass.ui.screens.FaceClassTeacherHome
import br.com.faceclass.ui.theme.FaceClassTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

private val PrimaryPurple = Color(0xFF6B46C1)
private val DarkPurple = Color(0xFF553C9A)
private val LightPurple = Color(0xFFF3E8FF)
private val AppBackground = Color(0xFFF8F9FE)
private val TextMain = Color(0xFF2D3748)
private val TextMuted = Color(0xFF718096)
private val Green = Color(0xFF38A169)
private val Red = Color(0xFFE53E3E)
private val Orange = Color(0xFFDD6B20)

private data class FaceReaderStatus(
    val title: String,
    val detail: String,
    val readyToCheckIn: Boolean = false,
    val signature: List<Float>? = null,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.rgb(248, 249, 254)))
        setContent {
            FaceClassTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
                    FaceClassApp()
                }
            }
        }
    }

}

@Composable
private fun FaceClassApp(viewModel: FaceClassViewModel = viewModel()) {
    val state by viewModel.state
    when (val user = state.user) {
        null -> FaceClassLoginScreen(
            loading = state.isLoading,
            message = state.message,
            onLogin = viewModel::login,
            onClearMessage = viewModel::clearMessage,
        )

        else -> when (user.perfil) {
            "ALUNO" -> StudentHome(
                name = user.nome,
                lessons = state.lessons,
                attendance = state.attendance,
                lateness = state.lateness,
                faceRegistered = state.faceRegistered,
                loading = state.isLoading,
                message = state.message,
                onRefresh = viewModel::refreshStudentData,
                onEnrollFace = viewModel::enrollFace,
                onFaceCheckIn = viewModel::faceCheckIn,
                onLogout = viewModel::logout,
                onClearMessage = viewModel::clearMessage,
            )

            "PROFESSOR" -> TeacherHome(
                name = user.nome,
                dashboard = state.dashboard,
                loading = state.isLoading,
                message = state.message,
                onCreateLesson = viewModel::createLesson,
                onLoadDashboard = viewModel::loadDashboard,
                onLogout = viewModel::logout,
                onClearMessage = viewModel::clearMessage,
            )

            "RESPONSAVEL" -> FaceClassParentHome(
                name = user.nome,
                attendance = state.attendance,
                onLogout = viewModel::logout,
            )

            else -> AdminHome(name = user.nome, onLogout = viewModel::logout)
        }
    }
}

@Composable
private fun LoginScreen(
    loading: Boolean,
    message: String?,
    onLogin: (String, String) -> Unit,
    onClearMessage: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("aluno@faceclass.com") }
    var password by rememberSaveable { mutableStateOf("Aluno2026!") }

    Box(Modifier.fillMaxSize().background(AppBackground)) {
        Box(
            Modifier.fillMaxWidth().height(245.dp)
                .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                .background(PrimaryPurple)
                .padding(horizontal = 24.dp, vertical = 48.dp),
        ) {
            BrandLogo(large = true)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 164.dp, bottom = 24.dp),
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Entrar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = TextMain)
                    Text("Acesse sua conta FaceClass", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(22.dp))
                    FieldLabel("E-mail")
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("seuemail@escola.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(14.dp))
                    FieldLabel("Senha")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    MessageCard(message, onClearMessage)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { onLogin(email, password) },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Entrar na conta", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Demonstração: aluno@faceclass.com / Aluno2026!",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun StudentHome(
    name: String,
    lessons: List<Lesson>,
    attendance: List<Attendance>,
    lateness: LatenessSummary?,
    faceRegistered: Boolean,
    loading: Boolean,
    message: String?,
    onRefresh: () -> Unit,
    onEnrollFace: (List<Float>) -> Unit,
    onFaceCheckIn: (Int, List<Float>) -> Unit,
    onLogout: () -> Unit,
    onClearMessage: () -> Unit,
) {
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    var registeringFace by remember { mutableStateOf(false) }

    if (registeringFace) {
        FaceEnrollmentCamera(
            loading = loading,
            onBack = { registeringFace = false },
            onConfirm = { signature ->
                onEnrollFace(signature)
                registeringFace = false
            },
        )
        return
    }

    if (selectedLesson != null) {
        FaceDemoCamera(
            lesson = selectedLesson!!,
            loading = loading,
            onBack = { selectedLesson = null },
            onConfirm = { signature ->
                onFaceCheckIn(selectedLesson!!.id_aula, signature)
                selectedLesson = null
            },
        )
        return
    }

    FaceClassStudentHome(
        name = name,
        lessons = lessons,
        attendance = attendance,
        faceRegistered = faceRegistered,
        loading = loading,
        message = message,
        onRefresh = onRefresh,
        onOpenFace = { selectedLesson = it },
        onEnrollFace = { registeringFace = true },
        onLogout = onLogout,
        onClearMessage = onClearMessage,
    )
}

private enum class StudentTab { HOME, LESSONS, LATENESS, REPORTS }

@Composable
private fun StudentHeader(name: String, onRefresh: () -> Unit, refreshing: Boolean) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(105.dp)
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(PrimaryPurple).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandLogo()
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onRefresh, enabled = !refreshing) {
                Text(if (refreshing) "..." else "↻", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Olá, ${firstName(name)}!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = TextMain)
                Text("Presença em tempo real.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(LightPurple),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials(name), color = PrimaryPurple, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StudentDashboard(
    name: String,
    lessons: List<Lesson>,
    attendance: List<Attendance>,
    faceRegistered: Boolean,
    loading: Boolean,
    message: String?,
    onClearMessage: () -> Unit,
    onChooseLesson: (Lesson) -> Unit,
    onEnrollFace: () -> Unit,
) {
    val activeLesson = lessons.firstOrNull()
    val alreadyChecked = activeLesson?.let { lesson -> attendance.any { it.id_aula == lesson.id_aula } } == true
    val present = attendance.count { it.status == "PRESENTE" }
    val absent = attendance.count { it.status == "FALTA" }
    val total = attendance.size
    val frequency = if (total == 0) 0 else (present * 100f / total).roundToInt()

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { MessageCard(message, onClearMessage) }
        if (loading) item { LoadingCard() }
        item {
            PresenceBanner(
                lesson = activeLesson,
                alreadyChecked = alreadyChecked,
                faceRegistered = faceRegistered,
                onClick = { activeLesson?.let(onChooseLesson) },
                onEnrollFace = onEnrollFace,
            )
        }
        item { FaceEnrollmentCard(registered = faceRegistered, onEnrollFace = onEnrollFace) }
        item { FrequencyCard(frequency, present, absent) }
        item { LastRegistrationCard(attendance.firstOrNull()) }
        item { AttendanceHistoryCard(attendance) }
        item {
            Text(
                if (activeLesson == null) "Para registrar presença, o professor precisa abrir uma aula agora." else "Aula atual: ${activeLesson.disciplina}",
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun PresenceBanner(
    lesson: Lesson?,
    alreadyChecked: Boolean,
    faceRegistered: Boolean,
    onClick: () -> Unit,
    onEnrollFace: () -> Unit,
) {
    val enabled = lesson != null && !alreadyChecked
    val gradient = if (enabled) {
        Brush.linearGradient(listOf(PrimaryPurple, DarkPurple))
    } else {
        Brush.linearGradient(listOf(Color(0xFFA0AEC0), Color(0xFF718096)))
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = {
            if (faceRegistered) onClick() else onEnrollFace()
        }),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 8.dp else 0.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().background(gradient).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Text("◉", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        lesson == null -> "Nenhuma aula disponível"
                        alreadyChecked -> "Presença confirmada"
                        !faceRegistered -> "Cadastre seu rosto primeiro"
                        else -> "Registrar presença"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    when {
                        lesson == null -> "Aguarde o professor abrir a chamada"
                        alreadyChecked -> "A chamada desta aula já foi registrada"
                        !faceRegistered -> "A câmera cria sua assinatura facial uma única vez"
                        else -> "${lesson.disciplina} • validar biometria facial"
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text("›", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun FaceEnrollmentCard(registered: Boolean, onEnrollFace: () -> Unit) {
    FaceCard(modifier = Modifier.clickable(onClick = onEnrollFace)) {
        Text(
            if (registered) "Rosto cadastrado" else "Cadastrar meu rosto",
            fontWeight = FontWeight.Bold,
            color = if (registered) Green else PrimaryPurple,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (registered) {
                "Sua assinatura facial está pronta para confirmar chamadas. Toque para atualizar."
            } else {
                "Faça o cadastro uma vez, olhando para a câmera e piscando os olhos."
            },
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FrequencyCard(frequency: Int, present: Int, absent: Int) {
    FaceCard {
        Text("Frequência escolar", fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Donut(frequency)
            Spacer(Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatisticDot(PrimaryPurple, present, "Presenças")
                StatisticDot(Red, absent, "Faltas")
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Os dados são calculados a partir das chamadas reais.", color = PrimaryPurple, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Donut(frequency: Int) {
    Box(Modifier.size(102.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 12.dp.toPx())
            drawArc(LightPurple, -90f, 360f, false, style = stroke)
            drawArc(PrimaryPurple, -90f, 360f * (frequency / 100f), false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$frequency%", fontWeight = FontWeight.ExtraBold, color = TextMain)
            Text("presenças", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun StatisticDot(color: Color, value: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(9.dp))
        Text("$value", fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.size(4.dp))
        Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LastRegistrationCard(record: Attendance?) {
    val style = attendanceStyle(record?.status)
    FaceCard {
        Text("Último registro", fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(style.background),
                contentAlignment = Alignment.Center,
            ) { Text(if (record == null) "–" else style.icon, color = style.color, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(attendanceLabel(record?.status), fontWeight = FontWeight.Bold, color = TextMain)
                Text(
                    record?.let { attendanceSubtitle(it) } ?: "A próxima chamada aparecerá aqui.",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (record != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(displayDate(record.dia_hora), color = TextMuted, style = MaterialTheme.typography.labelSmall)
                    Text(displayTime(record.dia_hora), color = TextMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun AttendanceHistoryCard(attendance: List<Attendance>) {
    FaceCard {
        Text("Histórico de presenças", fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.height(4.dp))
        Text("Registros reais enviados pela escola.", color = TextMuted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(12.dp))

        if (attendance.isEmpty()) {
            Text(
                "Ainda não há presenças, atrasos ou faltas registrados.",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            attendance.take(3).forEachIndexed { index, record ->
                AttendanceHistoryRow(record)
                if (index < minOf(attendance.size, 3) - 1) {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun AttendanceHistoryRow(record: Attendance) {
    val style = attendanceStyle(record.status)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(style.background),
            contentAlignment = Alignment.Center,
        ) { Text(style.icon, color = style.color, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(attendanceLabel(record.status), color = TextMain, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            Text(attendanceSubtitle(record), color = TextMuted, style = MaterialTheme.typography.labelSmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(displayDate(record.dia_hora), color = TextMuted, style = MaterialTheme.typography.labelSmall)
            Text(displayTime(record.dia_hora), color = TextMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private data class AttendanceStyle(val color: Color, val background: Color, val icon: String)

private fun attendanceStyle(status: String?): AttendanceStyle = when (status?.uppercase()) {
    "PRESENTE" -> AttendanceStyle(Green, Color(0xFFE6FFFA), "✓")
    "ATRASO" -> AttendanceStyle(Orange, Color(0xFFFFF5EB), "!")
    "FALTA" -> AttendanceStyle(Red, Color(0xFFFFE5E5), "×")
    else -> AttendanceStyle(PrimaryPurple, LightPurple, "•")
}

private fun attendanceLabel(status: String?): String = when (status?.uppercase()) {
    "PRESENTE" -> "Presença confirmada"
    "ATRASO" -> "Presença com atraso"
    "FALTA" -> "Falta registrada"
    else -> "Aguardando registro"
}

private fun attendanceSubtitle(record: Attendance): String = when {
    record.status.uppercase() == "ATRASO" && record.minutos_atraso > 0 -> "${record.minutos_atraso} min de atraso"
    record.status.uppercase() == "FALTA" -> record.observacao ?: "Falta informada pela chamada"
    else -> record.origem.lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
private fun StudentLessons(lessons: List<Lesson>, onChooseLesson: (Lesson) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Aulas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = TextMain)
            Text("Disciplinas disponíveis neste momento.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        if (lessons.isEmpty()) item { EmptyCard("Não há aulas em andamento agora.") }
        items(lessons, key = { it.id_aula }) { lesson ->
            FaceCard(modifier = Modifier.clickable { onChooseLesson(lesson) }) {
                Text(lesson.disciplina, fontWeight = FontWeight.Bold, color = TextMain)
                Spacer(Modifier.height(5.dp))
                Text("Das ${displayTime(lesson.inicio_previsto)} às ${displayTime(lesson.fim_previsto)}", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StudentReports(attendance: List<Attendance>) {
    val present = attendance.count { it.status == "PRESENTE" }
    val delayed = attendance.count { it.status == "ATRASO" }
    val absent = attendance.count { it.status == "FALTA" }
    val total = attendance.size
    val frequency = if (total == 0) 0 else (present * 100f / total).roundToInt()
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Relatórios", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = TextMain)
            Text("Resumo de frequência do aluno.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            FaceCard {
                ReportLine("Total de chamadas", total.toString())
                ReportLine("Presenças confirmadas", present.toString())
                ReportLine("Atrasos", delayed.toString())
                ReportLine("Faltas", absent.toString())
                Spacer(Modifier.height(10.dp))
                Text("Aproveitamento atual: $frequency%", color = PrimaryPurple, fontWeight = FontWeight.ExtraBold)
            }
        }
        item { Text("Registros", fontWeight = FontWeight.Bold, color = TextMain) }
        items(attendance, key = { it.id_presenca }) { LastRegistrationCard(it) }
    }
}

@Composable
private fun StudentLateness(summary: LatenessSummary?, loading: Boolean) {
    val currentPercentage = summary?.percentual_acumulado ?: 0
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Atrasos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = TextMain)
            Text("Acompanhe seu saldo de atrasos do mês.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
        if (loading) item { LoadingCard() }
        item { LatenessGauge(currentPercentage) }
        item {
            FaceCard {
                Text("Como funciona", fontWeight = FontWeight.Bold, color = TextMain)
                Spacer(Modifier.height(14.dp))
                LatenessRule("Tolerância inicial", "${summary?.tolerancia_inicial_minutos ?: 10} minutos após o início da aula")
                LatenessRule("Após a tolerância", "Cada bloco de ${summary?.bloco_atraso_minutos ?: 5} minutos soma ${summary?.percentual_por_bloco ?: 10}%")
                LatenessRule("Ao atingir 100%", "O saldo é convertido automaticamente em uma falta")
            }
        }
        item {
            FaceCard {
                Text("Resumo deste mês", fontWeight = FontWeight.Bold, color = TextMain)
                Spacer(Modifier.height(12.dp))
                ReportLine("Atrasos registrados", (summary?.atrasos_no_mes ?: 0).toString())
                ReportLine("Minutos em atraso", "${summary?.minutos_de_atraso_no_mes ?: 0} min")
                ReportLine("Faltas convertidas", (summary?.faltas_convertidas ?: 0).toString())
            }
        }
    }
}

@Composable
private fun LatenessGauge(percentage: Int) {
    val safePercentage = percentage.coerceIn(0, 100)
    FaceCard {
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                val stroke = 22.dp.toPx()
                val diameter = size.width - stroke
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = LightPurple,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = if (safePercentage >= 80) Red else PrimaryPurple,
                    startAngle = 180f,
                    sweepAngle = 180f * (safePercentage / 100f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("$safePercentage%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = TextMain)
                Text("atrasos acumulados", color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LatenessRule(title: String, detail: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(title, color = PrimaryPurple, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(detail, color = TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReportLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
        Text(value, color = TextMain, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StudentBottomNavigation(tab: StudentTab, onTabChange: (StudentTab) -> Unit, onLogout: () -> Unit) {
    Surface(shadowElevation = 10.dp, color = Color.White) {
        Row(Modifier.fillMaxWidth().height(67.dp), verticalAlignment = Alignment.CenterVertically) {
            BottomNavItem("⌂", "Início", tab == StudentTab.HOME) { onTabChange(StudentTab.HOME) }
            BottomNavItem("▣", "Aulas", tab == StudentTab.LESSONS) { onTabChange(StudentTab.LESSONS) }
            BottomNavItem("◴", "Atrasos", tab == StudentTab.LATENESS) { onTabChange(StudentTab.LATENESS) }
            BottomNavItem("▤", "Relatórios", tab == StudentTab.REPORTS) { onTabChange(StudentTab.REPORTS) }
            BottomNavItem("↪", "Sair", false, onLogout)
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(icon, color = if (selected) PrimaryPurple else TextMuted)
        Text(label, color = if (selected) PrimaryPurple else TextMuted, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun TeacherHome(
    name: String,
    dashboard: ClassDashboard?,
    loading: Boolean,
    message: String?,
    onCreateLesson: (String, String) -> Unit,
    onLoadDashboard: (String) -> Unit,
    onLogout: () -> Unit,
    onClearMessage: () -> Unit,
) {
    FaceClassTeacherHome(name = name, onLogout = onLogout)
}

@Composable
private fun AdminHome(name: String, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AppBackground)) {
        SimplePurpleHeader("FaceClass", "Administrador: $name", onLogout)
        FaceCard(modifier = Modifier.padding(20.dp)) {
            Text("Dados iniciais configurados", fontWeight = FontWeight.ExtraBold, color = TextMain)
            Text("O app móvel já atende os fluxos de professor e aluno. Os cadastros administrativos continuam disponíveis na API.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FaceDemoCamera(
    lesson: Lesson,
    loading: Boolean,
    onBack: () -> Unit,
    onConfirm: (List<Float>) -> Unit,
) {
    FaceCameraScreen(
        title = "Biometria facial",
        subtitle = lesson.disciplina,
        actionLabel = "Registrar presença",
        loading = loading,
        onBack = onBack,
        onConfirm = onConfirm,
    )
}

@Composable
private fun FaceEnrollmentCamera(
    loading: Boolean,
    onBack: () -> Unit,
    onConfirm: (List<Float>) -> Unit,
) {
    FaceCameraScreen(
        title = "Cadastrar meu rosto",
        subtitle = "A assinatura fica vinculada à sua conta",
        actionLabel = "Cadastrar rosto",
        loading = loading,
        onBack = onBack,
        onConfirm = onConfirm,
    )
}

@Composable
private fun FaceCameraScreen(
    title: String,
    subtitle: String,
    actionLabel: String,
    loading: Boolean,
    onBack: () -> Unit,
    onConfirm: (List<Float>) -> Unit,
) {
    var readerStatus by remember {
        mutableStateOf(
            FaceReaderStatus(
                title = "Procurando rosto",
                detail = "Centralize seu rosto dentro do círculo.",
            )
        )
    }
    Column(Modifier.fillMaxSize().background(AppBackground)) {
        SimplePurpleHeader(title, subtitle, onBack, exitLabel = "←")
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Posicione o rosto dentro do círculo e pisque os olhos.", color = TextMuted)
            Box(
                Modifier.size(240.dp).clip(CircleShape).background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onReaderStatus = { readerStatus = it },
                )
            }
            FaceCard {
                Text(readerStatus.title, fontWeight = FontWeight.Bold, color = if (readerStatus.readyToCheckIn) Green else PrimaryPurple)
                Text(readerStatus.detail, color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { readerStatus.signature?.let(onConfirm) },
                enabled = readerStatus.readyToCheckIn && readerStatus.signature != null && !loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            ) { Text(if (loading) "Salvando..." else actionLabel, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
@OptIn(ExperimentalGetImage::class)
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onReaderStatus: (FaceReaderStatus) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val blinkWasSeen = remember { AtomicBoolean(false) }
    val detector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setMinFaceSize(0.18f)
            .build()
        FaceDetection.getClient(options)
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            detector.close()
            analyzerExecutor.shutdown()
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Exception) {
                // A tela pode ser fechada antes de a câmera terminar de iniciar.
            }
        }
    }

    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    if (!granted) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("Permita a câmera para continuar", color = Color.White) }
        return
    }
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PreviewView(viewContext).also { previewView ->
                // No emulador, TextureView é mais confiável que a SurfaceView padrão.
                // Em aparelho físico, continua mostrando a mesma prévia da câmera.
                previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                val providerFuture = ProcessCameraProvider.getInstance(viewContext)
                providerFuture.addListener({
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees,
                        )
                        detector.process(image)
                            .addOnSuccessListener { faces ->
                                val status = when {
                                    faces.isEmpty() -> FaceReaderStatus(
                                        "Nenhum rosto encontrado",
                                        "Centralize apenas o seu rosto no círculo.",
                                    )

                                    faces.size > 1 -> FaceReaderStatus(
                                        "Mais de um rosto detectado",
                                        "A chamada deve ser feita por uma pessoa de cada vez.",
                                    )

                                    faces.first().boundingBox.width() < imageProxy.width * 0.24f -> FaceReaderStatus(
                                        "Aproxime-se um pouco",
                                        "Seu rosto precisa ocupar melhor o círculo.",
                                    )

                                    else -> {
                                        val face = faces.first()
                                        val leftEye = face.leftEyeOpenProbability
                                        val rightEye = face.rightEyeOpenProbability
                                        val signature = buildFaceSignature(face)
                                        when {
                                            signature == null -> FaceReaderStatus(
                                                "Ajuste seu rosto",
                                                "Olhe de frente para a câmera, com boa iluminação.",
                                            )

                                            leftEye == null || rightEye == null -> FaceReaderStatus(
                                                "Olhe para a câmera",
                                                "Mantenha o rosto de frente e com boa iluminação.",
                                            )

                                            leftEye < 0.35f && rightEye < 0.35f -> {
                                                blinkWasSeen.set(true)
                                                FaceReaderStatus(
                                                    "Piscada detectada",
                                                    "Agora abra os olhos para concluir a confirmação.",
                                                )
                                            }

                                            blinkWasSeen.get() && leftEye > 0.70f && rightEye > 0.70f -> FaceReaderStatus(
                                                "Rosto vivo confirmado",
                                                "Assinatura facial pronta. Pode continuar.",
                                                readyToCheckIn = true,
                                                signature = signature,
                                            )

                                            else -> FaceReaderStatus(
                                                "Confirme sua presença",
                                                "Pisca os olhos uma vez para continuar.",
                                            )
                                        }
                                    }
                                }
                                ContextCompat.getMainExecutor(viewContext).execute {
                                    onReaderStatus(status)
                                }
                            }
                            .addOnFailureListener {
                                ContextCompat.getMainExecutor(viewContext).execute {
                                    onReaderStatus(
                                        FaceReaderStatus(
                                            "Não foi possível ler o rosto",
                                            "Verifique a permissão da câmera e tente novamente.",
                                        )
                                    )
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                    try {
                        val cameraProvider = providerFuture.get()
                        val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            // Alguns emuladores só expõem a webcam como câmera traseira.
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            analysis,
                        )
                    } catch (_: Exception) {
                        onReaderStatus(
                            FaceReaderStatus(
                                "Câmera indisponível",
                                "Este emulador precisa estar ligado a uma webcam para testar o leitor.",
                            )
                        )
                    }
                }, ContextCompat.getMainExecutor(viewContext))
            }
        },
    )
}

/**
 * Constrói 12 medidas proporcionais a partir de seis pontos do rosto.
 * A imagem nunca é persistida: somente esta lista normalizada segue à API.
 */
private fun buildFaceSignature(face: Face): List<Float>? {
    val bounds = face.boundingBox
    val width = bounds.width().toFloat()
    val height = bounds.height().toFloat()
    if (width <= 0f || height <= 0f) return null

    val types = listOf(
        FaceLandmark.LEFT_EYE,
        FaceLandmark.RIGHT_EYE,
        FaceLandmark.NOSE_BASE,
        FaceLandmark.MOUTH_LEFT,
        FaceLandmark.MOUTH_RIGHT,
        FaceLandmark.MOUTH_BOTTOM,
    )
    val result = mutableListOf<Float>()
    for (type in types) {
        val point = face.getLandmark(type)?.position ?: return null
        result += ((point.x - bounds.left) / width * 1_000f).roundToInt() / 1_000f
        result += ((point.y - bounds.top) / height * 1_000f).roundToInt() / 1_000f
    }
    return result
}

@Composable
private fun SimplePurpleHeader(title: String, subtitle: String, onExit: () -> Unit, exitLabel: String = "Sair") {
    Row(
        modifier = Modifier.fillMaxWidth().height(94.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(PrimaryPurple).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onExit) { Text(exitLabel, color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun BrandLogo(large: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("⌜◉⌟", color = Color.White, style = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(8.dp))
        Text("FaceClass", color = Color.White, style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun FaceCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) { Column(Modifier.padding(18.dp)) { content() } }
}

@Composable
private fun MessageCard(message: String?, onDismiss: () -> Unit) {
    if (message == null) return
    val isError = listOf("não foi", "problema", "erro", "incorreto", "expirou", "não tem").any {
        message.contains(it, ignoreCase = true)
    }
    val background = if (isError) Color(0xFFFFE5E5) else LightPurple
    val textColor = if (isError) Color(0xFFB83280) else DarkPurple
    Spacer(Modifier.height(12.dp))
    Surface(color = background, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = textColor, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismiss) { Text("OK", color = textColor) }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = TextMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyCard(text: String) = FaceCard { Text(text, color = TextMuted, style = MaterialTheme.typography.bodySmall) }

@Composable
private fun LoadingCard() = FaceCard {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(Modifier.size(26.dp), color = PrimaryPurple) }
}

private fun firstName(name: String) = name.trim().split(" ").firstOrNull().orEmpty()
private fun initials(name: String) = name.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
private fun displayDate(value: String) = value.replace("T", " ").take(10)
private fun displayTime(value: String) = value.replace("T", " ").drop(11).take(5)
