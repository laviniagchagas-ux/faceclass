from datetime import date, datetime, time

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_student, get_current_teacher
from app.db.database import get_db
from app.db.models import Aluno, Aula, Matricula, Presenca, Professor, SaldoAtraso, Turma
from app.schemas import AttendanceOut, ClassDashboard, LatenessSummary, StudentAttendanceRow
from app.services.attendance import (
    BLOCO_ATRASO_MINUTOS,
    PERCENTUAL_POR_BLOCO,
    TOLERANCIA_INICIAL_MINUTOS,
    now_in_school_timezone,
)

router = APIRouter(prefix="/dashboard", tags=["Painel"])


def current_period() -> date:
    now = now_in_school_timezone()
    return date(now.year, now.month, 1)


@router.get("/classes/{class_id}", response_model=ClassDashboard)
def class_dashboard(
    class_id: int,
    teacher: Professor = Depends(get_current_teacher),
    db: Session = Depends(get_db),
):
    class_group = db.get(Turma, class_id)
    if class_group is None:
        raise HTTPException(status_code=404, detail="Turma não encontrada.")
    if class_group.id_escola != teacher.id_escola:
        raise HTTPException(status_code=403, detail="Turma fora da sua escola.")
    teaches_class = (
        db.query(Aula)
        .filter(Aula.id_turma == class_id, Aula.id_professor == teacher.id_professor)
        .first()
    )
    if teaches_class is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Você não tem aulas cadastradas nesta turma.",
        )

    rows: list[StudentAttendanceRow] = []
    enrollments = (
        db.query(Matricula)
        .join(Aluno)
        .filter(Matricula.id_turma == class_id, Matricula.ativa.is_(True))
        .order_by(Aluno.nome_aluno)
        .all()
    )
    for enrollment in enrollments:
        student = enrollment.aluno
        records = (
            db.query(Presenca)
            .join(Aula)
            .filter(Presenca.id_aluno == student.id_aluno, Aula.id_turma == class_id)
            .all()
        )
        balance = (
            db.query(SaldoAtraso)
            .filter(
                SaldoAtraso.id_aluno == student.id_aluno,
                SaldoAtraso.periodo == current_period(),
            )
            .first()
        )
        latest = max((record.dia_hora for record in records), default=None)
        rows.append(
            StudentAttendanceRow(
                id_aluno=student.id_aluno,
                nome_aluno=student.nome_aluno,
                ra=student.ra,
                presentes=sum(record.status == "PRESENTE" for record in records),
                atrasos=sum(record.status == "ATRASO" for record in records),
                faltas=sum(record.status == "FALTA" for record in records),
                faltas_por_atraso=sum(
                    record.faltas_geradas_por_atraso for record in records
                ),
                percentual_atraso_atual=(
                    balance.percentual_acumulado if balance is not None else 0
                ),
                ultima_presenca=latest,
            )
        )

    return ClassDashboard(
        id_turma=class_group.id_turma,
        turma=class_group.nome_turma,
        ano_letivo=class_group.ano_letivo,
        alunos=rows,
    )


@router.get("/me/attendance", response_model=list[AttendanceOut])
def student_attendance_history(
    student: Aluno = Depends(get_current_student),
    db: Session = Depends(get_db),
):
    return (
        db.query(Presenca)
        .filter(Presenca.id_aluno == student.id_aluno)
        .order_by(Presenca.dia_hora.desc())
        .all()
    )


@router.get("/me/lateness", response_model=LatenessSummary)
def student_lateness_summary(
    student: Aluno = Depends(get_current_student),
    db: Session = Depends(get_db),
):
    """Devolve o saldo mensal mostrado na tela de atrasos do aplicativo."""
    period = current_period()
    first_day = datetime.combine(period, time.min)
    balance = (
        db.query(SaldoAtraso)
        .filter(
            SaldoAtraso.id_aluno == student.id_aluno,
            SaldoAtraso.periodo == period,
        )
        .first()
    )
    late_records = (
        db.query(Presenca)
        .filter(
            Presenca.id_aluno == student.id_aluno,
            Presenca.status == "ATRASO",
            Presenca.dia_hora >= first_day,
        )
        .all()
    )
    return LatenessSummary(
        periodo=period,
        percentual_acumulado=balance.percentual_acumulado if balance else 0,
        faltas_convertidas=balance.faltas_convertidas if balance else 0,
        atrasos_no_mes=len(late_records),
        minutos_de_atraso_no_mes=sum(record.minutos_atraso for record in late_records),
        tolerancia_inicial_minutos=TOLERANCIA_INICIAL_MINUTOS,
        bloco_atraso_minutos=BLOCO_ATRASO_MINUTOS,
        percentual_por_bloco=PERCENTUAL_POR_BLOCO,
    )
