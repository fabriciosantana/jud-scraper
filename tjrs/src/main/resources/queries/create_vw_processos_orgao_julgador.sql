CREATE OR REPLACE VIEW public.vw_processos_orgao_julgador AS
SELECT
    p.numero_processo,
    p.payload -> 'orgaoJulgador' ->> 'codigoMunicipioIBGE' AS orgao_municipio_ibge,
    p.payload -> 'orgaoJulgador' ->> 'codigo'              AS orgao_codigo,
    p.payload -> 'orgaoJulgador' ->> 'nome'                AS orgao_nome,
    p.payload -> 'orgaoJulgador'                           AS orgao_julgador_json
FROM public.processos_datajud p;
