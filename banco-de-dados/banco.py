import os
from contextlib import contextmanager
from pathlib import Path

import mysql.connector
from dotenv import load_dotenv

# Localiza a pasta do backend para carregar a configuração deste projeto.
BASE = Path(__file__).resolve().parent


# Lê as credenciais do banco guardadas no arquivo local .env.
def carregar_configuracao():
    # No fluxo local, o arquivo salvo pelo assistente é a configuração escolhida.
    # Senhas são literais: não expandir trechos como ${VARIAVEL}.
    load_dotenv(BASE / ".env", override=True, interpolate=False)


carregar_configuracao()


# Abre uma conexão MySQL usando os dados definidos no .env.
def conectar():
    senha = os.getenv("DB_PASSWORD")
    usuario = os.getenv("DB_USER")
    if senha is None or not usuario:
        raise RuntimeError("Configure DB_USER e DB_PASSWORD no arquivo .env.")

    return mysql.connector.connect(
        host=os.getenv("DB_HOST", "127.0.0.1"),
        port=int(os.getenv("DB_PORT", "3306")),
        user=usuario,
        password=senha,
        database=os.getenv("DB_NAME", "faceclass"),
        connection_timeout=5,
        autocommit=False,
    )


# Agrupa operações do banco para confirmar tudo junto ou desfazer em caso de erro.
@contextmanager
def transacao():
    conexao = conectar()
    cursor = None
    try:
        cursor = conexao.cursor(dictionary=True)
        yield conexao, cursor
        conexao.commit()
    except Exception:
        conexao.rollback()
        raise
    finally:
        if cursor is not None:
            cursor.close()
        conexao.close()
