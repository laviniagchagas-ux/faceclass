from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import attendance, auth, biometrics, dashboard, management
from app.core.config import settings
from app.core.security import hash_password
from app.db.database import Base, SessionLocal, engine

# Garante que todos os modelos sejam registrados antes de create_all.
from app.db import models  # noqa: F401
from app.db.models import Aluno, Escola, Matricula, Professor, Turma, Usuario


def seed_demo_data() -> None:
    """Cria os dados mínimos para o app funcionar na demonstração local."""
    if not settings.demo_mode:
        return

    db = SessionLocal()
    try:
        if db.query(Usuario).first() is not None:
            return

        escola = Escola(nome_escola="Escola FaceClass")
        admin = Usuario(
            nome="Administrador",
            email="admin@faceclass.com",
            senha_hash=hash_password("Admin2026!"),
            perfil="ADMIN",
        )
        usuario_professor = Usuario(
            nome="Professora Ana",
            email="professor@faceclass.com",
            senha_hash=hash_password("Professor2026!"),
            perfil="PROFESSOR",
        )
        professor = Professor(
            escola=escola,
            usuario=usuario_professor,
            nome_professor="Professora Ana",
        )
        usuario_aluno = Usuario(
            nome="Aluno Demonstração",
            email="aluno@faceclass.com",
            senha_hash=hash_password("Aluno2026!"),
            perfil="ALUNO",
        )
        aluno = Aluno(
            escola=escola,
            usuario=usuario_aluno,
            nome_aluno="Aluno Demonstração",
            ra="20260001",
        )
        turma = Turma(escola=escola, nome_turma="3º A", ano_letivo=2026)
        matricula = Matricula(aluno=aluno, turma=turma)
        db.add_all(
            [escola, admin, professor, aluno, turma, matricula]
        )
        db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


@asynccontextmanager
async def lifespan(_: FastAPI):
    if settings.create_tables_on_start:
        Base.metadata.create_all(bind=engine)
        seed_demo_data()
    yield


app = FastAPI(
    title=settings.app_name,
    version=settings.api_version,
    description=(
        "API do projeto FaceClass: cadastro, autenticação, chamada por aula, "
        "atrasos e painel do professor."
    ),
    lifespan=lifespan,
)

# Durante o desenvolvimento, o front-end local pode chamar a API.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(management.router)
app.include_router(biometrics.router)
app.include_router(attendance.router)
app.include_router(dashboard.router)


@app.get("/health", tags=["Sistema"])
def health():
    return {"status": "ok", "service": settings.app_name}
