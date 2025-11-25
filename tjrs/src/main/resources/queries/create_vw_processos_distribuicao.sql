CREATE OR REPLACE VIEW public.vw_processos_distribuicao AS
SELECT
    p.numero_processo,
    p.payload -> 'distribuicao' ->> 'data'            AS distribuicao_data,
    p.payload -> 'distribuicao' ->> 'tipo'            AS distribuicao_tipo,
    p.payload -> 'distribuicao' ->> 'comarca'         AS distribuicao_comarca,
    p.payload -> 'distribuicao' ->> 'foro'            AS distribuicao_foro,
    p.payload -> 'distribuicao'                       AS distribuicao_json
FROM public.processos_datajud p;
