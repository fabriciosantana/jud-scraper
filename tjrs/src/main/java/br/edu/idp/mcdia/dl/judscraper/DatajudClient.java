package br.edu.idp.mcdia.dl.judscraper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.idp.mcdia.dl.judscraper.config.AppConfig;
import br.edu.idp.mcdia.dl.judscraper.model.DatajudResponse;
import br.edu.idp.mcdia.dl.judscraper.model.Processo;
import br.edu.idp.mcdia.dl.judscraper.repository.DatajudRepository;
import tools.jackson.databind.ObjectMapper;

public class DatajudClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatajudClient.class);
    private static final AppConfig CONFIG = AppConfig.load();
    private static final String API_URL = CONFIG.require("datajud.api.url");
    private static final String DEFAULT_API_KEY = System.getenv().getOrDefault(
            "DATAJUD_API_KEY",
            CONFIG.require("datajud.api.key")
    );
    private static final String DB_URL = System.getenv().getOrDefault(
            "DATAJUD_DB_URL",
            CONFIG.require("datajud.db.url")
    );
    private static final String DB_USER = System.getenv().getOrDefault(
            "DATAJUD_DB_USER",
            CONFIG.require("datajud.db.user")
    );
    private static final String DB_PASSWORD = System.getenv().getOrDefault(
            "DATAJUD_DB_PASSWORD",
            CONFIG.require("datajud.db.password")
    );
    private static final String DB_TABLE = CONFIG.getOrDefault("datajud.db.table", "processos_datajud");
    private static final String DEFAULT_TERM = CONFIG.getOrDefault("datajud.default.term", "improbidade administrativa");
    private static final int MAX_RESULTADOS = 100;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String apiKey;

    public DatajudClient() {
        this(DEFAULT_API_KEY);
    }

    public DatajudClient(String apiKey) {
        this(apiKey, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    DatajudClient(String apiKey, HttpClient httpClient) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("A API Key do Datajud é obrigatória.");
        }
        this.apiKey = apiKey;
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    public DatajudResponse buscarSentencas(String termoLivre, int tamanhoLote, List<String> searchAfter)
            throws IOException, InterruptedException {

        if (termoLivre == null || termoLivre.isBlank()) {
            throw new IllegalArgumentException("O termo de busca não pode ser vazio.");
        }

        if (tamanhoLote <= 0 || tamanhoLote > MAX_RESULTADOS) {
            throw new IllegalArgumentException("O tamanho do lote deve estar entre 1 e " + MAX_RESULTADOS + ".");
        }

        String payload = montarPayloadBuscaLivre(termoLivre, tamanhoLote, searchAfter);
        LOGGER.info("Consultando Datajud (size={}) termo '{}' cursor={}", tamanhoLote, termoLivre, searchAfter);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "APIKey " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();

        if (statusCode >= 200 && statusCode < 300) {
            LOGGER.info("Consulta ao Datajud concluída com status {}.", statusCode);
            return OBJECT_MAPPER.readValue(response.body(), DatajudResponse.class);
        }

        LOGGER.error("Falha na consulta ao Datajud. HTTP {} - {}", statusCode, response.body());
        throw new IOException("Falha ao consultar o Datajud. HTTP " + statusCode + " - " + response.body());
    }

    private String montarPayloadBuscaLivre(String termoLivre, int quantidadeResultados, List<String> searchAfter) {
        String termoSanitizado = termoLivre.replace("\"", "\\\"");
        String payloadTemplate = """
                {
                  "size": %d,
                  "query": {
                    "query_string": {
                      "query": "%s",
                      "default_operator": "AND"
                    }
                  },
                  "sort": [
                    {"dataHoraUltimaAtualizacao": {"order": "asc", "unmapped_type": "date"}},
                    {"numeroProcesso.keyword": {"order": "asc"}}
                  ]""";

        StringBuilder builder = new StringBuilder(String.format(payloadTemplate, quantidadeResultados, termoSanitizado));

        if (searchAfter != null && !searchAfter.isEmpty()) {
            builder.append(",\n  \"search_after\": [");
            for (int i = 0; i < searchAfter.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append("\"").append(searchAfter.get(i).replace("\"", "\\\"")).append("\"");
            }
            builder.append("]");
        }

        builder.append("\n}\n");
        return builder.toString();
    }

    public static void main(String[] args) {
        boolean resetCursor = false;
        boolean resetFull = false;
        Integer totalRegistros = null;

        for (String arg : args) {
            if ("--reset-full".equalsIgnoreCase(arg)) {
                resetFull = true;
                resetCursor = true;
            } else if ("--reset".equalsIgnoreCase(arg)) {
                resetCursor = true;
            } else if (totalRegistros == null) {
                totalRegistros = Integer.parseInt(arg);
            }
        }

        if (totalRegistros == null || totalRegistros <= 0) {
            System.err.println("A quantidade deve ser maior que zero.");
            return;
        }

        DatajudClient scraper = new DatajudClient();

        LOGGER.info("Iniciando coleta do Datajud: termo='{}' | quantidade solicitada={}", DEFAULT_TERM, totalRegistros);
        long inicioExecucao = System.nanoTime();
        long tempoApiAcumulado = 0L;
        int chamadasApi = 0;

        try (DatajudRepository repository = new DatajudRepository(DB_URL, DB_USER, DB_PASSWORD, DB_TABLE, OBJECT_MAPPER)) {
            if (resetFull) {
                repository.truncarProcessos();
            }
            if (resetCursor) {
                repository.truncarCursor();
            }
            int processados = 0;
            List<String> cursor = resetCursor ? Collections.emptyList() : repository.carregarCursor();
            while (processados < totalRegistros) {
                int tamanhoLote = Math.min(MAX_RESULTADOS, totalRegistros - processados);
                long inicioChamada = System.nanoTime();
                DatajudResponse resultado = scraper.buscarSentencas(DEFAULT_TERM, tamanhoLote, cursor);
                long duracaoChamada = System.nanoTime() - inicioChamada;
                tempoApiAcumulado += duracaoChamada;
                chamadasApi++;
                LOGGER.info("Chamada #{} retornou {} registros em {} ms",
                        chamadasApi,
                        resultado != null && resultado.hits() != null ? resultado.hits().hits().size() : 0,
                        Duration.ofNanos(duracaoChamada).toMillis());

                imprimirResumo(resultado);
                boolean persistiu = salvarProcessosNoBanco(repository, resultado);
                if (!persistiu || resultado == null || resultado.hits() == null
                        || resultado.hits().hits() == null || resultado.hits().hits().isEmpty()) {
                    LOGGER.info("Nenhum resultado retornado neste lote; encerrando paginação.");
                    break;
                }
                processados += resultado.hits().hits().size();
                cursor = extrairCursor(resultado);
                if (!cursor.isEmpty()) {
                    repository.salvarCursor(cursor);
                }
            }
            LOGGER.info("Resumo da execução: processos persistidos={} | chamadas_api={} | tempo_api_acumulado={} ms | duração_total={} ms",
                    processados,
                    chamadasApi,
                    Duration.ofNanos(tempoApiAcumulado).toMillis(),
                    Duration.ofNanos(System.nanoTime() - inicioExecucao).toMillis());
        } catch (Exception e) {
            LOGGER.error("Erro ao buscar dados processuais.", e);
            System.err.println("Erro ao buscar dados processuais: " + e.getMessage());
        }
    }

    private static void imprimirResumo(DatajudResponse response) {
        if (response == null || response.hits() == null || response.hits().hits() == null) {
            System.out.println("Nenhum resultado retornado pelo Datajud.");
            LOGGER.info("Nenhum resultado retornado pelo Datajud.");
            System.out.flush();
            return;
        }

        List<DatajudResponse.ProcessHit> hits = response.hits().hits();
        System.out.printf("Total de resultados retornados: %d%n", hits.size());
        LOGGER.info("Total de resultados retornados: {}", hits.size());
        for (DatajudResponse.ProcessHit hit : hits) {
            Processo processo = hit.processo();
            if (processo == null) {
                continue;
            }
            String classe = processo.classe() != null ? processo.classe().nome() : "Classe não informada";
            String orgao = processo.orgaoJulgador() != null ? processo.orgaoJulgador().nome() : "Órgão não informado";
            System.out.printf("Processo %s | Classe: %s | Órgão julgador: %s%n",
                    processo.numeroProcesso(),
                    classe,
                    orgao);
            LOGGER.info("Processo {} | Classe: {} | Órgão julgador: {}", processo.numeroProcesso(), classe, orgao);
        }
        System.out.flush();
    }

    private static boolean salvarProcessosNoBanco(DatajudRepository repository, DatajudResponse response) {
        List<Processo> processos = extrairProcessos(response);
        if (processos.isEmpty()) {
            System.out.println("Nenhum processo para persistir no banco.");
            return false;
        }

        try {
            repository.salvarProcessos(processos);
            System.out.printf("%d processos persistidos na tabela %s.%n", processos.size(), DB_TABLE);
            LOGGER.info("{} processos persistidos na tabela {}.", processos.size(), DB_TABLE);
            return true;
        } catch (Exception e) {
            LOGGER.error("Falha ao persistir processos no banco.", e);
            System.err.println("Falha ao persistir processos no banco: " + e.getMessage());
            return false;
        }
    }

    private static List<Processo> extrairProcessos(DatajudResponse response) {
        if (response == null || response.hits() == null || response.hits().hits() == null) {
            return Collections.emptyList();
        }
        List<Processo> processos = new ArrayList<>();
        for (DatajudResponse.ProcessHit hit : response.hits().hits()) {
            if (hit != null && hit.processo() != null) {
                processos.add(hit.processo());
            }
        }
        return processos;
    }

    private static List<String> extrairCursor(DatajudResponse response) {
        if (response == null || response.hits() == null || response.hits().hits() == null
                || response.hits().hits().isEmpty()) {
            return Collections.emptyList();
        }
        DatajudResponse.ProcessHit ultimo = response.hits().hits()
                .get(response.hits().hits().size() - 1);
        if (ultimo.sort() == null || ultimo.sort().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> cursor = new ArrayList<>();
        for (Object sortValue : ultimo.sort()) {
            if (sortValue == null) {
                cursor.add("");
            } else if (sortValue instanceof String) {
                cursor.add((String) sortValue);
            } else {
                cursor.add(sortValue.toString());
            }
        }
        return cursor;
    }
}
