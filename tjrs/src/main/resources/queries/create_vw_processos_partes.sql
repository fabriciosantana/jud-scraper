CREATE OR REPLACE VIEW public.vw_processos_partes AS
SELECT
    p.numero_processo,
    parte_elem ->> 'tipo'       AS parte_tipo,
    parte_elem ->> 'nome'       AS parte_nome,
    parte_elem ->> 'documento'  AS parte_documento,
    parte_elem                  AS parte_json
FROM public.processos_datajud p
    CROSS JOIN LATERAL jsonb_array_elements(p.payload -> 'partes') AS parte_elem;
