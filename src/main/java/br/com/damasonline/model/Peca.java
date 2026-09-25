package br.com.damasonline.model;

public class Peca {
    private String cor;
    private boolean dama;

    public Peca(){

    }

    public Peca(String cor){
        this.cor = cor;
        this.dama = false;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public boolean isDama() {
        return dama;
    }

    public void setDama(boolean dama) {
        this.dama = dama;
    }
}
