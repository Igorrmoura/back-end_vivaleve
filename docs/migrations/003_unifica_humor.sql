-- Migração: o registro de humor passa a existir só em "registros_humor" (MoodLog)
--
-- Antes, o front gravava cada humor duas vezes: em /v1/mood-logs (tabela registros_humor) e em
-- /v1/entries (tabela registros + registro_atividades, usada pelas estatísticas e pela IA).
-- Agora o MoodLog tem as atividades (tabela nova registro_humor_atividades, criada pelo
-- "ddl-auto: update") e o Entry foi apagado do código.
--
-- ORDEM (não inverter):
--   1. Deploy do back com esta versão (cria registro_humor_atividades).
--   2. Deploy do front que para de chamar /v1/entries.
--   3. Backup das tabelas antigas (Render → Postgres → Backups, ou pg_dump -t registros -t registro_atividades).
--   4. Rodar o PASSO A e conferir o resultado.
--   5. Rodar o PASSO B (drop).
--
-- Se o PASSO B rodar ANTES do deploy, nada quebra no banco, mas o back antigo ainda tem a
-- EntryEntity e o Hibernate recria as tabelas vazias.

-- PASSO A: copia as atividades de cada Entry para o MoodLog gravado junto com ele.
-- Os dois POSTs saem do front no mesmo clique, então o par é o MoodLog do mesmo usuário com o
-- created_at mais próximo (até 5 minutos). A data não é usada no casamento porque o Entry usava
-- a data do navegador e o MoodLog usa a de São Paulo (pode virar o dia perto da meia-noite).
BEGIN;

INSERT INTO registro_humor_atividades (registro_humor_id, atividade)
SELECT DISTINCT par.id, a.atividade
FROM registros r
JOIN registro_atividades a ON a.registro_id = r.id
JOIN LATERAL (
    SELECT h.id
    FROM registros_humor h
    WHERE h.user_id = r.usuario_id
      AND abs(extract(epoch FROM h.created_at - r.created_at)) <= 300
    ORDER BY abs(extract(epoch FROM h.created_at - r.created_at))
    LIMIT 1
) par ON true
WHERE NOT EXISTS (
    SELECT 1 FROM registro_humor_atividades x
    WHERE x.registro_humor_id = par.id AND x.atividade = a.atividade
);

-- Conferência: Entries com atividades que não acharam par (as atividades deles se perdem).
SELECT count(DISTINCT r.id) AS entries_sem_par
FROM registros r
JOIN registro_atividades a ON a.registro_id = r.id
WHERE NOT EXISTS (
    SELECT 1 FROM registros_humor h
    WHERE h.user_id = r.usuario_id
      AND abs(extract(epoch FROM h.created_at - r.created_at)) <= 300
);

COMMIT;

-- PASSO B: apaga as tabelas antigas (só depois do backup e da conferência acima).
-- DROP TABLE registro_atividades;
-- DROP TABLE registros;
