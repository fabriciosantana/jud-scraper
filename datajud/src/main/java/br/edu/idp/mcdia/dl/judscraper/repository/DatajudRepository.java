package br.edu.idp.mcdia.dl.judscraper.repository;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.idp.mcdia.dl.judscraper.model.Processo;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class DatajudRepository implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatajudRepository.class);
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final Connection connection;
    private final String tableName;
    private final ObjectMapper objectMapper;
    private final String cursorTable;

    public DatajudRepository(String url, String user, String password, String tableName, ObjectMapper objectMapper) throws SQLException {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL do banco não pode ser vazia.");
        }
        LOGGER.info("Conectando ao banco PostgreSQL em {}", url);
        this.connection = DriverManager.getConnection(url, user, password);
        this.tableName = sanitizeTableName(tableName);
        this.objectMapper = objectMapper;
        this.cursorTable = this.tableName + "_cursor";
        criarTabelaSeNecessario();
    }

    private String sanitizeTableName(String provided) {
        String name = (provided == null || provided.isBlank()) ? "processos_datajud" : provided.trim();
        if (!TABLE_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Nome de tabela inválido: " + name);
        }
        return name;
    }

    private void criarTabelaSeNecessario() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id BIGSERIAL PRIMARY KEY," +
                "numero_processo TEXT NOT NULL," +
                "grau TEXT," +
                "classe TEXT," +
                "orgao_julgador TEXT," +
                "payload JSONB NOT NULL," +
                "sentenca_caminho TEXT," +
                "sentenca_salva_em TIMESTAMPTZ," +
                "atualizado_em TIMESTAMPTZ NOT NULL DEFAULT NOW()," +
                "CONSTRAINT uq_numero_processo UNIQUE (numero_processo)" +
                ")";
        String cursorSql = "CREATE TABLE IF NOT EXISTS " + cursorTable + " (" +
                "id SMALLINT PRIMARY KEY DEFAULT 1," +
                "cursor JSONB," +
                "atualizado_em TIMESTAMPTZ NOT NULL DEFAULT NOW()" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS sentenca_caminho TEXT");
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS sentenca_salva_em TIMESTAMPTZ");
            stmt.execute(cursorSql);
            LOGGER.info("Tabela {} e controle de cursor disponíveis para persistência.", tableName);
        }
    }

    public List<String> listarProcessosParaSentenca(int limit, int offset, boolean apenasPendentes) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT numero_processo FROM " + tableName);
        if (apenasPendentes) {
            sql.append(" WHERE sentenca_caminho IS NULL");
        }
        sql.append(" ORDER BY id LIMIT ? OFFSET ?");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            ResultSet rs = ps.executeQuery();
            List<String> numeros = new java.util.ArrayList<>();
            while (rs.next()) {
                numeros.add(rs.getString(1));
            }
            return numeros;
        }
    }

    public void registrarSentenca(String numeroProcesso, Path arquivo) throws SQLException {
        String sql = "UPDATE " + tableName + " SET sentenca_caminho = ?, sentenca_salva_em = NOW() WHERE numero_processo = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, arquivo != null ? arquivo.toString() : null);
            ps.setString(2, numeroProcesso);
            ps.executeUpdate();
        }
    }

    public void truncarCursor() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE " + cursorTable);
            LOGGER.info("Tabela de cursor '{}' truncada.", cursorTable);
        }
    }

    public void truncarProcessos() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE " + tableName + " RESTART IDENTITY");
            LOGGER.info("Tabela de processos '{}' truncada.", tableName);
        }
    }

    public List<String> carregarCursor() throws SQLException {
        String sql = "SELECT cursor FROM " + cursorTable + " WHERE id = 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String json = rs.getString(1);
                if (json == null || json.isBlank()) {
                    return Collections.emptyList();
                }
                try {
                    JsonNode node = objectMapper.readTree(json);
                    List<String> valores = new ArrayList<>();
                    for (JsonNode valueNode : node) {
                        if (valueNode != null && valueNode.isString()) {
                            valores.add(valueNode.asString());
                        } else if (valueNode != null) {
                            valores.add(valueNode.toString());
                        }
                    }
                    return valores;
                } catch (JacksonException e) {
                    LOGGER.error("Falha ao converter cursor salvo. Reiniciando a partir do início.", e);
                    return Collections.emptyList();
                }
            }
            return Collections.emptyList();
        }
    }

    public void salvarCursor(List<String> cursor) throws SQLException {
        String json = null;
        try {
            if (cursor != null && !cursor.isEmpty()) {
                json = objectMapper.writeValueAsString(cursor);
            }
        } catch (JacksonException e) {
            LOGGER.error("Não foi possível serializar o cursor de busca.", e);
            return;
        }

        String sql = "INSERT INTO " + cursorTable + " (id, cursor, atualizado_em) VALUES (1, ?::jsonb, NOW()) " +
                "ON CONFLICT (id) DO UPDATE SET cursor = EXCLUDED.cursor, atualizado_em = NOW()";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, json);
            ps.executeUpdate();
        }
    }

    public void salvarProcessos(List<Processo> processos) throws SQLException, IOException {
        if (processos == null || processos.isEmpty()) {
            LOGGER.info("Nenhum processo recebido para persistir.");
            return;
        }

        LOGGER.info("Persistindo {} processos na tabela {}", processos.size(), tableName);
        String sql = "INSERT INTO " + tableName + " (numero_processo, grau, classe, orgao_julgador, payload) " +
                "VALUES (?, ?, ?, ?, ?::jsonb) " +
                "ON CONFLICT (numero_processo) DO UPDATE SET " +
                "grau = EXCLUDED.grau, " +
                "classe = EXCLUDED.classe, " +
                "orgao_julgador = EXCLUDED.orgao_julgador, " +
                "payload = EXCLUDED.payload, " +
                "atualizado_em = NOW()";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Processo processo : processos) {
                if (processo == null || processo.numeroProcesso() == null || processo.numeroProcesso().isBlank()) {
                    LOGGER.warn("Processo ignorado por não possuir número válido.");
                    continue;
                }
                ps.setString(1, processo.numeroProcesso());
                ps.setString(2, processo.grau());
                ps.setString(3, processo.classe() != null ? processo.classe().nome() : null);
                ps.setString(4, processo.orgaoJulgador() != null ? processo.orgaoJulgador().nome() : null);
                ps.setString(5, objectMapper.writeValueAsString(processo));
                ps.addBatch();
            }
            ps.executeBatch();
            LOGGER.info("Persistência concluída na tabela {}.", tableName);
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            LOGGER.info("Encerrando conexão com o banco.");
            connection.close();
        }
    }
}
