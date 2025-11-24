package br.edu.idp.mcdia.dl.judscraper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

/**
 * Cliente HTTP simples para o endpoint público do Datajud referente ao TJRS.
 * As configurações sensíveis (URL e API Key) são carregadas a partir de um
 * arquivo de propriedades presente no classpath.
 */
public class DatajudClient {

    private static final String PROPERTIES_FILE = "datajud.properties";
    private static final Properties CONFIG = loadProperties();
    private static final String API_URL = requireProperty("datajud.api.url");
    private static final String DEFAULT_API_KEY = System.getenv().getOrDefault(
            "DATAJUD_API_KEY",
            requireProperty("datajud.api.key")
    );
    private static final int MAX_RESULTADOS = 100;

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

    /**
     * Executa uma busca livre no Datajud para o TJRS, retornando o JSON cru
     * enviado pelo serviço. O método usa a API pública oficial do CNJ.
     *
     * @param termoLivre             texto a ser procurado nas sentenças
     * @param quantidadeResultados   quantidade máxima de documentos retornados
     * @return JSON com os metadados dos processos encontrados
     */
    public String buscarSentencasPorTermoLivre(String termoLivre, int quantidadeResultados)
            throws IOException, InterruptedException {

        if (termoLivre == null || termoLivre.isBlank()) {
            throw new IllegalArgumentException("O termo de busca não pode ser vazio.");
        }

        if (quantidadeResultados <= 0 || quantidadeResultados > MAX_RESULTADOS) {
            throw new IllegalArgumentException("A quantidade deve estar entre 1 e " + MAX_RESULTADOS + ".");
        }

        String payload = montarPayloadBuscaLivre(termoLivre, quantidadeResultados);

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
            return response.body();
        }

        throw new IOException("Falha ao consultar o Datajud. HTTP " + statusCode + " - " + response.body());
    }

    private String montarPayloadBuscaLivre(String termoLivre, int quantidadeResultados) {
        String termoSanitizado = termoLivre.replace("\"", "\\\"");
        return "{\n" +
                "  \"size\": " + quantidadeResultados + ",\n" +
                "  \"query\": {\n" +
                "    \"query_string\": {\n" +
                "      \"query\": \"" + termoSanitizado + "\",\n" +
                "      \"default_operator\": \"AND\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"sort\": [\n" +
                "    {\"dataUltimaAtualizacao\": {\"order\": \"desc\"}}\n" +
                "  ]\n" +
                "}\n";
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

        try {
            String resultado = scraper.buscarSentencasPorTermoLivre(termo, limite);
            System.out.println(resultado);
        } catch (Exception e) {
            System.err.println("Erro ao buscar dados processuais: " + e.getMessage());
        }
    }
}
