package br.edu.idp.mcdia.dl.judscraper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/**
 * Cliente HTTP simples para o endpoint público do Datajud referente ao TJRS.
 * A classe encapsula a autenticação via API Key e o payload para buscas livres
 * no banco de sentenças do tribunal.
 */
public class TJRSBancoSentencasScraper {

    private static final String API_URL = "https://api-publica.datajud.cnj.jus.br/api_publica_tjrs/_search";
    private static final String DEFAULT_API_KEY = "cDZHYzlZa0JadVREZDJCendQbXY6SkJlTzNjLV9TRENyQk1RdnFKZGRQdw==";
    private static final int MAX_RESULTADOS = 100;

    private final HttpClient httpClient;
    private final String apiKey;

    public TJRSBancoSentencasScraper() {
        this(System.getenv().getOrDefault("DATAJUD_API_KEY", DEFAULT_API_KEY));
    }

    public TJRSBancoSentencasScraper(String apiKey) {
        this(apiKey, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    TJRSBancoSentencasScraper(String apiKey, HttpClient httpClient) {
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
     * @param termoLivre        texto a ser procurado nas sentenças
     * @param quantidadeResultados quantidade máxima de documentos retornados
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

    public static void main(String[] args) {
        TJRSBancoSentencasScraper scraper = new TJRSBancoSentencasScraper();
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
