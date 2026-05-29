-- ==============================================
-- MIGRACIÓN: Normaliza tipo_cita tras reducción del enum TipoCita
-- Los valores DESPARASITACION, REVISION, BANO y CORTE_PELO
-- fueron eliminados del enum; se reasignan a CONSULTA como
-- equivalente histórico genérico.
-- ==============================================

UPDATE cita
SET tipo_cita = 'CONSULTA'
WHERE tipo_cita IN ('DESPARASITACION', 'REVISION', 'BANO', 'CORTE_PELO');
