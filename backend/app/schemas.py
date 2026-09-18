import math
from datetime import date, datetime
from typing import Literal, Optional

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator, model_validator


class ApiModel(BaseModel):
    model_config = ConfigDict(from_attributes=True)


class Message(ApiModel):
    message: str


class Token(ApiModel):
    access_token: str
    token_type: str = "bearer"
    user_id: int
    role: str


class LoginInput(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class BootstrapAdminInput(LoginInput):
    nome: str = Field(min_length=3, max_length=100)


class CurrentUserOut(ApiModel):
    id_usuario: int
    nome: str
    email: EmailStr
    perfil: str


class SchoolCreate(BaseModel):
    nome_escola: str = Field(min_length=3, max_length=100)
    wifi_ssid: Optional[str] = Field(default=None, max_length=100)
    latitude: Optional[float] = Field(default=None, ge=-90, le=90)
    longitude: Optional[float] = Field(default=None, ge=-180, le=180)
    raio_permitido_metros: int = Field(default=150, ge=20, le=5000)

    @model_validator(mode="after")
    def location_is_complete(self):
        if (self.latitude is None) != (self.longitude is None):
            raise ValueError("Latitude e longitude devem ser informadas juntas.")
        return self


class SchoolOut(ApiModel):
    id_escola: int
    nome_escola: str
    wifi_ssid: Optional[str]
    latitude: Optional[float]
    longitude: Optional[float]
    raio_permitido_metros: int


class StudentCreate(BaseModel):
    id_escola: int
    nome_aluno: str = Field(min_length=3, max_length=100)
    ra: str = Field(min_length=1, max_length=14)
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    hash_face: Optional[str] = Field(
        default=None,
        min_length=64,
        max_length=64,
        pattern=r"^[A-Fa-f0-9]{64}$",
    )


class FaceTemplateInput(BaseModel):
    """Hash SHA-256 de template facial; não aceita foto, vídeo ou Base64."""

    hash_face: str = Field(
        min_length=64,
        max_length=64,
        pattern=r"^[A-Fa-f0-9]{64}$",
    )


class FaceSignatureInput(BaseModel):
    """Medidas faciais normalizadas extraídas no celular; nunca recebe imagens."""

    assinatura: list[float] = Field(min_length=12, max_length=12)

    @field_validator("assinatura")
    @classmethod
    def signature_must_be_valid(cls, values: list[float]) -> list[float]:
        if any(not math.isfinite(value) or value < -0.2 or value > 1.2 for value in values):
            raise ValueError("Assinatura facial inválida.")
        return values


class FaceStatus(ApiModel):
    cadastrada: bool
    face_cadastrada_em: Optional[datetime]


class StudentOut(ApiModel):
    id_aluno: int
    id_escola: int
    id_usuario: Optional[int]
    nome_aluno: str
    ra: Optional[str]


class GuardianCreate(BaseModel):
    id_aluno: int
    nome_pais: str = Field(min_length=3, max_length=100)
    email: EmailStr


class GuardianOut(ApiModel):
    id_pais: int
    id_aluno: int
    nome_pais: str
    email: EmailStr


class TeacherCreate(BaseModel):
    id_escola: int
    nome_professor: str = Field(min_length=3, max_length=100)
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class TeacherOut(ApiModel):
    id_professor: int
    id_escola: int
    id_usuario: int
    nome_professor: str


class ClassCreate(BaseModel):
    id_escola: int
    nome_turma: str = Field(min_length=2, max_length=80)
    ano_letivo: int = Field(ge=2020, le=2100)


class ClassOut(ApiModel):
    id_turma: int
    id_escola: int
    nome_turma: str
    ano_letivo: int


class EnrollmentCreate(BaseModel):
    id_aluno: int
    id_turma: int


class EnrollmentOut(ApiModel):
    id_matricula: int
    id_aluno: int
    id_turma: int
    ativa: bool


class LessonCreate(BaseModel):
    """Comando para o professor abrir uma chamada.

    O horário é definido pelo servidor da escola, não pelo relógio do celular.
    Isso impede que uma aula criada num emulador com horário diferente nasça
    fora do período de disponibilidade para o aluno.
    """

    id_turma: int
    disciplina: str = Field(min_length=2, max_length=100)


class LessonOut(ApiModel):
    id_aula: int
    id_turma: int
    id_professor: int
    disciplina: str
    inicio_previsto: datetime
    fim_previsto: datetime
    ativa: bool


class CheckInInput(BaseModel):
    id_aula: int
    face_verification_token: str = Field(min_length=8, max_length=500)
    wifi_ssid: Optional[str] = Field(default=None, max_length=100)
    latitude: Optional[float] = Field(default=None, ge=-90, le=90)
    longitude: Optional[float] = Field(default=None, ge=-180, le=180)

    @model_validator(mode="after")
    def location_is_complete(self):
        if (self.latitude is None) != (self.longitude is None):
            raise ValueError("Latitude e longitude devem ser informadas juntas.")
        return self


class FaceCheckInInput(FaceSignatureInput):
    id_aula: int
    wifi_ssid: Optional[str] = Field(default=None, max_length=100)
    latitude: Optional[float] = Field(default=None, ge=-90, le=90)
    longitude: Optional[float] = Field(default=None, ge=-180, le=180)

    @model_validator(mode="after")
    def location_is_complete(self):
        if (self.latitude is None) != (self.longitude is None):
            raise ValueError("Latitude e longitude devem ser informadas juntas.")
        return self


class AttendanceCorrection(BaseModel):
    status: Literal["PRESENTE", "ATRASO", "FALTA"]
    observacao: str = Field(min_length=3, max_length=500)


class AttendanceOut(ApiModel):
    id_presenca: int
    id_aluno: int
    id_aula: Optional[int]
    dia_hora: datetime
    tipo_presenca: bool
    status: str
    minutos_atraso: int
    percentual_atraso_adicionado: int
    faltas_geradas_por_atraso: int
    origem: str
    observacao: Optional[str]


class LatenessSummary(ApiModel):
    """Resumo mensal do saldo de atrasos do aluno."""

    periodo: date
    percentual_acumulado: int
    faltas_convertidas: int
    atrasos_no_mes: int
    minutos_de_atraso_no_mes: int
    tolerancia_inicial_minutos: int
    bloco_atraso_minutos: int
    percentual_por_bloco: int


class CheckInOut(ApiModel):
    message: str
    attendance: AttendanceOut


class ManualAttendanceInput(BaseModel):
    id_aula: int
    id_aluno: int
    status: Literal["PRESENTE", "ATRASO", "FALTA"]
    observacao: str = Field(min_length=3, max_length=500)


class StudentAttendanceRow(ApiModel):
    id_aluno: int
    nome_aluno: str
    ra: Optional[str]
    presentes: int
    atrasos: int
    faltas: int
    faltas_por_atraso: int
    percentual_atraso_atual: int
    ultima_presenca: Optional[datetime]


class ClassDashboard(ApiModel):
    id_turma: int
    turma: str
    ano_letivo: int
    alunos: list[StudentAttendanceRow]
