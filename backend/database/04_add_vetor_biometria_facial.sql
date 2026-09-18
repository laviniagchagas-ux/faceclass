-- FaceClass - migração para a comparação facial do protótipo.
-- Execute no MySQL Workbench, dentro do schema face_class.
-- Pode ser executado novamente: preserva a coluna se ela já existir.
-- A coluna guarda somente 12 medidas geométricas normalizadas em JSON;
-- nenhuma foto ou vídeo de aluno é gravado no banco.

USE face_class;

SET @faceclass_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'aluno'
          AND COLUMN_NAME = 'vetor_face'
    ),
    'DO 0',
    'ALTER TABLE aluno ADD COLUMN vetor_face LONGTEXT NULL AFTER hash_face'
);
PREPARE faceclass_stmt FROM @faceclass_sql;
EXECUTE faceclass_stmt;
DEALLOCATE PREPARE faceclass_stmt;
