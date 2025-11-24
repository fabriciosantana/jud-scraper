package br.edu.idp.mcdia.dl.judscraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Movimento {
    private Integer codigo;
    private String nome;
    private String dataHora;

    @JsonProperty("complementosTabelados")
    private List<ComplementoTabelado> complementos;

    public Integer getCodigo() {
        return codigo;
    }

    public void setCodigo(Integer codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDataHora() {
        return dataHora;
    }

    public void setDataHora(String dataHora) {
        this.dataHora = dataHora;
    }

    public List<ComplementoTabelado> getComplementos() {
        return complementos;
    }

    public void setComplementos(List<ComplementoTabelado> complementos) {
        this.complementos = complementos;
    }
}
