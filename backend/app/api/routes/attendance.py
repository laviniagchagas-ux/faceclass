from datetime import timedelta

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_student, get_current_teacher
from app.db.database import get_db
from app.db.models import Aluno, Aula, Matricula, Presenca, Professor, Turma
from app.schemas import (
    AttendanceCorrection,
    AttendanceOut,
    CheckInInput,
    CheckInOut,
    FaceCheckInInput,
    LessonCreate,
    LessonOut,
    ManualAttendanceInput,
    Message,
)
from app.services.biometrics import compare_signatures, deserialize_signature
from app.services.attendance import now_in_school_timezone, register_check_in

router = APIRouter(tags=["Chamada"])


def assert_teacher_owns_lesson(teacher: Professor, lesson: Aula | None) -> Aula:
    if lesson is None:
        raise HTTPException(status_code=404, detail="Aula não encontrada.")
    if lesson.id_professor != teacher.id_professor:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Esta aula pertence a outro professor.",
        )
    return lesson


@router.post("/lessons", response_model=LessonOut, status_code=201)
def create_lesson(
    payload: LessonCreate,
    teacher: Professor = Depends(get_current_teacher),
    db: Session = Depends(get_db),
):
    """Abre uma chamada válida durante uma hora, no horário da escola."""
    class_group = db.get(Turma, payload.id_turma)
    if class_group is None:
        raise HTTPException(status_code=404, detail="Turma não encontrada.")
    if class_group.id_escola != teacher.id_escola:
        raise HTTPException(
            status_code=403,
            detail="Você só pode criar aulas para turmas da sua escola.",
        )

    starts_at = now_in_school_timezone()
    lesson = Aula(
        id_turma=class_group.id_turma,
        id_professor=teacher.id_professor,
        disciplina=payload.disciplina,
        inicio_previsto=starts_at,
        fim_previsto=starts_at + timedelta(hours=1),
    )
    db.add(lesson)
    db.commit()
    db.refresh(lesson)
    return lesson


@router.post("/lessons/{lesson_id}/close", response_model=Message)
def close_lesson(
    lesson_id: int,
    teacher: Professor = Depends(get_current_teacher),
    db: Session = Depends(get_db),
):
    lesson = assert_teacher_owns_lesson(teacher, db.get(Aula, lesson_id))
    lesson.ativa = False
    db.commit()
    return Message(message="Aula encerrada; novos registros automáticos foram bloqueados.")


@router.get("/lessons/available", response_model=list[LessonOut])
def list_available_lessons(
    student: Aluno = Depends(get_current_student),
    db: Session = Depends(get_db),
):
    now = now_in_school_timezone()
    return (
        db.query(Aula)
        .join(Matricula, Matricula.id_turma == Aula.id_turma)
        .filter(
            Matricula.id_aluno == student.id_aluno,
            Matricula.ativa.is_(True),
            Aula.ativa.is_(True),
            Aula.inicio_previsto <= now,
            Aula.fim_previsto >= now,
        )
        .order_by(Aula.inicio_previsto)
        .all()
    )


@router.post("/attendance/check-in", response_model=CheckInOut, status_code=201)
def check_in(
    payload: CheckInInput,
    student: Aluno = Depends(get_current_student),
    db: Session = Depends(get_db),
):
    result = register_check_in(db, student, payload)
    db.commit()
    db.refresh(result.attendance)
    return CheckInOut(message=result.message, attendance=result.attendance)


@router.post("/attendance/face-check-in", response_model=CheckInOut, status_code=201)
def face_check_in(
    payload: FaceCheckInInput,
    student: Aluno = Depends(get_current_student),
    db: Session = Depends(get_db),
):
    """Confirma a assinatura facial cadastrada e só então grava a presença."""
    enrolled_signature = deserialize_signature(student.vetor_face)
    if enrolled_signature is None:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Seu rosto ainda não foi cadastrado no FaceClass.",
        )

    matched, distance = compare_signatures(enrolled_signature, payload.assinatura)
    if not matched:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=(
                "O rosto lido não corresponde ao cadastro desta conta "
                f"(diferença biométrica: {distance})."
            ),
        )

    demo_payload = CheckInInput(
        id_aula=payload.id_aula,
        face_verification_token="face-geometry-verified",
        wifi_ssid=payload.wifi_ssid,
        latitude=payload.latitude,
        longitude=payload.longitude,
    )
    result = register_check_in(db, student, demo_payload, facial_verified=True)
    db.commit()
    db.refresh(result.attendance)
    return CheckInOut(message=result.message, attendance=result.attendance)


@router.post("/attendance/manual", response_model=AttendanceOut, status_code=201)
def register_manual_attendance(
    payload: ManualAttendanceInput,
    teacher: Professor = Depends(get_current_teacher),
    db: Session = Depends(get_db),
):
    lesson = assert_teacher_owns_lesson(teacher, db.get(Aula, payload.id_aula))
    student = db.get(Aluno, payload.id_aluno)
    if student is None:
        raise HTTPException(status_code=404, detail="Aluno não encontrado.")
    enrolled = (
        db.query(Matricula)
        .filter(
            Matricula.id_aluno == student.id_aluno,
            Matricula.id_turma == lesson.id_turma,
            Matricula.ativa.is_(True),
        )
        .first()
    )
    if enrolled is None:
        raise HTTPException(status_code=422, detail="Aluno não está matriculado nesta turma.")
    if (
        db.query(Presenca)
        .filter(Presenca.id_aula == lesson.id_aula, Presenca.id_aluno == student.id_aluno)
        .first()
        is not None
    ):
        raise HTTPException(
            status_code=409,
            detail="Já existe presença para este aluno nesta aula. Use a correção.",
        )

    attendance = Presenca(
        dia_hora=now_in_school_timezone(),
        id_aluno=student.id_aluno,
        id_aula=lesson.id_aula,
        tipo_presenca=payload.status != "FALTA",
        status=payload.status,
        origem="MANUAL",
        observacao=payload.observacao,
        corrigido_por_usuario_id=teacher.id_usuario,
        corrigido_em=now_in_school_timezone(),
    )
    db.add(attendance)
    db.commit()
    db.refresh(attendance)
    return attendance


@router.put("/attendance/{attendance_id}", response_model=AttendanceOut)
def correct_attendance(
    attendance_id: int,
    payload: AttendanceCorrection,
    teacher: Professor = Depends(get_current_teacher),
    db: Session = Depends(get_db),
):
    attendance = db.get(Presenca, attendance_id)
    if attendance is None:
        raise HTTPException(status_code=404, detail="Registro de presença não encontrado.")
    assert_teacher_owns_lesson(teacher, attendance.aula)

    attendance.status = payload.status
    attendance.tipo_presenca = payload.status != "FALTA"
    attendance.origem = "MANUAL"
    attendance.observacao = payload.observacao
    attendance.corrigido_por_usuario_id = teacher.id_usuario
    attendance.corrigido_em = now_in_school_timezone()
    db.commit()
    db.refresh(attendance)
    return attendance


@router.get("/attendance/my", response_model=list[AttendanceOut])
def my_attendance(
    student: Aluno = Depends(get_current_student),
    db: Session = Depends(get_db),
):
    return (
        db.query(Presenca)
        .filter(Presenca.id_aluno == student.id_aluno)
        .order_by(Presenca.dia_hora.desc())
        .all()
    )
