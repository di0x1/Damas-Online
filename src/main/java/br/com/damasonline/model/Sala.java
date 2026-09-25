package br.com.damasonline.model;

import java.util.concurrent.locks.ReentrantLock;

public class Sala {
    private String id;
    private Jogador jogador1;
    private Jogador jogador2;
    private Tabuleiro tabuleiro;
    private String jogadorAtual;
    private String estado;
    private final ReentrantLock lock = new ReentrantLock();

    private int linhaCapturaObrigatoria = -1;
    private int colunaCapturaObrigatoria = -1;

    public Sala(String id, Jogador jogador1, Jogador jogador2){
        this.id = id;
        this.jogador1 = jogador1;
        this.jogador2 = jogador2;
        this.tabuleiro = new Tabuleiro();
        this.jogadorAtual = jogador1.getId();
        this.estado = "EM_ANDAMENTO";
    }

    public Jogador outroJogador(String jogadorId){
        if(jogador1.getId().equals(jogadorId)){
            return jogador2;
        }
        return jogador1;
    }

    public Boolean temJogador(String jogadorId){
        return jogador1.getId().equals(jogadorId)
        || jogador2.getId().equals(jogadorId);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Jogador getJogador1() {
        return jogador1;
    }

    public void setJogador1(Jogador jogador1) {
        this.jogador1 = jogador1;
    }

    public Jogador getJogador2() {
        return jogador2;
    }

    public void setJogador2(Jogador jogador2) {
        this.jogador2 = jogador2;
    }

    public Tabuleiro getTabuleiro() {
        return tabuleiro;
    }

    public void setTabuleiro(Tabuleiro tabuleiro) {
        this.tabuleiro = tabuleiro;
    }

    public String getJogadorAtual() {
        return jogadorAtual;
    }

    public void setJogadorAtual(String jogadorAtual) {
        this.jogadorAtual = jogadorAtual;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public int getLinhaCapturaObrigatoria() {
        return linhaCapturaObrigatoria;
    }

    public void setLinhaCapturaObrigatoria(int linhaCapturaObrigatoria) {
        this.linhaCapturaObrigatoria = linhaCapturaObrigatoria;
    }

    public int getColunaCapturaObrigatoria() {
        return colunaCapturaObrigatoria;
    }

    public void setColunaCapturaObrigatoria(int colunaCapturaObrigatoria) {
        this.colunaCapturaObrigatoria = colunaCapturaObrigatoria;
    }
}
