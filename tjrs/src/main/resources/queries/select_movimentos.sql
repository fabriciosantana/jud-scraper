-- expandindo movimentos
SELECT
    p.numero_processo,
    movimento_elem ->> 'codigo'   AS movimento_codigo,
    movimento_elem ->> 'nome'     AS movimento_nome,
    movimento_elem ->> 'dataHora' AS movimento_datahora
FROM public.processos_datajud p
    CROSS JOIN LATERAL jsonb_array_elements(p.payload -> 'movimentos') AS movimento_elem;