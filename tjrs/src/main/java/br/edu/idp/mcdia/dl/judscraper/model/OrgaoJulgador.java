package br.edu.idp.mcdia.dl.judscraper.model;

public class OrgaoJulgador {
    private Integer codigoMunicipioIBGE;
    private Integer codigo;
    private String nome;

    public Integer getCodigoMunicipioIBGE() {
        return codigoMunicipioIBGE;
    }

    public void setCodigoMunicipioIBGE(Integer codigoMunicipioIBGE) {
        this.codigoMunicipioIBGE = codigoMunicipioIBGE;
    }

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
}
