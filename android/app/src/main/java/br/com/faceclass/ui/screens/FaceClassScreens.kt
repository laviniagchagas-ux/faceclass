package br.com.faceclass.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.faceclass.data.Attendance
import br.com.faceclass.data.Lesson
import kotlin.math.roundToInt

private val FaceClassPrimary = Color(0xFF6B46C1)
private val FaceClassPrimaryDark = Color(0xFF553C9A)
private val FaceClassPrimaryLight = Color(0xFFF3E8FF)
private val FaceClassBackground = Color(0xFFF8F9FE)
private val FaceClassText = Color(0xFF2D3748)
private val FaceClassMuted = Color(0xFF718096)
private val FaceClassGreen = Color(0xFF38A169)
private val FaceClassGreenBg = Color(0xFFC6F6D5)
private val FaceClassRed = Color(0xFFE53E3E)
private val FaceClassRedBg = Color(0xFFFED7D7)
private val FaceClassTabBg = Color(0xFFEAF0F7)

private data class FaceClassNotice(val type: String, val title: String, val description: String)

/* The HTML keeps announcements only in the browser session. This mirrors that
 * behavior without pretending that the API already has an announcements route. */
private object FaceClassFrontMemory {
    val notices = mutableStateListOf<FaceClassNotice>()
    val absences = mutableStateMapOf<String, Boolean>()
}

