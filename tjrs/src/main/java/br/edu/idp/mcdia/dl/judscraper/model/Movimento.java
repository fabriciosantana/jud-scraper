package br.edu.idp.mcdia.dl.judscraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Movimento(
        Integer codigo,
        String nome,
        String dataHora,
        @JsonProperty("complementosTabelados") List<ComplementoTabelado> complementos) {
}
