SELECT
    f.numero_processo,
    p.grau,
    s.sistema_codigo,
    s.sistema_nome,
    c.classe_codigo,
    c.classe_nome,
    f.formato_codigo,
    f.formato_nome
FROM public.vw_processos_formato f
JOIN public.vw_processos_datajud p ON p.numero_processo = f.numero_processo
JOIN public.vw_processos_sistema s ON s.numero_processo = f.numero_processo
JOIN public.vw_processos_classe c ON c.numero_processo = f.numero_processo
JOIN public.vw_processos_movimentos m ON m.numero_processo = f.numero_processo
WHERE 1 = 1 
AND f.formato_codigo = '1'
AND p.grau = 'G1'
AND s.sistema_codigo = '4'
AND c.classe_codigo = '7'
AND (m.movimento_codigo = '219' OR m.movimento_codigo = '220');
;
