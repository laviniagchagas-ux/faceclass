-- FaceClass - esquema completo do banco de dados (MySQL 8+)
-- Execute este arquivo em um banco novo. Ele mantém as quatro entidades
-- fornecidas inicialmente: escola, aluno, pais e presenca.

CREATE DATABASE IF NOT EXISTS face_class
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE face_class;

CREATE TABLE escola (
    id_escola INT PRIMARY KEY AUTO_INCREMENT,
    nome_escola VARCHAR(100) NOT NULL,
    wifi_ssid VARCHAR(100) NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    raio_permitido_metros INT NOT NULL DEFAULT 150
) ENGINE=InnoDB;

CREATE TABLE usuario (
    id_usuario INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ADMIN', 'PROFESSOR', 'ALUNO'))
) ENGINE=InnoDB;

CREATE TABLE aluno (
    id_aluno INT PRIMARY KEY AUTO_INCREMENT,
    id_escola INT NOT NULL,
    id_usuario INT NULL UNIQUE,
    nome_aluno VARCHAR(100) NOT NULL UNIQUE,
    ra VARCHAR(14) UNIQUE,
    -- Assinatura geométrica facial em JSON e hash SHA-256 para integridade.
    -- Nunca armazena foto, vídeo ou imagem em Base64 do aluno.
    hash_face CHAR(64) NULL UNIQUE,
    vetor_face LONGTEXT NULL,
    face_cadastrada_em DATETIME NULL,
    CONSTRAINT fk_aluno_escola FOREIGN KEY (id_escola)
        REFERENCES escola(id_escola),
    CONSTRAINT fk_aluno_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE pais (
    id_pais INT PRIMARY KEY AUTO_INCREMENT,
    nome_pais VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    id_aluno INT NOT NULL,
    CONSTRAINT fk_pais_aluno FOREIGN KEY (id_aluno)
        REFERENCES aluno(id_aluno)
) ENGINE=InnoDB;

CREATE TABLE professor (
    id_professor INT PRIMARY KEY AUTO_INCREMENT,
    id_escola INT NOT NULL,
    id_usuario INT NOT NULL UNIQUE,
    nome_professor VARCHAR(100) NOT NULL,
    CONSTRAINT fk_professor_escola FOREIGN KEY (id_escola)
        REFERENCES escola(id_escola),
    CONSTRAINT fk_professor_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE turma (
    id_turma INT PRIMARY KEY AUTO_INCREMENT,
    id_escola INT NOT NULL,
    nome_turma VARCHAR(80) NOT NULL,
    ano_letivo INT NOT NULL,
    CONSTRAINT uq_turma_escola_nome UNIQUE (id_escola, nome_turma),
    CONSTRAINT fk_turma_escola FOREIGN KEY (id_escola)
        REFERENCES escola(id_escola)
) ENGINE=InnoDB;

CREATE TABLE matricula (
    id_matricula INT PRIMARY KEY AUTO_INCREMENT,
    id_aluno INT NOT NULL,
    id_turma INT NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_matricula_aluno_turma UNIQUE (id_aluno, id_turma),
    CONSTRAINT fk_matricula_aluno FOREIGN KEY (id_aluno)
        REFERENCES aluno(id_aluno),
    CONSTRAINT fk_matricula_turma FOREIGN KEY (id_turma)
        REFERENCES turma(id_turma)
) ENGINE=InnoDB;

CREATE TABLE aula (
    id_aula INT PRIMARY KEY AUTO_INCREMENT,
    id_turma INT NOT NULL,
    id_professor INT NOT NULL,
    disciplina VARCHAR(100) NOT NULL,
    inicio_previsto DATETIME NOT NULL,
    fim_previsto DATETIME NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_aula_turma FOREIGN KEY (id_turma)
        REFERENCES turma(id_turma),
    CONSTRAINT fk_aula_professor FOREIGN KEY (id_professor)
        REFERENCES professor(id_professor)
) ENGINE=InnoDB;

CREATE TABLE presenca (
    id_presenca INT PRIMARY KEY AUTO_INCREMENT,
    dia_hora DATETIME NOT NULL,
    id_aluno INT NOT NULL,
    id_aula INT NULL,
    -- Campo trazido do banco original: 1 = presente/atraso; 0 = falta.
    tipo_presenca BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    minutos_atraso INT NOT NULL DEFAULT 0,
    percentual_atraso_adicionado INT NOT NULL DEFAULT 0,
    faltas_geradas_por_atraso INT NOT NULL DEFAULT 0,
    origem VARCHAR(20) NOT NULL DEFAULT 'AUTOMATICO',
    observacao TEXT NULL,
    corrigido_por_usuario_id INT NULL,
    corrigido_em DATETIME NULL,
    CONSTRAINT uq_presenca_aula_aluno UNIQUE (id_aula, id_aluno),
    CONSTRAINT ck_presenca_status CHECK (status IN ('PRESENTE', 'ATRASO', 'FALTA')),
    CONSTRAINT ck_presenca_origem CHECK (origem IN ('AUTOMATICO', 'MANUAL')),
    CONSTRAINT fk_presenca_aluno FOREIGN KEY (id_aluno)
        REFERENCES aluno(id_aluno),
    CONSTRAINT fk_presenca_aula FOREIGN KEY (id_aula)
        REFERENCES aula(id_aula),
    CONSTRAINT fk_presenca_corretor FOREIGN KEY (corrigido_por_usuario_id)
        REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE saldo_atraso (
    id_saldo_atraso INT PRIMARY KEY AUTO_INCREMENT,
    id_aluno INT NOT NULL,
    periodo DATE NOT NULL,
    percentual_acumulado INT NOT NULL DEFAULT 0,
    faltas_convertidas INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_saldo_aluno_periodo UNIQUE (id_aluno, periodo),
    CONSTRAINT fk_saldo_aluno FOREIGN KEY (id_aluno)
        REFERENCES aluno(id_aluno)
) ENGINE=InnoDB;

CREATE INDEX idx_aula_turma_horario
    ON aula (id_turma, inicio_previsto, fim_previsto);
CREATE INDEX idx_presenca_aluno_data
    ON presenca (id_aluno, dia_hora);
