"""Teste de ponta a ponta sem MySQL: usa SQLite temporário e TestClient.

Execute a partir da raiz do projeto com:
    python tests/smoke_test.py
"""

import os
import sys
from datetime import datetime, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

database_file = Path(__file__).with_name("faceclass_smoke.sqlite3")
database_file.unlink(missing_ok=True)
project_root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(project_root))
os.environ["DATABASE_URL"] = f"sqlite:///{database_file.as_posix()}"
os.environ["CREATE_TABLES_ON_START"] = "true"
os.environ["DEMO_MODE"] = "true"

from fastapi.testclient import TestClient  # noqa: E402

from app.db.database import engine  # noqa: E402
from app.main import app  # noqa: E402


def require_status(response, expected_status: int):
    if response.status_code != expected_status:
        raise AssertionError(f"Esperado {expected_status}; recebido {response.status_code}: {response.text}")
    return response.json()


def login(client: TestClient, email: str, password: str) -> str:
    body = require_status(
        client.post("/auth/login", json={"email": email, "password": password}), 200
    )
    return body["access_token"]


with TestClient(app) as client:
    admin = require_status(
        client.post(
            "/auth/bootstrap-admin",
            json={
                "nome": "Administrador",
                "email": "admin@example.com",
                "password": "senha-segura-123",
            },
        ),
        201,
    )
    assert admin["perfil"] == "ADMIN"
    admin_headers = {"Authorization": f"Bearer {login(client, 'admin@example.com', 'senha-segura-123')}"}

    school = require_status(
        client.post(
            "/schools",
            headers=admin_headers,
            json={"nome_escola": "Escola Modelo"},
        ),
        201,
    )
    teacher = require_status(
        client.post(
            "/teachers",
            headers=admin_headers,
            json={
                "id_escola": school["id_escola"],
                "nome_professor": "Professora Ana",
                "email": "ana@example.com",
                "password": "senha-segura-123",
            },
        ),
        201,
    )
    student = require_status(
        client.post(
            "/students",
            headers=admin_headers,
            json={
                "id_escola": school["id_escola"],
                "nome_aluno": "Aluno Teste",
                "ra": "20260001",
                "email": "aluno@example.com",
                "password": "senha-segura-123",
            },
        ),
        201,
    )
    class_group = require_status(
        client.post(
            "/classes",
            headers=admin_headers,
            json={
                "id_escola": school["id_escola"],
                "nome_turma": "3º A",
                "ano_letivo": 2026,
            },
        ),
        201,
    )
    require_status(
        client.post(
            "/enrollments",
            headers=admin_headers,
            json={
                "id_aluno": student["id_aluno"],
                "id_turma": class_group["id_turma"],
            },
        ),
        201,
    )

    teacher_headers = {
        "Authorization": f"Bearer {login(client, 'ana@example.com', 'senha-segura-123')}"
    }
    now = datetime.now(ZoneInfo("America/Sao_Paulo")).replace(tzinfo=None, microsecond=0)
    lesson = require_status(
        client.post(
            "/lessons",
            headers=teacher_headers,
            json={
                "id_turma": class_group["id_turma"],
                "disciplina": "Matemática",
                "inicio_previsto": (now - timedelta(minutes=5)).isoformat(),
                "fim_previsto": (now + timedelta(minutes=55)).isoformat(),
            },
        ),
        201,
    )

    student_headers = {
        "Authorization": f"Bearer {login(client, 'aluno@example.com', 'senha-segura-123')}"
    }
    available_lessons = require_status(
        client.get("/lessons/available", headers=student_headers), 200
    )
    assert available_lessons[0]["id_aula"] == lesson["id_aula"]
    checked_in = require_status(
        client.post(
            "/attendance/check-in",
            headers=student_headers,
            json={
                "id_aula": lesson["id_aula"],
                "face_verification_token": "faceclass-demo-ok",
            },
        ),
        201,
    )
    assert checked_in["attendance"]["status"] == "PRESENTE"

    dashboard = require_status(
        client.get(
            f"/dashboard/classes/{class_group['id_turma']}",
            headers=teacher_headers,
        ),
        200,
    )
    assert dashboard["alunos"][0]["presentes"] == 1

engine.dispose()
database_file.unlink(missing_ok=True)
print("Smoke test concluído com sucesso.")
