package br.com.damasonline.model;

public class Jogador {
    private String id;
    private String nome;
    private String cor;
    private String sessionId;
    private String salaId;

    public Jogador(){

    }

    public Jogador(String id, String nome, String sessionId){
        this.id = id;
        this.nome = nome;
        this.sessionId = sessionId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSalaId() {
        return salaId;
    }

    public void setSalaId(String salaId) {
        this.salaId = salaId;
    }
}
