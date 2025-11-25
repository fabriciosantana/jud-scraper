CREATE OR REPLACE VIEW public.vw_processos_movimentos_complementos AS
SELECT
    p.numero_processo,
    movimento_elem ->> 'codigo'   AS movimento_codigo,
    movimento_elem ->> 'nome'     AS movimento_nome,
    complemento_elem ->> 'codigo' AS complemento_codigo,
    complemento_elem ->> 'valor'  AS complemento_valor,
    complemento_elem ->> 'nome'   AS complemento_nome,
    complemento_elem ->> 'descricao' AS complemento_descricao,
    complemento_elem              AS complemento_json
FROM public.processos_datajud p
    CROSS JOIN LATERAL jsonb_array_elements(p.payload -> 'movimentos') AS movimento_elem
    CROSS JOIN LATERAL jsonb_array_elements(movimento_elem -> 'complementosTabelados') AS complemento_elem;
