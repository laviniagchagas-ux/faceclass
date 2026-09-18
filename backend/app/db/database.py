from collections.abc import Generator

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker
from sqlalchemy.pool import StaticPool

from app.core.config import settings


class Base(DeclarativeBase):
    pass


engine_options = {"pool_pre_ping": True, "pool_recycle": 3600}

# O banco em memória é usado para a demonstração local quando o MySQL não está
# disponível. StaticPool mantém os dados entre as requisições do mesmo servidor.
if settings.database_url == "sqlite://":
    engine_options["connect_args"] = {"check_same_thread": False}
    engine_options["poolclass"] = StaticPool

engine = create_engine(settings.database_url, **engine_options)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
