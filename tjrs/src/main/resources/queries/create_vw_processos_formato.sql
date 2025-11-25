CREATE OR REPLACE VIEW public.vw_processos_formato AS
SELECT
    p.numero_processo,
    p.payload -> 'formato' ->> 'codigo' AS formato_codigo,
    p.payload -> 'formato' ->> 'nome'   AS formato_nome,
    p.payload -> 'formato'              AS formato_json
FROM public.processos_datajud p;
