package br.com.damasonline.model;

import br.com.damasonline.enums.CorPeca;

public class Peca {
    private CorPeca cor;
    private boolean dama;

    public Peca(){

    }

    public Peca(CorPeca cor){
        this.cor = cor;
        this.dama = false;
    }

    public CorPeca getCor() {
        return cor;
    }

    public void setCor(CorPeca cor) {
        this.cor = cor;
    }

    public boolean isDama() {
        return dama;
    }

    public void setDama(boolean dama) {
        this.dama = dama;
    }
}