/** Login layout copied from frontend/front.html. Authentication remains the real API call. */
@Composable
fun FaceClassLoginScreen(
    loading: Boolean,
    message: String?,
    onLogin: (String, String) -> Unit,
    onClearMessage: () -> Unit,
) {
    var role by rememberSaveable { mutableStateOf("Aluno") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(FaceClassBackground).verticalScroll(rememberScrollState()),
    ) {
        Box(
            Modifier.fillMaxWidth().height(184.dp)
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(FaceClassPrimary)
                .padding(horizontal = 28.dp, vertical = 44.dp),
        ) {
            FaceClassBrand(large = true)
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp).offset(y = (-48).dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 6.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Bem-vindo!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = FaceClassText)
                Text("Acesse sua conta FaceClass", style = MaterialTheme.typography.bodySmall, color = FaceClassMuted)
                Spacer(Modifier.height(20.dp))

                RoleTabs(role = role, onRoleChange = { role = it })
                Spacer(Modifier.height(22.dp))

                val fieldLabel = when (role) {
                    "Professor" -> "E-mail institucional"
                    "Responsável" -> "E-mail do responsável"
                    else -> "E-mail do aluno"
                }
                Text(fieldLabel, fontWeight = FontWeight.Bold, color = FaceClassText, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("seuemail@escola.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text("Senha", fontWeight = FontWeight.Bold, color = FaceClassText, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Digite sua senha") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                )
                if (message != null) {
                    Spacer(Modifier.height(14.dp))
                    Surface(color = FaceClassRedBg, shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(message, color = FaceClassRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = onClearMessage) { Text("Fechar", color = FaceClassRed) }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = { onLogin(email, password) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FaceClassPrimary),
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Entrar", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun RoleTabs(role: String, onRoleChange: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = FaceClassTabBg) {
        Row(Modifier.padding(4.dp)) {
            listOf("Aluno", "Professor", "Responsável").forEach { option ->
                val selected = role == option
                Surface(
                    modifier = Modifier.weight(1f).height(39.dp).clickable { onRoleChange(option) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) Color.White else Color.Transparent,
                    shadowElevation = if (selected) 2.dp else 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(option, color = if (selected) FaceClassPrimary else FaceClassMuted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun FaceClassStudentHome(
    name: String,
    lessons: List<Lesson>,
    attendance: List<Attendance>,
    faceRegistered: Boolean,
    loading: Boolean,
    message: String?,
    onRefresh: () -> Unit,
    onOpenFace: (Lesson) -> Unit,
    onEnrollFace: () -> Unit,
    onLogout: () -> Unit,
    onClearMessage: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf("inicio") }
    val selectedLesson = lessons.firstOrNull()

    Column(Modifier.fillMaxSize().background(FaceClassBackground)) {
        FaceClassTopHeader(
            trailing = if (loading) "..." else "↻",
            onTrailingClick = onRefresh,
        )
        Box(Modifier.weight(1f)) {
            when (page) {
                "avisos" -> FaceClassNoticesPage()
                "aulas" -> FaceClassClassesPage()
                "relatorios" -> FaceClassReportsPage(attendance)
                else -> FaceClassStudentDashboard(
                    name = name,
                    attendance = attendance,
                    faceRegistered = faceRegistered,
                    selectedLesson = selectedLesson,
                    message = message,
                    onClearMessage = onClearMessage,
                    onOpenFace = onOpenFace,
                    onEnrollFace = onEnrollFace,
                )
            }
        }
        FaceClassBottomNav(page = page, onPageChange = { page = it }, onLogout = onLogout)
    }
}

@Composable
private fun FaceClassStudentDashboard(
    name: String,
    attendance: List<Attendance>,
    faceRegistered: Boolean,
    selectedLesson: Lesson?,
    message: String?,
    onClearMessage: () -> Unit,
    onOpenFace: (Lesson) -> Unit,
    onEnrollFace: () -> Unit,
) {
    val present = attendance.count { it.tipo_presenca && it.status.uppercase() != "FALTA" }
    val total = attendance.size
    val percentage = if (total == 0) 0 else (present * 100f / total).roundToInt()
    val avatar = initials(name)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FaceClassUserGreeting(name = name, subtitle = "Acompanhe sua presença", initials = avatar)

        Surface(
            modifier = Modifier.fillMaxWidth().clickable {
                if (faceRegistered && selectedLesson != null) onOpenFace(selectedLesson) else onEnrollFace()
            },
            shape = RoundedCornerShape(20.dp),
            color = if (selectedLesson == null) FaceClassMuted else Color.Transparent,
            shadowElevation = if (selectedLesson == null) 0.dp else 5.dp,
        ) {
            Box(
                Modifier.background(
                    if (selectedLesson == null) Brush.linearGradient(listOf(Color(0xFFA0AEC0), FaceClassMuted))
                    else Brush.linearGradient(listOf(FaceClassPrimary, FaceClassPrimaryDark)),
                ).padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(46.dp), shape = RoundedCornerShape(14.dp), color = Color.Transparent, border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = .8f))) {
                        Box(contentAlignment = Alignment.Center) { Text("⌁", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (selectedLesson == null) "Nenhuma aula disponível" else if (faceRegistered) "Registrar presença" else "Cadastrar rosto",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            if (selectedLesson == null) "Aguardando uma aula ativa" else if (faceRegistered) selectedLesson.disciplina else "Faça seu cadastro facial primeiro",
                            color = Color.White.copy(alpha = .86f), style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text("›", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        FaceClassFrequencyCard(percentage = percentage, present = present, absent = (total - present).coerceAtLeast(0))
        if (message != null) FaceClassMessage(message, onClearMessage)
        FaceClassLastRecord(attendance.firstOrNull())
    }
}

@Composable
fun FaceClassTeacherHome(name: String, onLogout: () -> Unit) {
    var page by rememberSaveable { mutableStateOf("grade") }
    var day by rememberSaveable { mutableStateOf("Seg") }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(FaceClassBackground)) {
        FaceClassTopHeader(trailing = "Sair", onTrailingClick = onLogout)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FaceClassUserGreeting(name = name, subtitle = "Área do professor", initials = initials(name))
            FaceClassSegmentedTabs(
                first = "Grade de Aulas", second = "Quadro de Avisos", selectedFirst = page == "grade",
                onFirst = { page = "grade" }, onSecond = { page = "avisos" },
            )
            if (page == "grade") {
                TeacherSchedule(day = day, onDayChange = { day = it })
            } else {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 3.dp) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Novo aviso", fontWeight = FontWeight.ExtraBold, color = FaceClassText)
                        OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Título do aviso") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                        OutlinedTextField(description, { description = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Escreva o recado") }, shape = RoundedCornerShape(12.dp), minLines = 3)
                        Button(
                            onClick = {
                                if (title.isNotBlank() && description.isNotBlank()) {
                                    FaceClassFrontMemory.notices.add(FaceClassNotice("RECADO", title.trim(), description.trim()))
                                    title = ""
                                    description = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = FaceClassPrimary),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("Publicar aviso") }
                    }
                }
                if (FaceClassFrontMemory.notices.isEmpty()) EmptyNoticeCard("Os avisos publicados aparecerão aqui.")
                else FaceClassNoticeList(FaceClassFrontMemory.notices)
            }
        }
    }
}

@Composable
fun FaceClassParentHome(name: String, attendance: List<Attendance>, onLogout: () -> Unit) {
    val present = attendance.count { it.tipo_presenca && it.status.uppercase() != "FALTA" }
    val total = attendance.size
    val frequency = if (total == 0) 0 else (present * 100f / total).roundToInt()
    Column(Modifier.fillMaxSize().background(FaceClassBackground)) {
        FaceClassTopHeader(trailing = "Sair", onTrailingClick = onLogout)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FaceClassUserGreeting(name = name, subtitle = "Acompanhe a frequência do aluno", initials = initials(name))
            FaceClassFrequencyCard(frequency, present, (total - present).coerceAtLeast(0))
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 3.dp) {
                Column(Modifier.padding(18.dp)) {
                    Text("Status atual", fontWeight = FontWeight.ExtraBold, color = FaceClassText)
                    Spacer(Modifier.height(10.dp))
                    Surface(color = FaceClassGreenBg, shape = RoundedCornerShape(20.dp)) {
                        Text("● Frequência acompanhada", color = FaceClassGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Os registros dependem das presenças já enviadas pela API.", color = FaceClassMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Quadro de avisos", fontWeight = FontWeight.ExtraBold, color = FaceClassText)
            if (FaceClassFrontMemory.notices.isEmpty()) EmptyNoticeCard("Nenhum aviso publicado até agora.")
            else FaceClassNoticeList(FaceClassFrontMemory.notices)
        }
    }
}

@Composable
private fun TeacherSchedule(day: String, onDayChange: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 3.dp) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Minha Grade de Aulas", fontWeight = FontWeight.ExtraBold, color = FaceClassText, style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Seg", "Ter", "Qua", "Qui", "Sex").forEach { item ->
                    val selected = day == item
                    Surface(
                        modifier = Modifier.weight(1f).height(40.dp).clickable { onDayChange(item) },
                        color = if (selected) Color.White else FaceClassTabBg,
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = if (selected) 2.dp else 0.dp,
                    ) { Box(contentAlignment = Alignment.Center) { Text(item, color = if (selected) FaceClassPrimary else FaceClassMuted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall) } }
                }
            }
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF5F7FA)) {
                Text("Intervalo: 10:20 às 10:40", modifier = Modifier.padding(12.dp), color = FaceClassMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text(dayName(day), fontWeight = FontWeight.ExtraBold, color = FaceClassText)
            scheduleFor(day).forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.first, color = FaceClassText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(item.second, color = FaceClassMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    val key = "$day-${item.first}"
                    Surface(
                        modifier = Modifier.clickable { FaceClassFrontMemory.absences[key] = !(FaceClassFrontMemory.absences[key] ?: false) },
                        color = if (FaceClassFrontMemory.absences[key] == true) FaceClassGreenBg else FaceClassRedBg,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(if (FaceClassFrontMemory.absences[key] == true) "Avisado" else "Avisar Falta", color = if (FaceClassFrontMemory.absences[key] == true) FaceClassGreen else FaceClassRed, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun FaceClassNoticesPage() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Quadro de Avisos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = FaceClassText)
        Text("Fique por dentro dos recados da escola.", color = FaceClassMuted, style = MaterialTheme.typography.bodySmall)
        if (FaceClassFrontMemory.notices.isEmpty()) EmptyNoticeCard("Nenhum aviso no momento.") else FaceClassNoticeList(FaceClassFrontMemory.notices)
    }
}

@Composable
private fun FaceClassClassesPage() {
    var day by rememberSaveable { mutableStateOf("Seg") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Grade de Aulas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = FaceClassText)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Seg", "Ter", "Qua", "Qui", "Sex").forEach { item ->
                val selected = item == day
                Surface(modifier = Modifier.weight(1f).height(38.dp).clickable { day = item }, color = if (selected) FaceClassPrimary else FaceClassTabBg, shape = RoundedCornerShape(10.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(item, color = if (selected) Color.White else FaceClassMuted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
        Text(dayName(day), color = FaceClassText, fontWeight = FontWeight.ExtraBold)
        scheduleFor(day).forEach { item ->
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 2.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text(item.first, fontWeight = FontWeight.ExtraBold, color = FaceClassText)
                    Text(item.second, color = FaceClassMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun FaceClassReportsPage(attendance: List<Attendance>) {
    val present = attendance.count { it.tipo_presenca && it.status.uppercase() != "FALTA" }
    val absences = (attendance.size - present).coerceAtLeast(0)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Relatórios", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = FaceClassText)
        FaceClassFrequencyCard(if (attendance.isEmpty()) 0 else (present * 100f / attendance.size).roundToInt(), present, absences)
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 3.dp) {
            Column(Modifier.padding(18.dp)) {
                Text("Histórico de presenças", fontWeight = FontWeight.ExtraBold, color = FaceClassText)
                Spacer(Modifier.height(12.dp))
                if (attendance.isEmpty()) Text("Ainda não há registros de presença.", color = FaceClassMuted, style = MaterialTheme.typography.bodySmall)
                attendance.take(12).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.dia_hora, fontWeight = FontWeight.Bold, color = FaceClassText, style = MaterialTheme.typography.bodySmall)
                            Text(row.origem, color = FaceClassMuted, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(if (row.tipo_presenca) "Presente" else "Falta", color = if (row.tipo_presenca) FaceClassGreen else FaceClassRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun FaceClassFrequencyCard(percentage: Int, present: Int, absent: Int) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            FrequencyRing(percentage)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Frequência", fontWeight = FontWeight.ExtraBold, color = FaceClassText)
                Spacer(Modifier.height(8.dp))
                Text("● $present presença(s)", color = FaceClassGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text("● $absent falta(s)", color = FaceClassRed, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FrequencyRing(percentage: Int) {
    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(FaceClassTabBg, -90f, 360f, false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
            drawArc(FaceClassPrimary, -90f, 360f * percentage.coerceIn(0, 100) / 100f, false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$percentage%", fontWeight = FontWeight.ExtraBold, color = FaceClassText, style = MaterialTheme.typography.titleMedium)
            Text("presença", color = FaceClassMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FaceClassLastRecord(attendance: Attendance?) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 3.dp) {
        Column(Modifier.padding(18.dp)) {
            Text("Último registro", fontWeight = FontWeight.ExtraBold, color = FaceClassText)
            Spacer(Modifier.height(8.dp))
            if (attendance == null) {
                Text("Nenhuma presença registrada ainda.", color = FaceClassMuted, style = MaterialTheme.typography.bodySmall)
            } else {
                Text(if (attendance.tipo_presenca) "✓ Presença confirmada" else "• Falta registrada", color = if (attendance.tipo_presenca) FaceClassGreen else FaceClassRed, fontWeight = FontWeight.Bold)
                Text(attendance.dia_hora, color = FaceClassMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FaceClassMessage(message: String, onClear: () -> Unit) {
    Surface(color = FaceClassPrimaryLight, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), color = FaceClassPrimaryDark, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onClear) { Text("Fechar", color = FaceClassPrimaryDark) }
        }
    }
}

@Composable
private fun FaceClassNoticeList(notices: List<FaceClassNotice>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        notices.asReversed().forEach { notice ->
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D8FD))) {
                Column(Modifier.padding(14.dp)) {
                    val (tagColor, tagTextColor) = when (notice.type) {
                        "EVENTO" -> FaceClassGreenBg to Color(0xFF22543D)
                        "PROVA" -> FaceClassRedBg to Color(0xFF9B2C2C)
                        else -> Color(0xFFEBF8FF) to Color(0xFF2B6CB0)
                    }
                    Surface(color = tagColor, shape = RoundedCornerShape(6.dp)) { Text(notice.type, color = tagTextColor, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                    Spacer(Modifier.height(8.dp))
                    Text(notice.title, color = FaceClassText, fontWeight = FontWeight.ExtraBold)
                    Text(notice.description, color = FaceClassMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EmptyNoticeCard(text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 2.dp) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(18.dp), color = FaceClassMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FaceClassSegmentedTabs(first: String, second: String, selectedFirst: Boolean, onFirst: () -> Unit, onSecond: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = FaceClassTabBg) {
        Row(Modifier.padding(4.dp)) {
            SegmentedTab(first, selectedFirst, onFirst)
            SegmentedTab(second, !selectedFirst, onSecond)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SegmentedTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(modifier = Modifier.weight(1f).height(40.dp).clickable(onClick = onClick), shape = RoundedCornerShape(10.dp), color = if (selected) Color.White else Color.Transparent, shadowElevation = if (selected) 2.dp else 0.dp) {
        Box(contentAlignment = Alignment.Center) { Text(label, color = if (selected) FaceClassPrimary else FaceClassMuted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun FaceClassUserGreeting(name: String, subtitle: String, initials: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Olá, ${firstName(name)}!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = FaceClassText)
            Text(subtitle, color = FaceClassMuted, style = MaterialTheme.typography.bodySmall)
        }
        Surface(Modifier.size(48.dp), shape = CircleShape, color = FaceClassPrimaryLight) { Box(contentAlignment = Alignment.Center) { Text(initials, color = FaceClassPrimary, fontWeight = FontWeight.ExtraBold) } }
    }
}

@Composable
private fun FaceClassTopHeader(trailing: String, onTrailingClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).background(FaceClassPrimary).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FaceClassBrand()
        Spacer(Modifier.weight(1f))
        Text(modifier = Modifier.clickable(onClick = onTrailingClick).padding(8.dp), text = trailing, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FaceClassBrand(large: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(if (large) 31.dp else 26.dp)) {
            val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(Color.White, style = stroke)
            drawCircle(Color.White, radius = size.minDimension * .13f)
        }
        Spacer(Modifier.width(8.dp))
        Text("FaceClass", color = Color.White, fontWeight = FontWeight.ExtraBold, style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun FaceClassBottomNav(page: String, onPageChange: (String) -> Unit, onLogout: () -> Unit) {
    Surface(shadowElevation = 10.dp, color = Color.White) {
        Row(Modifier.fillMaxWidth().height(66.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("inicio" to "⌂" to "Início", "avisos" to "◌" to "Avisos", "aulas" to "▤" to "Aulas", "relatorios" to "▥" to "Relatórios").forEach { triple ->
                val key = triple.first.first
                val icon = triple.first.second
                val label = triple.second
                val selected = page == key
                Column(Modifier.weight(1f).clickable { onPageChange(key) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(icon, color = if (selected) FaceClassPrimary else FaceClassMuted)
                    Text(label, color = if (selected) FaceClassPrimary else FaceClassMuted, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
            Column(Modifier.weight(1f).clickable(onClick = onLogout), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("↪", color = FaceClassMuted)
                Text("Sair", color = FaceClassMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun firstName(name: String): String = name.trim().substringBefore(' ').ifBlank { "Usuário" }

private fun initials(name: String): String = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "FC" }

private fun dayName(day: String): String = when (day) {
    "Seg" -> "Segunda-feira"
    "Ter" -> "Terça-feira"
    "Qua" -> "Quarta-feira"
    "Qui" -> "Quinta-feira"
    else -> "Sexta-feira"
}

private fun scheduleFor(day: String): List<Pair<String, String>> = when (day) {
    "Seg" -> listOf("Análise e Desenvolvimento de Sistemas" to "07:00 · Sala 12 (1ª aula)", "Projeto Integrador" to "11:30 · Sala 14 (6ª aula)")
    "Ter" -> listOf("Banco de Dados" to "07:00 · Laboratório 03", "Desenvolvimento Web" to "09:40 · Laboratório 03")
    "Qua" -> listOf("Linguagem de Programação" to "08:00 · Sala 12", "Projeto Integrador" to "11:30 · Sala 14")
    "Qui" -> listOf("Análise e Desenvolvimento de Sistemas" to "09:40 · Sala 12", "Banco de Dados" to "13:00 · Laboratório 03")
    else -> listOf("Desenvolvimento Web" to "07:00 · Laboratório 03", "Linguagem de Programação" to "09:40 · Sala 12")
}
