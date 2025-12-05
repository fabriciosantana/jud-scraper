package br.edu.idp.mcdia.dl.judscraper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.idp.mcdia.dl.judscraper.config.AppConfig;
import br.edu.idp.mcdia.dl.judscraper.repository.DatajudRepository;

public final class SentencaDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentencaDownloader.class);

    private SentencaDownloader() {
    }

    public static void main(String[] args) {
        AppConfig config = AppConfig.load();

        int limite = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        int offset = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        boolean apenasPendentes = args.length > 2 ? Boolean.parseBoolean(args[2]) : true;

        String dbUrl = System.getenv().getOrDefault("DATAJUD_DB_URL", config.require("datajud.db.url"));
        String dbUser = System.getenv().getOrDefault("DATAJUD_DB_USER", config.require("datajud.db.user"));
        String dbPassword = System.getenv().getOrDefault("DATAJUD_DB_PASSWORD", config.require("datajud.db.password"));
        String tabela = config.getOrDefault("datajud.db.table", "processos_datajud");

        String consultaUrl = config.require("tjrs.consulta.url");
        Path destino = Paths.get(config.getOrDefault("tjrs.sentencas.dir", "dados/sentencas"));
        boolean headless = Boolean.parseBoolean(config.getOrDefault("tjrs.sentencas.headless", "true"));

        try (DatajudRepository repository = new DatajudRepository(dbUrl, dbUser, dbPassword, tabela, new tools.jackson.databind.ObjectMapper());
             TJRSSentenceScraper scraper = new TJRSSentenceScraper(consultaUrl, destino, headless)) {

            List<String> processos = repository.listarProcessosParaSentenca(limite, offset, apenasPendentes);
            LOGGER.info("Processos selecionados para download das sentenças: {}", processos.size());
            for (String numero : processos) {
                Optional<Path> arquivo = scraper.baixarSentenca(numero);
                if (arquivo.isPresent()) {
                    repository.registrarSentenca(numero, arquivo.get());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Falha ao executar o download das sentenças.", e);
            System.err.println("Erro ao executar o download das sentenças: " + e.getMessage());
        }
    }
}
