-- Visão com as colunas principais diretamente acessíveis
CREATE OR REPLACE VIEW public.vw_processos_datajud AS
SELECT
    p.id,
    p.numero_processo,
    p.grau,
    p.classe,
    p.orgao_julgador,
    p.atualizado_em,
    p.data_ultima_atualizacao,
    p.payload,
    p.payload ->> 'tribunal'                             AS tribunal,
    p.payload ->> '@timestamp'                          AS payload_timestamp,
    (p.payload ->> 'dataHoraUltimaAtualizacao')         AS payload_data_atualizacao,
    p.payload ->> 'dataAjuizamento'                     AS payload_data_ajuizamento,
    p.payload ->> 'id'                                  AS payload_id,
    (p.payload ->> 'nivelSigilo')::int                  AS payload_nivel_sigilo,
    p.payload -> 'classe'  ->> 'codigo'                 AS classe_codigo,
    p.payload -> 'classe'  ->> 'nome'                   AS classe_nome,
    p.payload -> 'sistema' ->> 'codigo'                 AS sistema_codigo,
    p.payload -> 'sistema' ->> 'nome'                   AS sistema_nome,
    p.payload -> 'formato' ->> 'codigo'                 AS formato_codigo,
    p.payload -> 'formato' ->> 'nome'                   AS formato_nome,
    p.payload -> 'orgaoJulgador' ->> 'codigo'           AS orgao_codigo,
    p.payload -> 'orgaoJulgador' ->> 'nome'             AS orgao_nome,
    p.payload -> 'orgaoJulgador' ->> 'codigoMunicipioIBGE' AS orgao_municipio_ibge,
    jsonb_array_length(COALESCE(p.payload -> 'assuntos', '[]'::jsonb))   AS qtde_assuntos,
    jsonb_array_length(COALESCE(p.payload -> 'movimentos', '[]'::jsonb)) AS qtde_movimentos,
    COALESCE((p.payload -> 'movimentos' -> 0) ->> 'dataHora', p.payload ->> 'dataHoraUltimaAtualizacao') AS primeiro_movimento_data
FROM public.processos_datajud p;

-- Consulta padrão utilizando a visão
SELECT *
FROM public.vw_processos_datajud
ORDER BY id;

-- expandindo assuntos em linhas (cada assunto por registro)
SELECT
    p.numero_processo,
    assunto_elem ->> 'codigo' AS assunto_codigo,
    assunto_elem ->> 'nome'   AS assunto_nome
FROM public.processos_datajud p
    CROSS JOIN LATERAL jsonb_array_elements(p.payload -> 'assuntos') AS assunto_elem;

-- expandindo movimentos
SELECT
    p.numero_processo,
    movimento_elem ->> 'codigo'   AS movimento_codigo,
    movimento_elem ->> 'nome'     AS movimento_nome,
    movimento_elem ->> 'dataHora' AS movimento_datahora
FROM public.processos_datajud p
    CROSS JOIN LATERAL jsonb_array_elements(p.payload -> 'movimentos') AS movimento_elem
--WHERE movimento_elem ->> 'nome' = 'Sentença';
