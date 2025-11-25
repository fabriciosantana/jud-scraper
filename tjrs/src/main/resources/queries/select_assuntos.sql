-- expandindo assuntos em linhas (cada assunto por registro)
SELECT
    p.numero_processo,
    assunto_elem ->> 'codigo' AS assunto_codigo,
    assunto_elem ->> 'nome'   AS assunto_nome
FROM public.processos_datajud p
    CROSS JOIN LATERAL jsonb_array_elements(p.payload -> 'assuntos') AS assunto_elem;

