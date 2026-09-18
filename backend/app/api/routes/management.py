from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, require_roles
from app.core.security import hash_password
from app.db.database import get_db
from app.db.models import Aluno, Escola, Matricula, Pais, Professor, Turma, Usuario
from app.schemas import (
    ClassCreate,
    ClassOut,
    EnrollmentCreate,
    EnrollmentOut,
    FaceTemplateInput,
    GuardianCreate,
    GuardianOut,
    Message,
    SchoolCreate,
    SchoolOut,
    StudentCreate,
    StudentOut,
    TeacherCreate,
    TeacherOut,
)

router = APIRouter(tags=["Cadastros"])
AdminUser = Depends(require_roles("ADMIN"))


def commit_or_conflict(db: Session, entity):
    try:
        db.commit()
        db.refresh(entity)
        return entity
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Já existe um cadastro com um campo que deve ser único.",
        ) from exc


@router.post("/schools", response_model=SchoolOut, status_code=201)
def create_school(
    payload: SchoolCreate,
    db: Session = Depends(get_db),
    _: Usuario = AdminUser,
):
    school = Escola(**payload.model_dump())
    db.add(school)
    return commit_or_conflict(db, school)


@router.get("/schools", response_model=list[SchoolOut])
def list_schools(
    db: Session = Depends(get_db), _: Usuario = Depends(get_current_user)
):
    return db.query(Escola).order_by(Escola.nome_escola).all()


@router.post("/students", response_model=StudentOut, status_code=201)
def create_student(
    payload: StudentCreate,
    db: Session = Depends(get_db),
    _: Usuario = AdminUser,
):
    if db.get(Escola, payload.id_escola) is None:
        raise HTTPException(status_code=404, detail="Escola não encontrada.")

    user = Usuario(
        nome=payload.nome_aluno,
        email=str(payload.email).lower(),
        senha_hash=hash_password(payload.password),
        perfil="ALUNO",
    )
    student = Aluno(
        id_escola=payload.id_escola,
        nome_aluno=payload.nome_aluno,
        ra=payload.ra,
        hash_face=payload.hash_face.lower() if payload.hash_face else None,
        face_cadastrada_em=datetime.now() if payload.hash_face else None,
        usuario=user,
    )
    db.add(student)
    return commit_or_conflict(db, student)


@router.get("/students", response_model=list[StudentOut])
def list_students(
    school_id: int | None = None,
    db: Session = Depends(get_db),
    _: Usuario = AdminUser,
):
    query = db.query(Aluno)
    if school_id is not None:
        query = query.filter(Aluno.id_escola == school_id)
    return query.order_by(Aluno.nome_aluno).all()


@router.put("/students/{student_id}/face-template", response_model=Message)
def register_face_template(
    student_id: int,
    payload: FaceTemplateInput,
    db: Session = Depends(get_db),
    _: Usuario = AdminUser,
):
    """Registra ou substitui o hash do template facial de um aluno."""
    student = db.get(Aluno, student_id)
    if student is None:
        raise HTTPException(status_code=404, detail="Aluno não encontrado.")

    student.hash_face = payload.hash_face.lower()
    student.face_cadastrada_em = datetime.now()
    commit_or_conflict(db, student)
    return Message(message="Template facial do aluno registrado com sucesso.")


@router.post("/guardians", response_model=GuardianOut, status_code=201)
def create_guardian(
    payload: GuardianCreate,
    db: Session = Depends(get_db),
    _: Usuario = AdminUser,
):
    if db.get(Aluno, payload.id_aluno) is None:
        raise HTTPException(status_code=404, detail="Aluno não encontrado.")
    guardian = Pais(nome_pais=payload.nome_pais, email=str(payload.email), id_aluno=payload.id_aluno)
    db.add(guardian)
    return commit_or_conflict(db, guardian)


@router.post("/teachers", response_model=TeacherOut, status_code=201)
def create_teacher(
    payload: TeacherCreate,
    db: Session = Depends(get_db),
    _: Usuario = AdminUser,
):
    if db.get(Escola, payload.id_escola) is None:
        raise HTTPException(status_code=404, detail="Escola não encontrada.")

    user = Usuario(
        nome=payload.nome_professor,
        email=str(payload.email).lower(),
        senha_hash=hash_password(payload.password),
        perfil="PROFESSOR",
    )
    teacher = Professor(
        id_escola=payload.id_escola,
        nome_professor=payload.nome_professor,
        usuario=user,
    )
    db.add(teacher)
    return commit_or_conflict(db, teacher)


@router.get("/teachers", response_model=list[TeacherOut])
def list_teachers(
    school_id: int | None = None,
    db: Session = Depends(get_db),
    _: Usuario = AdminUser,
):
    query = db.query(Professor)
    if school_id is not None:
        query = query.filter(Professor.id_escola == school_id)
    return query.order_by(Professor.nome_professor).all()


@router.post("/classes", response_model=ClassOut, status_code=201)
def create_class(
    payload: ClassCreate,
    db: Session = Depends(get_db),
    _: Usuario = AdminUser,
):
    if db.get(Escola, payload.id_escola) is None:
        raise HTTPException(status_code=404, detail="Escola não encontrada.")
    class_group = Turma(**payload.model_dump())
    db.add(class_group)
    return commit_or_conflict(db, class_group)


@router.get("/classes", response_model=list[ClassOut])
def list_classes(
    school_id: int | None = None,
    db: Session = Depends(get_db),
    _: Usuario = Depends(get_current_user),
):
    query = db.query(Turma)
    if school_id is not None:
        query = query.filter(Turma.id_escola == school_id)
    return query.order_by(Turma.ano_letivo.desc(), Turma.nome_turma).all()


@router.post("/enrollments", response_model=EnrollmentOut, status_code=201)
def enroll_student(
    payload: EnrollmentCreate,
    db: Session = Depends(get_db),
    _: Usuario = AdminUser,
):
    student = db.get(Aluno, payload.id_aluno)
    class_group = db.get(Turma, payload.id_turma)
    if student is None or class_group is None:
        raise HTTPException(status_code=404, detail="Aluno ou turma não encontrado.")
    if student.id_escola != class_group.id_escola:
        raise HTTPException(
            status_code=422,
            detail="Aluno e turma precisam pertencer à mesma escola.",
        )
    enrollment = Matricula(id_aluno=student.id_aluno, id_turma=class_group.id_turma)
    db.add(enrollment)
    return commit_or_conflict(db, enrollment)
