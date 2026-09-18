from __future__ import annotations

import math
from dataclasses import dataclass
from datetime import date, datetime, timedelta
from zoneinfo import ZoneInfo

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.core.config import settings
from app.db.models import Aluno, Aula, Matricula, Presenca, SaldoAtraso
from app.schemas import CheckInInput

TOLERANCIA_INICIAL_MINUTOS = 10
BLOCO_ATRASO_MINUTOS = 5
PERCENTUAL_POR_BLOCO = 10


@dataclass
class CheckInResult:
    attendance: Presenca
    message: str


def now_in_school_timezone() -> datetime:
    """Retorna um datetime sem fuso, compatível com DATETIME do MySQL."""
    return datetime.now(ZoneInfo(settings.timezone)).replace(tzinfo=None)


def distance_in_meters(
    first_latitude: float,
    first_longitude: float,
    second_latitude: float,
    second_longitude: float,
) -> float:
    """Calcula a distância entre dois pontos usando a fórmula de Haversine."""
    earth_radius_meters = 6_371_000
    latitude_delta = math.radians(second_latitude - first_latitude)
    longitude_delta = math.radians(second_longitude - first_longitude)
    a = (
        math.sin(latitude_delta / 2) ** 2
        + math.cos(math.radians(first_latitude))
        * math.cos(math.radians(second_latitude))
        * math.sin(longitude_delta / 2) ** 2
    )
    return 2 * earth_radius_meters * math.asin(math.sqrt(a))


def validate_check_in_context(
    aula: Aula, payload: CheckInInput, registered_at: datetime, facial_verified: bool = False
) -> None:
    """Confere horário, biometria, Wi-Fi e localização."""
    school = aula.turma.escola

    if registered_at < aula.inicio_previsto or registered_at > aula.fim_previsto:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="A chamada só pode ser feita durante o horário da aula.",
        )

    if facial_verified:
        pass
    elif settings.demo_mode:
        if payload.face_verification_token != settings.demo_face_token:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="Validação facial de demonstração não confirmada.",
            )
    else:
        raise HTTPException(
            status_code=status.HTTP_501_NOT_IMPLEMENTED,
            detail=(
                "Integre um provedor de reconhecimento facial que gere um token "
                "assinado antes de desativar o DEMO_MODE."
            ),
        )

    if school.wifi_ssid and payload.wifi_ssid != school.wifi_ssid:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="O aparelho não está conectado ao Wi-Fi institucional esperado.",
        )

    if school.latitude is not None and school.longitude is not None:
        if payload.latitude is None or payload.longitude is None:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail="A localização é obrigatória para esta escola.",
            )
        distance = distance_in_meters(
            school.latitude, school.longitude, payload.latitude, payload.longitude
        )
        if distance > school.raio_permitido_metros:
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail=(
                    "Você está fora do raio permitido da escola "
                    f"({distance:.0f} m de distância)."
                ),
            )


def calculate_lateness(
    scheduled_start: datetime, registered_at: datetime
) -> tuple[int, int]:
    """Retorna minutos após a tolerância e o percentual que será somado.

    Até 10 minutos após o início a presença é normal. A partir do primeiro
    minuto seguinte, cada bloco iniciado de 5 minutos soma 10%.
    """
    late_after_tolerance = registered_at - (
        scheduled_start + timedelta(minutes=TOLERANCIA_INICIAL_MINUTOS)
    )
    late_minutes = max(0, math.ceil(late_after_tolerance.total_seconds() / 60))
    if late_minutes == 0:
        return 0, 0

    blocks = math.ceil(late_minutes / BLOCO_ATRASO_MINUTOS)
    return late_minutes, blocks * PERCENTUAL_POR_BLOCO


def update_late_balance(
    db: Session, student_id: int, registered_at: datetime, percentage_to_add: int
) -> tuple[int, int]:
    """Atualiza o saldo mensal e devolve (saldo final, faltas convertidas agora)."""
    period = date(registered_at.year, registered_at.month, 1)
    balance = (
        db.query(SaldoAtraso)
        .filter(
            SaldoAtraso.id_aluno == student_id,
            SaldoAtraso.periodo == period,
        )
        .with_for_update()
        .first()
    )
    if balance is None:
        balance = SaldoAtraso(id_aluno=student_id, periodo=period)
        db.add(balance)
        db.flush()

    total = balance.percentual_acumulado + percentage_to_add
    converted_absences = total // 100
    balance.percentual_acumulado = total % 100
    balance.faltas_convertidas += converted_absences
    return balance.percentual_acumulado, converted_absences


def register_check_in(
    db: Session, student: Aluno, payload: CheckInInput, facial_verified: bool = False
) -> CheckInResult:
    aula = db.get(Aula, payload.id_aula)
    if aula is None or not aula.ativa:
        raise HTTPException(status_code=404, detail="Aula não encontrada ou inativa.")

    enrollment = (
        db.query(Matricula)
        .filter(
            Matricula.id_aluno == student.id_aluno,
            Matricula.id_turma == aula.id_turma,
            Matricula.ativa.is_(True),
        )
        .first()
    )
    if enrollment is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Você não está matriculado na turma desta aula.",
        )

    already_registered = (
        db.query(Presenca)
        .filter(
            Presenca.id_aula == aula.id_aula,
            Presenca.id_aluno == student.id_aluno,
        )
        .first()
    )
    if already_registered:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="A presença desta aula já foi registrada.",
        )

    registered_at = now_in_school_timezone()
    validate_check_in_context(aula, payload, registered_at, facial_verified=facial_verified)
    late_minutes, percentage_to_add = calculate_lateness(
        aula.inicio_previsto, registered_at
    )
    final_balance, converted_absences = update_late_balance(
        db, student.id_aluno, registered_at, percentage_to_add
    )

    status_value = "ATRASO" if late_minutes else "PRESENTE"
    attendance = Presenca(
        dia_hora=registered_at,
        id_aluno=student.id_aluno,
        id_aula=aula.id_aula,
        tipo_presenca=True,
        status=status_value,
        minutos_atraso=late_minutes,
        percentual_atraso_adicionado=percentage_to_add,
        faltas_geradas_por_atraso=converted_absences,
        origem="AUTOMATICO",
    )
    db.add(attendance)
    db.flush()

    if late_minutes == 0:
        message = "Presença confirmada dentro do horário."
    else:
        message = (
            f"Atraso de {late_minutes} minuto(s) registrado: +{percentage_to_add}% "
            f"no saldo. Saldo atual: {final_balance}%."
        )
        if converted_absences:
            message += f" {converted_absences} falta(s) foi(ram) gerada(s) por atraso."

    return CheckInResult(attendance=attendance, message=message)
