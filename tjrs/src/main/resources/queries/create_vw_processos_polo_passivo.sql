CREATE OR REPLACE VIEW public.vw_processos_polo_passivo AS
SELECT
    p.numero_processo,
    polo_elem ->> 'tipo'       AS polo_tipo,
    polo_elem ->> 'nome'       AS polo_nome,
    polo_elem ->> 'documento'  AS polo_documento,
    polo_elem                  AS polo_json
FROM public.processos_datajud p
    CROSS JOIN LATERAL jsonb_array_elements(p.payload -> 'poloPassivo') AS polo_elem;
