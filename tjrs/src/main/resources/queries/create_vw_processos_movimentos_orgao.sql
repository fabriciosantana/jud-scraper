CREATE OR REPLACE VIEW public.vw_processos_movimentos_orgao AS
SELECT
    p.numero_processo,
    movimento_elem ->> 'codigo' AS movimento_codigo,
    movimento_elem ->> 'nome'   AS movimento_nome,
    movimento_elem -> 'orgaoJulgador' ->> 'codigoOrgao' AS movimento_orgao_codigo,
    movimento_elem -> 'orgaoJulgador' ->> 'nomeOrgao'   AS movimento_orgao_nome,
    movimento_elem -> 'orgaoJulgador'                   AS movimento_orgao_json
FROM public.processos_datajud p
    CROSS JOIN LATERAL jsonb_array_elements(p.payload -> 'movimentos') AS movimento_elem;
