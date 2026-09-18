-- FaceClass: requisito de biometria facial do aluno.
-- Execute no MySQL Workbench, com o schema face_class selecionado.
-- Pode ser retomado: adiciona somente colunas e índice ausentes.
-- O valor salvo deve ser o hash SHA-256 de um template facial gerado por um
-- serviço de reconhecimento; não salve foto, imagem em Base64 ou vídeo.

USE face_class;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'aluno'
          AND COLUMN_NAME = 'hash_face'
    ),
    'DO 0',
    'ALTER TABLE aluno ADD COLUMN hash_face CHAR(64) NULL AFTER ra'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'aluno'
          AND COLUMN_NAME = 'face_cadastrada_em'
    ),
    'DO 0',
    'ALTER TABLE aluno ADD COLUMN face_cadastrada_em DATETIME NULL AFTER hash_face'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT INDEX_NAME FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'aluno'
          AND NON_UNIQUE = 0
        GROUP BY INDEX_NAME
        HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') = 'hash_face'
           AND SUM(SUB_PART IS NOT NULL) = 0
    ),
    'DO 0',
    'CREATE UNIQUE INDEX uq_aluno_hash_face ON aluno (hash_face)'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;

-- Exemplo APENAS para demonstração do TCC (não representa biometria real):
-- UPDATE aluno
-- SET hash_face = SHA2('faceclass-demo-aluno-1', 256),
--     face_cadastrada_em = NOW()
-- WHERE id_aluno = 1;
