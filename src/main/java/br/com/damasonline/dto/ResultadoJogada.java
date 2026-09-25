package br.com.damasonline.dto;

public class ResultadoJogada {
    private boolean valido;
    private String mensagem;
    private boolean captura;
    private boolean promocao;
    private boolean continuarCaptura;
    private boolean finalizada;
    private String vencedorId;

    public boolean isValido() {
        return valido;
    }

    public void setValido(boolean valido) {
        this.valido = valido;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public boolean isCaptura() {
        return captura;
    }

    public void setCaptura(boolean captura) {
        this.captura = captura;
    }

    public boolean isPromocao() {
        return promocao;
    }

    public void setPromocao(boolean promocao) {
        this.promocao = promocao;
    }

    public boolean isContinuarCaptura() {
        return continuarCaptura;
    }

    public void setContinuarCaptura(boolean continuarCaptura) {
        this.continuarCaptura = continuarCaptura;
    }

    public boolean isFinalizada() {
        return finalizada;
    }

    public void setFinalizada(boolean finalizada) {
        this.finalizada = finalizada;
    }

    public String getVencedorId() {
        return vencedorId;
    }

    public void setVencedorId(String vencedorId) {
        this.vencedorId = vencedorId;
    }
}
