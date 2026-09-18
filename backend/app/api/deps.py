from collections.abc import Callable

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.core.security import get_token_subject
from app.db.database import get_db
from app.db.models import Aluno, Professor, Usuario

# A API de login recebe JSON. HTTPBearer faz a documentação Swagger pedir
# diretamente o token devolvido por /auth/login, no formato Bearer token.
bearer_scheme = HTTPBearer()


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> Usuario:
    token = credentials.credentials
    user_id = get_token_subject(token)
    user = db.get(Usuario, user_id)
    if user is None or not user.ativo:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Usuário não encontrado ou inativo.",
        )
    return user


def require_roles(*roles: str) -> Callable:
    def validate_role(current_user: Usuario = Depends(get_current_user)) -> Usuario:
        if current_user.perfil not in roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Seu perfil não tem permissão para esta ação.",
            )
        return current_user

    return validate_role


def get_current_student(
    current_user: Usuario = Depends(require_roles("ALUNO")),
    db: Session = Depends(get_db),
) -> Aluno:
    student = db.query(Aluno).filter(Aluno.id_usuario == current_user.id_usuario).first()
    if student is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Não existe aluno vinculado a este usuário.",
        )
    return student


def get_current_teacher(
    current_user: Usuario = Depends(require_roles("PROFESSOR")),
    db: Session = Depends(get_db),
) -> Professor:
    teacher = (
        db.query(Professor)
        .filter(Professor.id_usuario == current_user.id_usuario)
        .first()
    )
    if teacher is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Não existe professor vinculado a este usuário.",
        )
    return teacher
