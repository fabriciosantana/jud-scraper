package br.edu.idp.mcdia.dl.judscraper;

import br.edu.idp.mcdia.dl.judscraper.model.DatajudResponse;
import br.edu.idp.mcdia.dl.judscraper.model.Processo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Cliente HTTP simples para o endpoint público do Datajud referente ao TJRS.
 * As configurações e credenciais são carregadas a partir de um arquivo de propriedades
 * presente no classpath e podem ser sobrescritas por variáveis de ambiente.
 */
public class DatajudClient {

    private static final String PROPERTIES_FILE = "datajud.properties";
    private static final Logger LOGGER = LoggerFactory.getLogger(DatajudClient.class);
    private static final Properties CONFIG = loadProperties();
    private static final String API_URL = requireProperty("datajud.api.url");
    private static final String DEFAULT_API_KEY = System.getenv().getOrDefault(
            "DATAJUD_API_KEY",
            requireProperty("datajud.api.key")
    );
    private static final String DB_URL = System.getenv().getOrDefault(
            "DATAJUD_DB_URL",
            requireProperty("datajud.db.url")
    );
    private static final String DB_USER = System.getenv().getOrDefault(
            "DATAJUD_DB_USER",
            requireProperty("datajud.db.user")
    );
    private static final String DB_PASSWORD = System.getenv().getOrDefault(
            "DATAJUD_DB_PASSWORD",
            requireProperty("datajud.db.password")
    );
    private static final String DB_TABLE = CONFIG.getProperty("datajud.db.table", "processos_datajud").trim();
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

    public DatajudResponse buscarSentencas(String termoLivre, int tamanhoLote, int inicio, OffsetDateTime atualizadoApos)
            throws IOException, InterruptedException {

        if (termoLivre == null || termoLivre.isBlank()) {
            throw new IllegalArgumentException("O termo de busca não pode ser vazio.");
        }

        if (tamanhoLote <= 0 || tamanhoLote > MAX_RESULTADOS) {
            throw new IllegalArgumentException("O tamanho do lote deve estar entre 1 e " + MAX_RESULTADOS + ".");
        }

        String payload = montarPayloadBuscaLivre(termoLivre, tamanhoLote, inicio, atualizadoApos);
        LOGGER.info("Consultando Datajud (from={}, size={}) termo '{}' {}", inicio, tamanhoLote, termoLivre,
                atualizadoApos != null ? "após " + atualizadoApos : "");

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

    private String montarPayloadBuscaLivre(String termoLivre, int quantidadeResultados, int inicio, OffsetDateTime atualizadoApos) {
        String termoSanitizado = termoLivre.replace("\"", "\\\"");
        StringBuilder builder = new StringBuilder();
        builder.append("{\n")
                .append("  \"size\": ").append(quantidadeResultados).append(",\n")
                .append("  \"from\": ").append(inicio).append(",\n")
                .append("  \"query\": {\n");

        if (atualizadoApos != null) {
            builder.append("    \"bool\": {\n")
                    .append("      \"must\": {\n")
                    .append("        \"query_string\": {\n")
                    .append("          \"query\": \"").append(termoSanitizado).append("\",\n")
                    .append("          \"default_operator\": \"AND\"\n")
                    .append("        }\n")
                    .append("      },\n")
                    .append("      \"filter\": [\n")
                    .append("        {\n")
                    .append("          \"range\": {\n")
                    .append("            \"dataHoraUltimaAtualizacao\": {\n")
                    .append("              \"gt\": \"").append(atualizadoApos).append("\"\n")
                    .append("            }\n")
                    .append("          }\n")
                    .append("        }\n")
                    .append("      ]\n")
                    .append("    }\n");
        } else {
            builder.append("    \"query_string\": {\n")
                    .append("      \"query\": \"").append(termoSanitizado).append("\",\n")
                    .append("      \"default_operator\": \"AND\"\n")
                    .append("    }\n");
        }

        builder.append("  }\n")
                .append("}\n");
        return builder.toString();
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        InputStream resource = DatajudClient.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
        if (resource == null) {
            throw new IllegalStateException("Arquivo de propriedades '" + PROPERTIES_FILE + "' não encontrado.");
        }
        try (InputStream input = resource) {
            props.load(input);
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao carregar as configurações do Datajud.", e);
        }
    }

    private static String requireProperty(String key) {
        String value = CONFIG.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Propriedade obrigatória ausente: " + key);
        }
        return value.trim();
    }

