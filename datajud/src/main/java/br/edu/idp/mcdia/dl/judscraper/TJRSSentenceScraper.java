package br.edu.idp.mcdia.dl.judscraper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;

public class TJRSSentenceScraper implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TJRSSentenceScraper.class);
    private static final Pattern BOTAO_BUSCA_PATTERN = Pattern.compile("Pesquisar|Buscar|Consultar", Pattern.CASE_INSENSITIVE);
    private static final Pattern SENTENCA_PATTERN = Pattern.compile("Senten[cç]a|Inteiro Teor", Pattern.CASE_INSENSITIVE);

    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final String consultaUrl;
    private final Path outputDirectory;

    public TJRSSentenceScraper(String consultaUrl, Path outputDirectory, boolean headless) {
        this.consultaUrl = Objects.requireNonNull(consultaUrl, "consultaUrl");
        this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory");
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar o diretório de sentenças.", e);
        }

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setTimeout(25_000));
        context = browser.newContext(new Browser.NewContextOptions()
                .setLocale("pt-BR")
                .setTimezoneId("America/Sao_Paulo"));
    }

    public Optional<Path> baixarSentenca(String numeroProcesso) {
        try (Page page = context.newPage()) {
            LOGGER.info("Buscando sentença para o processo {}", numeroProcesso);
            page.navigate(consultaUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            preencherNumeroProcesso(page, numeroProcesso);
            acionarBusca(page);

            if (!aguardarResultados(page, numeroProcesso)) {
                LOGGER.warn("Nenhum resultado encontrado para o processo {}.", numeroProcesso);
                return Optional.empty();
            }

            abrirDetalhes(page, numeroProcesso);
            Path arquivo = capturarConteudo(page, numeroProcesso);
            LOGGER.info("Sentença salva em {}", arquivo);
            return Optional.of(arquivo);
        } catch (Exception e) {
            LOGGER.error("Falha ao coletar a sentença do processo {}.", numeroProcesso, e);
            return Optional.empty();
        } 
    }

    private void preencherNumeroProcesso(Page page, String numeroProcesso) {
        Locator campo = localizarCampoProcesso(page);
        if (campo == null) {
            throw new IllegalStateException("Campo para informar o número do processo não encontrado.");
        }
        campo.click();
        campo.fill("");
        campo.fill(numeroProcesso);
    }

    private Locator localizarCampoProcesso(Page page) {
        List<Locator> candidatos = new ArrayList<>();
        candidatos.add(page.locator("input[name='numeroProcesso']"));
        candidatos.add(page.locator("input[name='numeroCNJ']"));
        candidatos.add(page.locator("input[id*='numero']"));
        candidatos.add(page.locator("input[placeholder*='Número']"));
        candidatos.add(page.getByLabel("Número", new Page.GetByLabelOptions().setExact(false)));

        for (Locator locator : candidatos) {
            try {
                if (locator.count() > 0) {
                    return locator.first();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void acionarBusca(Page page) {
        Locator botao = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(BOTAO_BUSCA_PATTERN));
        if (botao.count() == 0) {
            botao = page.locator("button").filter(new Locator.FilterOptions().setHasText("Buscar"));
        }
        if (botao.count() == 0) {
            throw new IllegalStateException("Botão de busca não encontrado no portal do TJRS.");
        }
        botao.first().click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    private boolean aguardarResultados(Page page, String numeroProcesso) {
        try {
            Locator numero = page.getByText(numeroProcesso, new Page.GetByTextOptions().setExact(false));
            numero.waitFor(new Locator.WaitForOptions().setTimeout(15_000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void abrirDetalhes(Page page, String numeroProcesso) {
        List<Locator> opcoes = new ArrayList<>();
        opcoes.add(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(SENTENCA_PATTERN)));
        opcoes.add(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(SENTENCA_PATTERN)));
        opcoes.add(page.getByText("Inteiro Teor", new Page.GetByTextOptions().setExact(false)));
        opcoes.add(page.getByText("Sentença", new Page.GetByTextOptions().setExact(false)));
        opcoes.add(page.getByText(numeroProcesso, new Page.GetByTextOptions().setExact(false)));

        for (Locator opcao : opcoes) {
            try {
                if (opcao.count() == 0) {
                    continue;
                }
                Locator primeiro = opcao.first();
                if (!primeiro.isVisible()) {
                    continue;
                }
                primeiro.click();
                page.waitForTimeout(1000);
                return;
            } catch (Exception ignored) {
            }
        }
    }

    private Path capturarConteudo(Page page, String numeroProcesso) throws IOException {
        Locator sentencaLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(SENTENCA_PATTERN));
        if (sentencaLink.count() > 0) {
            try {
            Download download = page.waitForDownload(() -> sentencaLink.first().click());
                String extension = extrairExtensao(download.suggestedFilename());
                Path destino = outputDirectory.resolve(nomeArquivo(numeroProcesso, extension));
                download.saveAs(destino);
                return destino;
            } catch (Exception e) {
                LOGGER.debug("Download direto não disponível para {}: {}", numeroProcesso, e.getMessage());
            }
        }

        String html = page.content();
        Path destino = outputDirectory.resolve(nomeArquivo(numeroProcesso, ".html"));
        Files.writeString(destino, html, StandardCharsets.UTF_8);
        return destino;
    }

    private String nomeArquivo(String numeroProcesso, String extensao) {
        String sanitized = numeroProcesso.replaceAll("[^0-9]", "");
        if (sanitized.isEmpty()) {
            sanitized = numeroProcesso.replaceAll("\\W+", "_");
        }
        return sanitized + extensao;
    }

    private String extrairExtensao(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.contains(".")) {
            return ".pdf";
        }
        return nomeArquivo.substring(nomeArquivo.lastIndexOf('.'));
    }

    @Override
    public void close() {
        try {
            if (context != null) {
                context.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (browser != null) {
                browser.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (Exception ignored) {
        }
    }
}
