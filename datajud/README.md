# DataJUD Client

Cliente Java para coletar processos no DataJUD/CNJ e persistir os resultados em um banco PostgreSQL.

## Pré-requisitos
- Java 21 instalado (`java -version` deve apontar para 21.x).
- Maven 3.9 ou superior para compilar/empacotar o projeto.
- Acesso à API pública do DataJUD e a um banco PostgreSQL acessível a partir da máquina de execução.

## Configuração
1. Copie o arquivo de exemplo e preencha as credenciais:
   ```bash
   cp src/main/resources/datajud.properties.sample src/main/resources/datajud.properties
   ```
   Campos obrigatórios:
   - `datajud.api.url` e `datajud.api.key`
   - `datajud.db.url`, `datajud.db.user`, `datajud.db.password`
   - Opcionalmente `datajud.db.table` (padrão `processos_datajud`) e `datajud.default.term`
2. É possível sobrescrever qualquer propriedade via variáveis de ambiente:
   - `DATAJUD_API_KEY`, `DATAJUD_DB_URL`, `DATAJUD_DB_USER`, `DATAJUD_DB_PASSWORD`
3. Certifique-se de que o arquivo `datajud.properties` permaneça no classpath (em `src/main/resources`) antes de construir o projeto.

## Formas de Execução
O `DatajudClient` exige um argumento obrigatório com a quantidade de processos desejada e aceita os flags opcionais `--reset` (limpa apenas o cursor) e `--reset-full` (limpa cursor e tabela de processos).

### 1. Maven Exec Plugin (modo desenvolvimento)
Executa sem criar um artefato final, utilizando o classpath montado pelo Maven:
```bash
mvn exec:java \
  -Dexec.mainClass=br.edu.idp.mcdia.dl.judscraper.DatajudClient \
  -Dexec.args="1000 --reset"
```
- Substitua `1000` pela quantidade a ser coletada.
- Inclua `--reset` e/ou `--reset-full` quando precisar reiniciar a ingestão.

### 2. JAR + Java CLI (modo produção)
Gera o artefato e executa com o `java` padrão, utilizando as dependências copiadas para `target/dependency`:
```bash
mvn clean package dependency:copy-dependencies
java -cp target/datajud-client-1.0-SNAPSHOT.jar:target/dependency/* \
  br.edu.idp.mcdia.dl.judscraper.DatajudClient \
  1000 --reset-full
```
- `dependency:copy-dependencies` garante que todas as bibliotecas estejam em `target/dependency`.
- No comando `java`, ajuste a quantidade e os flags conforme necessário.

Em ambos os casos os logs seguirão a configuração do `logback.xml` e serão emitidos no console (e em `logs/` caso configurado).

