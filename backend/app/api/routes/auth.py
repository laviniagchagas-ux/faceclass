from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.config import settings
from app.core.security import create_access_token, hash_password, verify_password
from app.db.database import get_db
from app.db.models import Usuario
from app.schemas import (
    BootstrapAdminInput,
    CurrentUserOut,
    LoginInput,
    Token,
)

router = APIRouter(prefix="/auth", tags=["Autenticação"])


@router.post("/bootstrap-admin", response_model=CurrentUserOut, status_code=201)
def bootstrap_admin(payload: BootstrapAdminInput, db: Session = Depends(get_db)):
    """Cria o primeiro administrador uma única vez, apenas quando permitido no .env."""
    if not settings.allow_bootstrap_admin:
        raise HTTPException(status_code=403, detail="Bootstrap de administrador desativado.")
    if db.query(Usuario).first() is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Já existe um usuário. Use o administrador existente.",
        )
    admin = Usuario(
        nome=payload.nome,
        email=str(payload.email).lower(),
        senha_hash=hash_password(payload.password),
        perfil="ADMIN",
    )
    db.add(admin)
    db.commit()
    db.refresh(admin)
    return admin


@router.post("/login", response_model=Token)
def login(payload: LoginInput, db: Session = Depends(get_db)):
    user = (
        db.query(Usuario)
        .filter(Usuario.email == str(payload.email).lower())
        .first()
    )
    if user is None or not user.ativo or not verify_password(payload.password, user.senha_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="E-mail ou senha inválidos.",
        )
    return Token(
        access_token=create_access_token(user.id_usuario, user.perfil),
        user_id=user.id_usuario,
        role=user.perfil,
    )


@router.get("/me", response_model=CurrentUserOut)
def current_user(current_user: Usuario = Depends(get_current_user)):
    return current_user
