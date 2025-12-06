SELECT
    p.id,
    p.numero_processo,
    p.grau,
    p.atualizado_em,
    p.sentenca_caminho,
    p.sentenca_salva_em,
    p.payload ->> 'tribunal'                  AS tribunal,
    p.payload ->> '@timestamp'               AS payload_timestamp,
    p.payload ->> 'dataHoraUltimaAtualizacao' AS data_ultima_atualizacao,
    p.payload ->> 'dataAjuizamento'          AS data_ajuizamento,
    p.payload -> 'classe'  ->> 'codigo'      AS classe_codigo,
    p.payload -> 'classe'  ->> 'nome'        AS classe_nome,
    p.payload -> 'sistema' ->> 'codigo'      AS sistema_codigo,
    p.payload -> 'sistema' ->> 'nome'        AS sistema_nome,
    p.payload -> 'formato' ->> 'codigo'      AS formato_codigo,
    p.payload -> 'formato' ->> 'nome'        AS formato_nome,
    p.payload -> 'orgaoJulgador' ->> 'codigo' AS orgao_codigo,
    p.payload -> 'orgaoJulgador' ->> 'nome'   AS orgao_nome,
    p.payload -> 'orgaoJulgador' ->> 'codigoMunicipioIBGE' AS orgao_municipio_ibge,
    assunto_elem ->> 'codigo'                AS assunto_codigo,
    assunto_elem ->> 'nome'                  AS assunto_nome,
    movimento_elem ->> 'codigo'              AS movimento_codigo,
    movimento_elem ->> 'nome'                AS movimento_nome,
    movimento_elem ->> 'dataHora'            AS movimento_datahora,
    movimento_elem -> 'orgaoJulgador' ->> 'codigoOrgao' AS mov_orgao_codigo,
    movimento_elem -> 'orgaoJulgador' ->> 'nomeOrgao'   AS mov_orgao_nome,
    complemento_elem ->> 'codigo'            AS complemento_codigo,
    complemento_elem ->> 'descricao'         AS complemento_descricao,
    complemento_elem ->> 'valor'             AS complemento_valor
FROM public.processos_datajud p
LEFT JOIN LATERAL jsonb_array_elements(
    COALESCE(p.payload -> 'assuntos', '[]'::jsonb)
) AS assunto_elem ON true
LEFT JOIN LATERAL jsonb_array_elements(
    COALESCE(p.payload -> 'movimentos', '[]'::jsonb)
) AS movimento_elem ON true
LEFT JOIN LATERAL jsonb_array_elements(
    CASE
        WHEN jsonb_typeof(movimento_elem -> 'complementosTabelados') = 'array'
            THEN movimento_elem -> 'complementosTabelados'
        ELSE '[]'::jsonb
    END
) AS complemento_elem ON true;
