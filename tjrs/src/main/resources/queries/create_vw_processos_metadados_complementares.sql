CREATE OR REPLACE VIEW public.vw_processos_metadados_complementares AS
SELECT
    p.numero_processo,
    metadado_elem ->> 'chave' AS metadado_chave,
    metadado_elem ->> 'valor' AS metadado_valor,
    metadado_elem            AS metadado_json
FROM public.processos_datajud p
    CROSS JOIN LATERAL jsonb_array_elements(p.payload -> 'metadadosComplementares') AS metadado_elem;
