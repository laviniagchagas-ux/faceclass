from __future__ import annotations

from datetime import date, datetime
from typing import Optional

from sqlalchemy import (
    Boolean,
    Date,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
    UniqueConstraint,
    func,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.database import Base


class Escola(Base):
    __tablename__ = "escola"

    id_escola: Mapped[int] = mapped_column(Integer, primary_key=True)
    nome_escola: Mapped[str] = mapped_column(String(100), nullable=False)
    wifi_ssid: Mapped[Optional[str]] = mapped_column(String(100))
    latitude: Mapped[Optional[float]] = mapped_column(Float)
    longitude: Mapped[Optional[float]] = mapped_column(Float)
    raio_permitido_metros: Mapped[int] = mapped_column(Integer, default=150, nullable=False)

    alunos: Mapped[list[Aluno]] = relationship(back_populates="escola")
    professores: Mapped[list[Professor]] = relationship(back_populates="escola")
    turmas: Mapped[list[Turma]] = relationship(back_populates="escola")


class Usuario(Base):
    __tablename__ = "usuario"

    id_usuario: Mapped[int] = mapped_column(Integer, primary_key=True)
    nome: Mapped[str] = mapped_column(String(100), nullable=False)
    email: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    senha_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    perfil: Mapped[str] = mapped_column(String(20), nullable=False)
    ativo: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    criado_em: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), nullable=False
    )

    aluno: Mapped[Optional[Aluno]] = relationship(back_populates="usuario")
    professor: Mapped[Optional[Professor]] = relationship(back_populates="usuario")


class Aluno(Base):
    __tablename__ = "aluno"

    id_aluno: Mapped[int] = mapped_column(Integer, primary_key=True)
    id_escola: Mapped[int] = mapped_column(ForeignKey("escola.id_escola"), nullable=False)
    id_usuario: Mapped[Optional[int]] = mapped_column(
        ForeignKey("usuario.id_usuario"), unique=True
    )
    nome_aluno: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    ra: Mapped[Optional[str]] = mapped_column(String(14), unique=True)
    # Template facial em hash SHA-256. Nunca guarda foto ou vídeo do aluno.
    hash_face: Mapped[Optional[str]] = mapped_column(String(64), unique=True)
    # JSON com uma assinatura geométrica facial normalizada. Nunca contém imagem.
    vetor_face: Mapped[Optional[str]] = mapped_column(Text)
    face_cadastrada_em: Mapped[Optional[datetime]] = mapped_column(DateTime)

    escola: Mapped[Escola] = relationship(back_populates="alunos")
    usuario: Mapped[Optional[Usuario]] = relationship(back_populates="aluno")
    responsaveis: Mapped[list[Pais]] = relationship(back_populates="aluno")
    matriculas: Mapped[list[Matricula]] = relationship(back_populates="aluno")
    presencas: Mapped[list[Presenca]] = relationship(back_populates="aluno")
    saldos_atraso: Mapped[list[SaldoAtraso]] = relationship(back_populates="aluno")


class Pais(Base):
    __tablename__ = "pais"

    id_pais: Mapped[int] = mapped_column(Integer, primary_key=True)
    nome_pais: Mapped[str] = mapped_column(String(100), nullable=False)
    email: Mapped[str] = mapped_column(String(100), nullable=False)
    id_aluno: Mapped[int] = mapped_column(ForeignKey("aluno.id_aluno"), nullable=False)

    aluno: Mapped[Aluno] = relationship(back_populates="responsaveis")


class Professor(Base):
    __tablename__ = "professor"

    id_professor: Mapped[int] = mapped_column(Integer, primary_key=True)
    id_escola: Mapped[int] = mapped_column(ForeignKey("escola.id_escola"), nullable=False)
    id_usuario: Mapped[int] = mapped_column(
        ForeignKey("usuario.id_usuario"), unique=True, nullable=False
    )
    nome_professor: Mapped[str] = mapped_column(String(100), nullable=False)

    escola: Mapped[Escola] = relationship(back_populates="professores")
    usuario: Mapped[Usuario] = relationship(back_populates="professor")
    aulas: Mapped[list[Aula]] = relationship(back_populates="professor")


