package br.edu.idp.mcdia.dl.judscraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

public record Processo(
        String numeroProcesso,
        Classe classe,
        Sistema sistema,
        Formato formato,
        String tribunal,
        String dataHoraUltimaAtualizacao,
        String grau,
        @JsonProperty("@timestamp") String timestamp,
        String dataAjuizamento,
        List<Movimento> movimentos,
        String id,
        Integer nivelSigilo,
        OrgaoJulgador orgaoJulgador,
        @JsonDeserialize(using = AssuntoListDeserializer.class) List<Assunto> assuntos) {
}
