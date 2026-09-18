package br.com.faceclass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.faceclass.data.Attendance
import br.com.faceclass.data.ClassDashboard
import br.com.faceclass.data.CreateLessonRequest
import br.com.faceclass.data.CurrentUser
import br.com.faceclass.data.FaceClassRepository
import br.com.faceclass.data.Lesson
import br.com.faceclass.data.LatenessSummary
import br.com.faceclass.data.SessionStore
import br.com.faceclass.data.friendlyApiError
import kotlinx.coroutines.launch

data class FaceClassUiState(
    val user: CurrentUser? = null,
    val lessons: List<Lesson> = emptyList(),
    val attendance: List<Attendance> = emptyList(),
    val lateness: LatenessSummary? = null,
    val faceRegistered: Boolean = false,
    val dashboard: ClassDashboard? = null,
    val activeTeacherLesson: Lesson? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
)

class FaceClassViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FaceClassRepository(SessionStore(application.applicationContext))
    var state = androidx.compose.runtime.mutableStateOf(FaceClassUiState())
        private set

    init {
        if (repository.hasSession()) restoreSession()
    }

    fun login(email: String, password: String) = runLoading {
        val user = repository.login(email.trim(), password)
        state.value = state.value.copy(user = user, message = "Bem-vindo, ${user.nome}!")
        if (user.perfil == "ALUNO") refreshStudentData()
    }

    fun restoreSession() = runLoading {
        val user = repository.currentUser()
        state.value = state.value.copy(user = user)
        if (user.perfil == "ALUNO") refreshStudentData()
    }

    fun refreshStudentData() = runLoading {
        val lessons = repository.availableLessons()
        val attendance = repository.attendanceHistory()
        val lateness = repository.latenessSummary()
        val faceStatus = repository.faceStatus()
        state.value = state.value.copy(
            lessons = lessons,
            attendance = attendance,
            lateness = lateness,
            faceRegistered = faceStatus.cadastrada,
        )
    }

    fun enrollFace(signature: List<Float>) = runLoading {
        repository.enrollFace(signature)
        state.value = state.value.copy(
            faceRegistered = true,
            message = "Rosto cadastrado. Agora a chamada vai comparar sua assinatura facial.",
        )
    }

    fun faceCheckIn(lessonId: Int, signature: List<Float>) = runLoading {
        val response = repository.faceCheckIn(lessonId, signature)
        state.value = state.value.copy(message = response.message)
        refreshStudentData()
    }

    fun createLesson(classIdText: String, subject: String) {
        val classId = classIdText.toIntOrNull()
        if (classId == null || subject.trim().length < 2) {
            state.value = state.value.copy(message = "Informe uma turma válida e uma disciplina.")
            return
        }
        runLoading {
            val lesson = repository.createLesson(
                CreateLessonRequest(
                    id_turma = classId,
                    disciplina = subject.trim(),
                )
            )
            state.value = state.value.copy(
                activeTeacherLesson = lesson,
                message = "Aula de ${lesson.disciplina} criada e válida por uma hora."
            )
            loadDashboard(classId.toString())
        }
    }

    fun loadDashboard(classIdText: String) {
        val classId = classIdText.toIntOrNull()
        if (classId == null) {
            state.value = state.value.copy(message = "Informe o ID numérico da turma.")
            return
        }
        runLoading {
            state.value = state.value.copy(dashboard = repository.classDashboard(classId))
        }
    }

    fun logout() {
        repository.logout()
        state.value = FaceClassUiState(message = "Sessão encerrada.")
    }

    fun clearMessage() {
        state.value = state.value.copy(message = null)
    }

    private fun runLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, message = null)
            try {
                block()
            } catch (exception: Exception) {
                state.value = state.value.copy(
                    message = friendlyApiError(exception)
                )
            } finally {
                state.value = state.value.copy(isLoading = false)
            }
        }
    }
}