class Turma(Base):
    __tablename__ = "turma"
    __table_args__ = (
        UniqueConstraint("id_escola", "nome_turma", name="uq_turma_escola_nome"),
    )

    id_turma: Mapped[int] = mapped_column(Integer, primary_key=True)
    id_escola: Mapped[int] = mapped_column(ForeignKey("escola.id_escola"), nullable=False)
    nome_turma: Mapped[str] = mapped_column(String(80), nullable=False)
    ano_letivo: Mapped[int] = mapped_column(Integer, nullable=False)

    escola: Mapped[Escola] = relationship(back_populates="turmas")
    matriculas: Mapped[list[Matricula]] = relationship(back_populates="turma")
    aulas: Mapped[list[Aula]] = relationship(back_populates="turma")


class Matricula(Base):
    __tablename__ = "matricula"
    __table_args__ = (
        UniqueConstraint("id_aluno", "id_turma", name="uq_matricula_aluno_turma"),
    )

    id_matricula: Mapped[int] = mapped_column(Integer, primary_key=True)
    id_aluno: Mapped[int] = mapped_column(ForeignKey("aluno.id_aluno"), nullable=False)
    id_turma: Mapped[int] = mapped_column(ForeignKey("turma.id_turma"), nullable=False)
    ativa: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    aluno: Mapped[Aluno] = relationship(back_populates="matriculas")
    turma: Mapped[Turma] = relationship(back_populates="matriculas")


class Aula(Base):
    __tablename__ = "aula"

    id_aula: Mapped[int] = mapped_column(Integer, primary_key=True)
    id_turma: Mapped[int] = mapped_column(ForeignKey("turma.id_turma"), nullable=False)
    id_professor: Mapped[int] = mapped_column(
        ForeignKey("professor.id_professor"), nullable=False
    )
    disciplina: Mapped[str] = mapped_column(String(100), nullable=False)
    inicio_previsto: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    fim_previsto: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    ativa: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    turma: Mapped[Turma] = relationship(back_populates="aulas")
    professor: Mapped[Professor] = relationship(back_populates="aulas")
    presencas: Mapped[list[Presenca]] = relationship(back_populates="aula")


class Presenca(Base):
    __tablename__ = "presenca"
    __table_args__ = (
        UniqueConstraint("id_aula", "id_aluno", name="uq_presenca_aula_aluno"),
    )

    id_presenca: Mapped[int] = mapped_column(Integer, primary_key=True)
    dia_hora: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    id_aluno: Mapped[int] = mapped_column(ForeignKey("aluno.id_aluno"), nullable=False)
    id_aula: Mapped[Optional[int]] = mapped_column(ForeignKey("aula.id_aula"))
    # Campo legado do SQL original: True para presença ou atraso; False para falta.
    tipo_presenca: Mapped[bool] = mapped_column(Boolean, nullable=False)
    status: Mapped[str] = mapped_column(String(20), nullable=False)
    minutos_atraso: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    percentual_atraso_adicionado: Mapped[int] = mapped_column(
        Integer, default=0, nullable=False
    )
    faltas_geradas_por_atraso: Mapped[int] = mapped_column(
        Integer, default=0, nullable=False
    )
    origem: Mapped[str] = mapped_column(String(20), default="AUTOMATICO", nullable=False)
    observacao: Mapped[Optional[str]] = mapped_column(Text)
    corrigido_por_usuario_id: Mapped[Optional[int]] = mapped_column(
        ForeignKey("usuario.id_usuario")
    )
    corrigido_em: Mapped[Optional[datetime]] = mapped_column(DateTime)

    aluno: Mapped[Aluno] = relationship(back_populates="presencas")
    aula: Mapped[Optional[Aula]] = relationship(back_populates="presencas")


class SaldoAtraso(Base):
    __tablename__ = "saldo_atraso"
    __table_args__ = (
        UniqueConstraint("id_aluno", "periodo", name="uq_saldo_aluno_periodo"),
    )

    id_saldo_atraso: Mapped[int] = mapped_column(Integer, primary_key=True)
    id_aluno: Mapped[int] = mapped_column(ForeignKey("aluno.id_aluno"), nullable=False)
    periodo: Mapped[date] = mapped_column(Date, nullable=False)
    percentual_acumulado: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    faltas_convertidas: Mapped[int] = mapped_column(Integer, default=0, nullable=False)

    aluno: Mapped[Aluno] = relationship(back_populates="saldos_atraso")
