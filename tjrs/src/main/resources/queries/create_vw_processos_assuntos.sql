CREATE OR REPLACE VIEW public.vw_processos_assuntos AS
SELECT
    p.numero_processo,
    assunto_elem ->> 'codigo' AS assunto_codigo,
    assunto_elem ->> 'nome'   AS assunto_nome,
    assunto_elem             AS assunto_json
FROM public.processos_datajud p
    CROSS JOIN LATERAL jsonb_array_elements(p.payload -> 'assuntos') AS assunto_elem;
