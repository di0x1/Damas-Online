package br.com.damasonline.model;

import br.com.damasonline.enums.CorPeca;

public class Tabuleiro {

    private Peca[][] casas;

    public Tabuleiro(){
        casas = new Peca[8][8];
        montarTabuleiro();
    }

    private void montarTabuleiro(){
        for(int linha = 0; linha<3; linha++){
            for(int coluna = 0; coluna<8; coluna++){
             if((linha + coluna)% 2 !=0){
                 casas[linha][coluna] = new Peca(CorPeca.PRETA);
               }
            }
        }

        for(int linha = 5; linha<8; linha++){
            for(int coluna = 0; coluna<8; coluna++){
                if((linha + coluna) % 2 != 0 ){
                    casas[linha][coluna] = new Peca(CorPeca.BRANCA);
                }
            }
        }
    }

    public Peca[][] getCasas() {
        return casas;
    }

    public void setCasas(Peca[][] casas) {
        this.casas = casas;
    }
}
