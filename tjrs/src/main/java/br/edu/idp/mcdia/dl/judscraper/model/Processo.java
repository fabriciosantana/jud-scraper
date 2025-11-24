package br.edu.idp.mcdia.dl.judscraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Processo {

    private String numeroProcesso;
    private Classe classe;
    private Sistema sistema;
    private Formato formato;
    private String tribunal;
    private String dataHoraUltimaAtualizacao;
    private String grau;

    @JsonProperty("@timestamp")
    private String timestamp;

    private String dataAjuizamento;
    private List<Movimento> movimentos;
    private String id;
    private Integer nivelSigilo;
    private OrgaoJulgador orgaoJulgador;
    private List<Assunto> assuntos;

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public Classe getClasse() {
        return classe;
    }

    public void setClasse(Classe classe) {
        this.classe = classe;
    }

    public Sistema getSistema() {
        return sistema;
    }

    public void setSistema(Sistema sistema) {
        this.sistema = sistema;
    }

    public Formato getFormato() {
        return formato;
    }

    public void setFormato(Formato formato) {
        this.formato = formato;
    }

    public String getTribunal() {
        return tribunal;
    }

    public void setTribunal(String tribunal) {
        this.tribunal = tribunal;
    }

    public String getDataHoraUltimaAtualizacao() {
        return dataHoraUltimaAtualizacao;
    }

    public void setDataHoraUltimaAtualizacao(String dataHoraUltimaAtualizacao) {
        this.dataHoraUltimaAtualizacao = dataHoraUltimaAtualizacao;
    }

    public String getGrau() {
        return grau;
    }

    public void setGrau(String grau) {
        this.grau = grau;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDataAjuizamento() {
        return dataAjuizamento;
    }

    public void setDataAjuizamento(String dataAjuizamento) {
        this.dataAjuizamento = dataAjuizamento;
    }

    public List<Movimento> getMovimentos() {
        return movimentos;
    }

    public void setMovimentos(List<Movimento> movimentos) {
        this.movimentos = movimentos;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getNivelSigilo() {
        return nivelSigilo;
    }

    public void setNivelSigilo(Integer nivelSigilo) {
        this.nivelSigilo = nivelSigilo;
    }

    public OrgaoJulgador getOrgaoJulgador() {
        return orgaoJulgador;
    }

    public void setOrgaoJulgador(OrgaoJulgador orgaoJulgador) {
        this.orgaoJulgador = orgaoJulgador;
    }

    public List<Assunto> getAssuntos() {
        return assuntos;
    }

    public void setAssuntos(List<Assunto> assuntos) {
        this.assuntos = assuntos;
    }
}
