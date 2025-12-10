select count(*)
from vw_processos_assuntos;

select assunto_nome, count(*)
from vw_processos_assuntos
group by 1
order by 2 desc;

select classe_nome, count(*)
from vw_processos_classe
group by 1
order by 2 desc;

select formato_nome, count(*) 
from vw_processos_formato
group by 1
order by 2 desc;

select movimento_codigo, movimento_nome, count(*) 
from vw_processos_movimentos
group by 1, 2
order by 3 desc;

select count(*) 
from vw_processos_movimentos
;

select movimento_nome, count(*) 
from vw_processos_movimentos_complementos
group by 1
order by 2 desc;

select orgao_nome, count(*) 
from vw_processos_orgao_julgador
group by 1
order by 2 desc;


select sistema_nome, sistema_codigo, count(*) 
from vw_processos_sistema
group by 1, 2
order by 3 desc;