    public static void main(String[] args) {
        DatajudClient scraper = new DatajudClient();
        String termo = args.length > 0 ? args[0] : "improbidade administrativa";
        int limite = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        int offsetInicial = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        boolean cargaCompleta = args.length > 3 && Boolean.parseBoolean(args[3]);

        try (DatajudRepository repository = new DatajudRepository(DB_URL, DB_USER, DB_PASSWORD, DB_TABLE, OBJECT_MAPPER)) {
            OffsetDateTime ultimaAtualizacao = cargaCompleta ? null : ajustarJanelaTemporal(repository.buscarUltimaDataAtualizacao());
            int restante = limite;
            int offset = Math.max(0, offsetInicial);
            while (restante > 0) {
                int tamanhoLote = Math.min(MAX_RESULTADOS, restante);
                DatajudResponse resultado = scraper.buscarSentencas(termo, tamanhoLote, offset, ultimaAtualizacao);
                imprimirResumo(resultado, ultimaAtualizacao);
                boolean persistiu = salvarProcessosNoBanco(repository, resultado);
                if (!persistiu || resultado.getHits() == null || resultado.getHits().getHits() == null
                        || resultado.getHits().getHits().isEmpty()) {
                    LOGGER.info("Nenhum resultado retornado neste lote; encerrando paginação.");
                    break;
                }
                restante -= tamanhoLote;
                offset += tamanhoLote;
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao buscar dados processuais.", e);
            System.err.println("Erro ao buscar dados processuais: " + e.getMessage());
        }
    }

    private static void imprimirResumo(DatajudResponse response, OffsetDateTime filtroAtualizacao) {
        if (response == null || response.getHits() == null || response.getHits().getHits() == null) {
            System.out.println("Nenhum resultado retornado pelo Datajud.");
            LOGGER.info("Nenhum resultado retornado pelo Datajud.");
            System.out.flush();
            return;
        }

        List<DatajudResponse.ProcessHit> hits = response.getHits().getHits();
        System.out.printf("Total de resultados retornados: %d%n", hits.size());
        if (filtroAtualizacao != null) {
            System.out.printf("Filtrados para atualizações posteriores a %s.%n", filtroAtualizacao);
            LOGGER.info("Total de resultados retornados: {} (após {}).", hits.size(), filtroAtualizacao);
        } else {
            LOGGER.info("Total de resultados retornados: {}", hits.size());
        }
        for (DatajudResponse.ProcessHit hit : hits) {
            Processo processo = hit.getProcesso();
            if (processo == null) {
                continue;
            }
            String classe = processo.getClasse() != null ? processo.getClasse().getNome() : "Classe não informada";
            String orgao = processo.getOrgaoJulgador() != null ? processo.getOrgaoJulgador().getNome() : "Órgão não informado";
            System.out.printf("Processo %s | Classe: %s | Órgão julgador: %s%n",
                    processo.getNumeroProcesso(),
                    classe,
                    orgao);
            LOGGER.info("Processo {} | Classe: {} | Órgão julgador: {}", processo.getNumeroProcesso(), classe, orgao);
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
        if (response == null || response.getHits() == null || response.getHits().getHits() == null) {
            return Collections.emptyList();
        }
        List<Processo> processos = new ArrayList<>();
        for (DatajudResponse.ProcessHit hit : response.getHits().getHits()) {
            if (hit != null && hit.getProcesso() != null) {
                processos.add(hit.getProcesso());
            }
        }
        return processos;
    }

    private static OffsetDateTime ajustarJanelaTemporal(OffsetDateTime ultimaAtualizacao) {
        if (ultimaAtualizacao == null) {
            return null;
        }
        OffsetDateTime agoraUtc = OffsetDateTime.now(ZoneOffset.UTC);
        if (ultimaAtualizacao.isAfter(agoraUtc)) {
            OffsetDateTime ajustada = agoraUtc.minusMinutes(1);
            LOGGER.warn("Última data de atualização ({}) está no futuro. Ajustando janela para {}.", ultimaAtualizacao, ajustada);
            return ajustada;
        }
        return ultimaAtualizacao;
    }
}
