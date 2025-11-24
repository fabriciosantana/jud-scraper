package br.edu.idp.mcdia.dl.judscraper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public final class AppConfig {

    private static final String PROPERTIES_FILE = "datajud.properties";
    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = properties;
    }

    public static AppConfig load() {
        Properties props = new Properties();
        InputStream resource = AppConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
        if (resource == null) {
            throw new IllegalStateException("Arquivo de propriedades '" + PROPERTIES_FILE + "' não encontrado.");
        }
        try (InputStream input = resource) {
            props.load(input);
            return new AppConfig(props);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao carregar configurações.", e);
        }
    }

    public String require(String key) {
        String value = properties.getProperty(Objects.requireNonNull(key, "key"));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Propriedade obrigatória ausente: " + key);
        }
        return value.trim();
    }

    public String getOrDefault(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null ? defaultValue : value.trim();
    }

    public Properties asProperties() {
        return properties;
    }
}
