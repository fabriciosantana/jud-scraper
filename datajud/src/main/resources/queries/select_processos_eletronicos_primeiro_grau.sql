SELECT
    f.numero_processo,
    p.grau,
    m.movimento_codigo,
    m.movimento_nome,
    m.movimento_datahora,
    s.sistema_codigo,
    s.sistema_nome,
    c.classe_codigo,
    c.classe_nome,
    a.assunto_codigo,
    a.assunto_nome,
    f.formato_codigo,
    f.formato_nome
FROM public.vw_processos_formato f
JOIN public.vw_processos_datajud p ON p.numero_processo = f.numero_processo
JOIN public.vw_processos_sistema s ON s.numero_processo = f.numero_processo
JOIN public.vw_processos_classe c ON c.numero_processo = f.numero_processo
JOIN public.vw_processos_movimentos m ON m.numero_processo = f.numero_processo
JOIN public.vw_processos_orgao_julgador o on o.numero_processo = f.numero_processo
JOIN public.vw_processos_assuntos a on o.numero_processo = a.numero_processo
WHERE 1 = 1 
AND f.formato_codigo = '1' -- Eletrônico
--AND p.grau = 'G1' -- 1º Grau
AND s.sistema_codigo = '1' -- PJE
--AND c.classe_codigo = '' -- A DEFINIR
AND a.assunto_codigo = '10433'
AND (m.movimento_codigo = '12200' OR m.movimento_codigo = '848' OR m.movimento_codigo = '239' OR m.movimento_codigo = '237' OR m.movimento_codigo = '238') -- 12200 = Mérito 848 = Trânsito em Julgado  239 = Não Provimento 237 = Provimento 238 = Provimento em parte
order by f.numero_processo, m.movimento_datahora desc
;


SELECT
    
    --c.classe_nome,
    a.assunto_codigo,
    a.assunto_nome,
    count(*)
    
FROM public.vw_processos_formato f
JOIN public.vw_processos_datajud p ON p.numero_processo = f.numero_processo
JOIN public.vw_processos_sistema s ON s.numero_processo = f.numero_processo
JOIN public.vw_processos_classe c ON c.numero_processo = f.numero_processo
JOIN public.vw_processos_movimentos m ON m.numero_processo = f.numero_processo
JOIN public.vw_processos_orgao_julgador o on o.numero_processo = f.numero_processo
JOIN public.vw_processos_assuntos a on o.numero_processo = a.numero_processo
WHERE 1 = 1 
AND f.formato_codigo = '1' -- Eletrônico
--AND p.grau = 'G1' -- 1º Grau
AND s.sistema_codigo = '1' -- PJE
--AND c.classe_codigo = '' -- A DEFINIR

AND (m.movimento_codigo = '12200' OR m.movimento_codigo = '848' OR m.movimento_codigo = '239' OR m.movimento_codigo = '237' OR m.movimento_codigo = '238') -- 12200 = Mérito 848 = Trânsito em Julgado  239 = Não Provimento 237 = Provimento 238 = Provimento em parte
group by 1, 2
order by 3 desc
;
