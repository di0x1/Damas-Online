package br.com.damasonline.dto;

public class MensagemJogo {
    private String codigo;
    private String nome;
    private String jogadorId;
    private String salaId;

    private Integer linhaOrigem;
    private Integer colunaOrigem;
    private Integer linhaDestino;
    private Integer colunaDestino;

    public MensagemJogo(){

    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getJogadorId() {
        return jogadorId;
    }

    public void setJogadorId(String jogadorId) {
        this.jogadorId = jogadorId;
    }

    public String getSalaId() {
        return salaId;
    }

    public void setSalaId(String salaId) {
        this.salaId = salaId;
    }

    public Integer getLinhaOrigem() {
        return linhaOrigem;
    }

    public void setLinhaOrigem(Integer linhaOrigem) {
        this.linhaOrigem = linhaOrigem;
    }

    public Integer getLinhaDestino() {
        return linhaDestino;
    }

    public void setLinhaDestino(Integer linhaDestino) {
        this.linhaDestino = linhaDestino;
    }

    public Integer getColunaOrigem() {
        return colunaOrigem;
    }

    public void setColunaOrigem(Integer colunaOrigem) {
        this.colunaOrigem = colunaOrigem;
    }

    public Integer getColunaDestino() {
        return colunaDestino;
    }

    public void setColunaDestino(Integer colunaDestino) {
        this.colunaDestino = colunaDestino;
    }
}
