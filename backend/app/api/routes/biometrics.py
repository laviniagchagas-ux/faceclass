from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_student
from app.db.database import get_db
from app.db.models import Aluno
from app.schemas import FaceSignatureInput, FaceStatus, Message
from app.services.biometrics import serialize_signature, signature_hash

router = APIRouter(prefix="/biometrics", tags=["Biometria facial"])


@router.get("/status", response_model=FaceStatus)
def biometric_status(student: Aluno = Depends(get_current_student)):
    return FaceStatus(
        cadastrada=bool(student.vetor_face and student.hash_face),
        face_cadastrada_em=student.face_cadastrada_em,
    )


@router.post("/enroll", response_model=FaceStatus)
def enroll_face(
    payload: FaceSignatureInput,
    student: Aluno = Depends(get_current_student),
    db: Session = Depends(get_db),
):
    """Cadastra a assinatura facial da própria conta do aluno autenticado."""
    try:
        student.vetor_face = serialize_signature(payload.assinatura)
        student.hash_face = signature_hash(payload.assinatura)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    student.face_cadastrada_em = datetime.now()
    db.commit()
    db.refresh(student)
    return FaceStatus(cadastrada=True, face_cadastrada_em=student.face_cadastrada_em)


@router.delete("/enroll", response_model=Message)
def delete_face_enrollment(
    student: Aluno = Depends(get_current_student),
    db: Session = Depends(get_db),
):
    """Permite remover o próprio cadastro biométrico no ambiente de demonstração."""
    student.vetor_face = None
    student.hash_face = None
    student.face_cadastrada_em = None
    db.commit()
    return Message(message="Cadastro facial removido.")
