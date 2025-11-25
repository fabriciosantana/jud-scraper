CREATE OR REPLACE VIEW public.vw_processos_movimentos AS
SELECT
    p.numero_processo,
    movimento_elem ->> 'codigo'   AS movimento_codigo,
    movimento_elem ->> 'nome'     AS movimento_nome,
    movimento_elem ->> 'dataHora' AS movimento_datahora,
    movimento_elem                AS movimento_json
FROM public.processos_datajud p
    CROSS JOIN LATERAL jsonb_array_elements(p.payload -> 'movimentos') AS movimento_elem;
