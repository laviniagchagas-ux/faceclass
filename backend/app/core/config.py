from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Valores lidos do arquivo .env ou de variáveis de ambiente."""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    app_name: str = "FaceClass API"
    api_version: str = "1.0.0"
    database_url: str = (
        "mysql+pymysql://faceclass:faceclass@localhost:3306/face_class"
    )
    jwt_secret: str = "troque-esta-chave-por-uma-frase-longa-e-secreta"
    jwt_algorithm: str = "HS256"
    jwt_expire_minutes: int = 480
    timezone: str = "America/Sao_Paulo"

    demo_mode: bool = True
    demo_face_token: str = "faceclass-demo-ok"
    allow_bootstrap_admin: bool = True
    create_tables_on_start: bool = False


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
