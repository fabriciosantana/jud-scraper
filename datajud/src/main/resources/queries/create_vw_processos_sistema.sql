CREATE OR REPLACE VIEW public.vw_processos_sistema AS
SELECT
    p.numero_processo,
    p.payload -> 'sistema' ->> 'codigo' AS sistema_codigo,
    p.payload -> 'sistema' ->> 'nome'   AS sistema_nome,
    p.payload -> 'sistema'              AS sistema_json
FROM public.processos_datajud p;
