package br.edu.idp.mcdia.dl.judscraper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.idp.mcdia.dl.judscraper.model.Processo;
import tools.jackson.databind.ObjectMapper;

public class DatajudRepository implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatajudRepository.class);
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final Connection connection;
    private final String tableName;
    private final ObjectMapper objectMapper;

    public DatajudRepository(String url, String user, String password, String tableName, ObjectMapper objectMapper) throws SQLException {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL do banco não pode ser vazia.");
        }
        LOGGER.info("Conectando ao banco PostgreSQL em {}", url);
        this.connection = DriverManager.getConnection(url, user, password);
        this.tableName = sanitizeTableName(tableName);
        this.objectMapper = objectMapper;
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
                "data_ultima_atualizacao TIMESTAMPTZ," +
                "atualizado_em TIMESTAMPTZ NOT NULL DEFAULT NOW()," +
                "CONSTRAINT uq_numero_processo UNIQUE (numero_processo)" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS data_ultima_atualizacao TIMESTAMPTZ");
            LOGGER.info("Tabela {} disponível para persistência.", tableName);
        }
    }

    public OffsetDateTime buscarUltimaDataAtualizacao() throws SQLException {
        String sql = "SELECT MAX(data_ultima_atualizacao) FROM " + tableName;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                OffsetDateTime data = rs.getObject(1, OffsetDateTime.class);
                LOGGER.info("Última data de atualização encontrada: {}", data);
                return data;
            }
            return null;
        }
    }

    public void salvarProcessos(List<Processo> processos) throws SQLException, IOException {
        if (processos == null || processos.isEmpty()) {
            LOGGER.info("Nenhum processo recebido para persistir.");
            return;
        }

        LOGGER.info("Persistindo {} processos na tabela {}", processos.size(), tableName);
        String sql = "INSERT INTO " + tableName + " (numero_processo, grau, classe, orgao_julgador, payload, data_ultima_atualizacao) " +
                "VALUES (?, ?, ?, ?, ?::jsonb, ?) " +
                "ON CONFLICT (numero_processo) DO UPDATE SET " +
                "grau = EXCLUDED.grau, " +
                "classe = EXCLUDED.classe, " +
                "orgao_julgador = EXCLUDED.orgao_julgador, " +
                "payload = EXCLUDED.payload, " +
                "data_ultima_atualizacao = EXCLUDED.data_ultima_atualizacao, " +
                "atualizado_em = NOW()";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Processo processo : processos) {
                if (processo == null || processo.getNumeroProcesso() == null || processo.getNumeroProcesso().isBlank()) {
                    LOGGER.warn("Processo ignorado por não possuir número válido.");
                    continue;
                }
                ps.setString(1, processo.getNumeroProcesso());
                ps.setString(2, processo.getGrau());
                ps.setString(3, processo.getClasse() != null ? processo.getClasse().getNome() : null);
                ps.setString(4, processo.getOrgaoJulgador() != null ? processo.getOrgaoJulgador().getNome() : null);
                ps.setString(5, objectMapper.writeValueAsString(processo));
                ps.setObject(6, parseDataHora(processo.getDataHoraUltimaAtualizacao()));
                ps.addBatch();
            }
            ps.executeBatch();
            LOGGER.info("Persistência concluída na tabela {}.", tableName);
        }
    }

    private OffsetDateTime parseDataHora(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(valor);
        } catch (Exception e) {
            LOGGER.warn("Não foi possível converter dataHora '{}': {}", valor, e.getMessage());
            return null;
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
