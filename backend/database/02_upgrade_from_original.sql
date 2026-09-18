-- FaceClass - atualização para quem JÁ executou o SQL original recebido.
-- Compatível com MySQL 8: consulta o catálogo antes de adicionar objetos.
-- Pode ser retomado após execução parcial; faça backup antes.
-- Preserva objetos existentes; não corrige definições diferentes nem dados inválidos.

USE face_class;

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'escola'
          AND COLUMN_NAME = 'wifi_ssid'
    ),
    'DO 0',
    'ALTER TABLE escola ADD COLUMN wifi_ssid VARCHAR(100) NULL'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'escola'
          AND COLUMN_NAME = 'latitude'
    ),
    'DO 0',
    'ALTER TABLE escola ADD COLUMN latitude DECIMAL(10, 7) NULL'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'escola'
          AND COLUMN_NAME = 'longitude'
    ),
    'DO 0',
    'ALTER TABLE escola ADD COLUMN longitude DECIMAL(10, 7) NULL'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'escola'
          AND COLUMN_NAME = 'raio_permitido_metros'
    ),
    'DO 0',
    'ALTER TABLE escola ADD COLUMN raio_permitido_metros INT NOT NULL DEFAULT 150'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'aluno'
          AND COLUMN_NAME = 'id_usuario'
    ),
    'DO 0',
    'ALTER TABLE aluno ADD COLUMN id_usuario INT NULL'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

CREATE TABLE IF NOT EXISTS professor (
    id_professor INT PRIMARY KEY AUTO_INCREMENT,
    id_escola INT NOT NULL,
    id_usuario INT NOT NULL UNIQUE,
    nome_professor VARCHAR(100) NOT NULL,
    CONSTRAINT fk_professor_escola FOREIGN KEY (id_escola)
        REFERENCES escola(id_escola),
    CONSTRAINT fk_professor_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS turma (
    id_turma INT PRIMARY KEY AUTO_INCREMENT,
    id_escola INT NOT NULL,
    nome_turma VARCHAR(80) NOT NULL,
    ano_letivo INT NOT NULL,
    CONSTRAINT uq_turma_escola_nome UNIQUE (id_escola, nome_turma),
    CONSTRAINT fk_turma_escola FOREIGN KEY (id_escola)
        REFERENCES escola(id_escola)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS matricula (
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

CREATE TABLE IF NOT EXISTS aula (
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

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'presenca'
          AND COLUMN_NAME = 'id_aula'
    ),
    'DO 0',
    'ALTER TABLE presenca ADD COLUMN id_aula INT NULL'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'presenca'
          AND COLUMN_NAME = 'status'
    ),
    'DO 0',
    'ALTER TABLE presenca ADD COLUMN status VARCHAR(20) NULL'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'presenca'
          AND COLUMN_NAME = 'minutos_atraso'
    ),
    'DO 0',
    'ALTER TABLE presenca ADD COLUMN minutos_atraso INT NOT NULL DEFAULT 0'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'presenca'
          AND COLUMN_NAME = 'percentual_atraso_adicionado'
    ),
    'DO 0',
    'ALTER TABLE presenca ADD COLUMN percentual_atraso_adicionado INT NOT NULL DEFAULT 0'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'presenca'
          AND COLUMN_NAME = 'faltas_geradas_por_atraso'
    ),
    'DO 0',
    'ALTER TABLE presenca ADD COLUMN faltas_geradas_por_atraso INT NOT NULL DEFAULT 0'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'presenca'
          AND COLUMN_NAME = 'origem'
    ),
    'DO 0',
    'ALTER TABLE presenca ADD COLUMN origem VARCHAR(20) NOT NULL DEFAULT ''AUTOMATICO'''
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'presenca'
          AND COLUMN_NAME = 'observacao'
    ),
    'DO 0',
    'ALTER TABLE presenca ADD COLUMN observacao TEXT NULL'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'presenca'
          AND COLUMN_NAME = 'corrigido_por_usuario_id'
    ),
    'DO 0',
    'ALTER TABLE presenca ADD COLUMN corrigido_por_usuario_id INT NULL'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'presenca'
          AND COLUMN_NAME = 'corrigido_em'
    ),
    'DO 0',
    'ALTER TABLE presenca ADD COLUMN corrigido_em DATETIME NULL'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

-- Dá um status aos registros que já existiam no banco antigo.
UPDATE presenca
SET status = CASE WHEN tipo_presenca THEN 'PRESENTE' ELSE 'FALTA' END
WHERE status IS NULL;

ALTER TABLE presenca MODIFY COLUMN status VARCHAR(20) NOT NULL;

CREATE TABLE IF NOT EXISTS saldo_atraso (
    id_saldo_atraso INT PRIMARY KEY AUTO_INCREMENT,
    id_aluno INT NOT NULL,
    periodo DATE NOT NULL,
    percentual_acumulado INT NOT NULL DEFAULT 0,
    faltas_convertidas INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_saldo_aluno_periodo UNIQUE (id_aluno, periodo),
    CONSTRAINT fk_saldo_aluno FOREIGN KEY (id_aluno)
        REFERENCES aluno(id_aluno)
) ENGINE=InnoDB;

-- Adiciona apenas as chaves e os índices ausentes.
SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'aluno'
          AND CONSTRAINT_NAME = 'fk_aluno_usuario'
    ),
    'DO 0',
    'ALTER TABLE aluno ADD CONSTRAINT fk_aluno_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'presenca'
          AND CONSTRAINT_NAME = 'fk_presenca_aula'
    ),
    'DO 0',
    'ALTER TABLE presenca ADD CONSTRAINT fk_presenca_aula FOREIGN KEY (id_aula) REFERENCES aula(id_aula)'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'presenca'
          AND CONSTRAINT_NAME = 'fk_presenca_corretor'
    ),
    'DO 0',
    'ALTER TABLE presenca ADD CONSTRAINT fk_presenca_corretor FOREIGN KEY (corrigido_por_usuario_id) REFERENCES usuario(id_usuario)'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT INDEX_NAME FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'aluno' AND NON_UNIQUE = 0
        GROUP BY INDEX_NAME
        HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') = 'id_usuario'
           AND SUM(SUB_PART IS NOT NULL) = 0
    ),
    'DO 0',
    'CREATE UNIQUE INDEX uq_aluno_usuario ON aluno (id_usuario)'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT INDEX_NAME FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'presenca' AND NON_UNIQUE = 0
        GROUP BY INDEX_NAME
        HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') = 'id_aula,id_aluno'
           AND SUM(SUB_PART IS NOT NULL) = 0
    ),
    'DO 0',
    'CREATE UNIQUE INDEX uq_presenca_aula_aluno ON presenca (id_aula,id_aluno)'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT INDEX_NAME FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'aula'
        GROUP BY INDEX_NAME
        HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') = 'id_turma,inicio_previsto,fim_previsto'
           AND SUM(SUB_PART IS NOT NULL) = 0
    ),
    'DO 0',
    'CREATE INDEX idx_aula_turma_horario ON aula (id_turma,inicio_previsto,fim_previsto)'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT INDEX_NAME FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'presenca'
        GROUP BY INDEX_NAME
        HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') = 'id_aluno,dia_hora'
           AND SUM(SUB_PART IS NOT NULL) = 0
    ),
    'DO 0',
    'CREATE INDEX idx_presenca_aluno_data ON presenca (id_aluno,dia_hora)'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

