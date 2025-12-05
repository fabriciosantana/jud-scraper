CREATE OR REPLACE VIEW public.vw_processos_classe AS
SELECT
    p.numero_processo,
    p.payload -> 'classe' ->> 'codigo' AS classe_codigo,
    p.payload -> 'classe' ->> 'nome'   AS classe_nome,
    p.payload -> 'classe'              AS classe_json
FROM public.processos_datajud p;
