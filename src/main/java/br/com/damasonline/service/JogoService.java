package br.com.damasonline.service;

import br.com.damasonline.dto.MensagemJogo;
import br.com.damasonline.dto.ResultadoJogada;
import br.com.damasonline.model.Jogador;
import br.com.damasonline.model.Peca;
import br.com.damasonline.model.Sala;
import br.com.damasonline.model.Tabuleiro;
import org.springframework.stereotype.Service;

@Service
public class JogoService {

    public ResultadoJogada mover(Sala sala, Jogador jogador, MensagemJogo mensagem) {

        ResultadoJogada resultado = new ResultadoJogada();

        sala.getLock().lock();

        try {

            if (!sala.getEstado().equals("EM_ANDAMENTO")) {
                return erro("A partida já terminou");
            }

            if (!sala.temJogador(jogador.getId())) {
                return erro("Jogador não pertence a essa sala");
            }

            if (!sala.getJogadorAtual().equals(jogador.getId())) {
                return erro("Não é sua vez");
            }

            if (mensagem.getLinhaOrigem() == null ||
                    mensagem.getColunaOrigem() == null ||
                    mensagem.getLinhaDestino() == null ||
                    mensagem.getColunaDestino() == null) {

                return erro("Movimento inválido");
            }

            int linhaOrigem = mensagem.getLinhaOrigem();
            int colunaOrigem = mensagem.getColunaOrigem();
            int linhaDestino = mensagem.getLinhaDestino();
            int colunaDestino = mensagem.getColunaDestino();

            if (!posicaoValida(linhaOrigem, colunaOrigem) ||
                    !posicaoValida(linhaDestino, colunaDestino)) {

                return erro("Posição fora do tabuleiro");
            }

            Tabuleiro tabuleiro = sala.getTabuleiro();

            Peca peca = tabuleiro.getCasas()[linhaOrigem][colunaOrigem];

            if (peca == null) {
                return erro("Não existe peça nessa posição");
            }

            if (!peca.getCor().equals(jogador.getCor())) {
                return erro("Essa peça não é sua");
            }

            if (tabuleiro.getCasas()[linhaDestino][colunaDestino] != null) {
                return erro("A posição de destino está ocupada");
            }

            if (sala.getLinhaCapturaObrigatoria() != -1) {

                if (linhaOrigem != sala.getLinhaCapturaObrigatoria() ||
                        colunaOrigem != sala.getColunaCapturaObrigatoria()) {

                    return erro("Você precisa continuar a captura com a mesma peça");
                }
            }

            boolean existeCaptura = existeCapturaParaJogador(
                    tabuleiro,
                    jogador.getCor()
            );

            int[] pecaCapturada = posicaoCapturada(
                    tabuleiro,
                    peca,
                    linhaOrigem,
                    colunaOrigem,
                    linhaDestino,
                    colunaDestino
            );

            boolean movimentoCaptura = pecaCapturada != null;

            if (existeCaptura && !movimentoCaptura) {
                return erro("Existe uma captura obrigatória");
            }

            if (!movimentoCaptura && !movimentoSimplesPermitido(
                    tabuleiro,
                    peca,
                    linhaOrigem,
                    colunaOrigem,
                    linhaDestino,
                    colunaDestino)) {

                return erro("Movimento inválido");
            }

            boolean capturou = false;

            if (movimentoCaptura) {

                tabuleiro.getCasas()[pecaCapturada[0]][pecaCapturada[1]] = null;

                capturou = true;
            }

            tabuleiro.getCasas()[linhaDestino][colunaDestino] = peca;
            tabuleiro.getCasas()[linhaOrigem][colunaOrigem] = null;

            boolean promocao = verificarPromocao(peca, linhaDestino);

            resultado.setValido(true);
            resultado.setCaptura(capturou);
            resultado.setPromocao(promocao);
            resultado.setMensagem("Movimento realizado");

            if (capturou && existeCapturaDaPeca(
                    tabuleiro,
                    linhaDestino,
                    colunaDestino,
                    peca)) {

                sala.setLinhaCapturaObrigatoria(linhaDestino);
                sala.setColunaCapturaObrigatoria(colunaDestino);

                resultado.setContinuarCaptura(true);

                return resultado;
            }

            sala.setLinhaCapturaObrigatoria(-1);
            sala.setColunaCapturaObrigatoria(-1);

            Jogador adversario = sala.outroJogador(jogador.getId());

            if (!temPecas(tabuleiro, adversario.getCor())
                    || !temMovimento(tabuleiro, adversario.getCor())) {

                sala.setEstado("FINALIZADA");

                resultado.setFinalizada(true);
                resultado.setVencedorId(jogador.getId());

                return resultado;
            }

            sala.setJogadorAtual(adversario.getId());

            return resultado;

        } finally {
            sala.getLock().unlock();
        }
    }

    private static final int[][] DIRECOES = {
            {-1, -1},
            {-1, 1},
            {1, -1},
            {1, 1}
    };

    private boolean movimentoSimplesPermitido(
            Tabuleiro tabuleiro,
            Peca peca,
            int linhaOrigem,
            int colunaOrigem,
            int linhaDestino,
            int colunaDestino) {

        int diferencaLinha = linhaDestino - linhaOrigem;
        int diferencaColuna = colunaDestino - colunaOrigem;

        if (Math.abs(diferencaLinha) != Math.abs(diferencaColuna)) {
            return false;
        }

        if (peca.isDama()) {
            return caminhoLivre(
                    tabuleiro,
                    linhaOrigem,
                    colunaOrigem,
                    linhaDestino,
                    colunaDestino
            );
        }

        if (Math.abs(diferencaColuna) != 1) {
            return false;
        }

        if (peca.getCor().equals("BRANCA")) {
            return diferencaLinha == -1;
        }

        return diferencaLinha == 1;
    }

    private int[] posicaoCapturada(
            Tabuleiro tabuleiro,
            Peca peca,
            int linhaOrigem,
            int colunaOrigem,
            int linhaDestino,
            int colunaDestino) {

        int diferencaLinha = linhaDestino - linhaOrigem;
        int diferencaColuna = colunaDestino - colunaOrigem;
        int distancia = Math.abs(diferencaLinha);

        if (distancia != Math.abs(diferencaColuna) || distancia < 2) {
            return null;
        }

        if (!peca.isDama() && distancia != 2) {
            return null;
        }

        int passoLinha = Integer.signum(diferencaLinha);
        int passoColuna = Integer.signum(diferencaColuna);

        int[] capturada = null;

        for (int i = 1; i < distancia; i++) {

            int linha = linhaOrigem + i * passoLinha;
            int coluna = colunaOrigem + i * passoColuna;

            Peca atual = tabuleiro.getCasas()[linha][coluna];

            if (atual == null) {
                continue;
            }

            if (atual.getCor().equals(peca.getCor()) || capturada != null) {
                return null;
            }

            capturada = new int[]{linha, coluna};
        }

        return capturada;
    }

    private boolean caminhoLivre(
            Tabuleiro tabuleiro,
            int linhaOrigem,
            int colunaOrigem,
            int linhaDestino,
            int colunaDestino) {

        int passoLinha = Integer.signum(linhaDestino - linhaOrigem);
        int passoColuna = Integer.signum(colunaDestino - colunaOrigem);
        int distancia = Math.abs(linhaDestino - linhaOrigem);

        for (int i = 1; i < distancia; i++) {

            if (tabuleiro.getCasas()[linhaOrigem + i * passoLinha]
                    [colunaOrigem + i * passoColuna] != null) {

                return false;
            }
        }

        return true;
    }

    private boolean verificarPromocao(Peca peca, int linha) {

        if (peca.isDama()) {
            return false;
        }

        if (peca.getCor().equals("BRANCA") && linha == 0) {
            peca.setDama(true);
            return true;
        }

        if (peca.getCor().equals("PRETA") && linha == 7) {
            peca.setDama(true);
            return true;
        }

        return false;
    }

    private boolean existeCapturaParaJogador(
            Tabuleiro tabuleiro,
            String cor) {

        for (int linha = 0; linha < 8; linha++) {

            for (int coluna = 0; coluna < 8; coluna++) {

                Peca peca = tabuleiro.getCasas()[linha][coluna];

                if (peca != null && peca.getCor().equals(cor)) {

                    if (existeCapturaDaPeca(
                            tabuleiro,
                            linha,
                            coluna,
                            peca)) {

                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean existeCapturaDaPeca(
            Tabuleiro tabuleiro,
            int linha,
            int coluna,
            Peca peca) {

        for (int[] direcao : DIRECOES) {

            int linhaAtual = linha + direcao[0];
            int colunaAtual = coluna + direcao[1];

            if (peca.isDama()) {
                while (posicaoValida(linhaAtual, colunaAtual) &&
                        tabuleiro.getCasas()[linhaAtual][colunaAtual] == null) {

                    linhaAtual += direcao[0];
                    colunaAtual += direcao[1];
                }
            }

            if (!posicaoValida(linhaAtual, colunaAtual)) {
                continue;
            }

            Peca alvo = tabuleiro.getCasas()[linhaAtual][colunaAtual];

            if (alvo == null || alvo.getCor().equals(peca.getCor())) {
                continue;
            }

            int linhaDestino = linhaAtual + direcao[0];
            int colunaDestino = colunaAtual + direcao[1];

            if (posicaoValida(linhaDestino, colunaDestino) &&
                    tabuleiro.getCasas()[linhaDestino][colunaDestino] == null) {

                return true;
            }
        }

        return false;
    }

    private boolean temPecas(Tabuleiro tabuleiro, String cor) {

        for (int linha = 0; linha < 8; linha++) {

            for (int coluna = 0; coluna < 8; coluna++) {

                Peca peca = tabuleiro.getCasas()[linha][coluna];

                if (peca != null && peca.getCor().equals(cor)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean temMovimento(Tabuleiro tabuleiro, String cor) {

        for (int linha = 0; linha < 8; linha++) {

            for (int coluna = 0; coluna < 8; coluna++) {

                Peca peca = tabuleiro.getCasas()[linha][coluna];

                if (peca == null || !peca.getCor().equals(cor)) {
                    continue;
                }

                if (existeCapturaDaPeca(tabuleiro, linha, coluna, peca)) {
                    return true;
                }

                if (temMovimentoSimples(tabuleiro, linha, coluna, peca)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean temMovimentoSimples(
            Tabuleiro tabuleiro,
            int linha,
            int coluna,
            Peca peca) {

        int[][] direcoes;

        if (peca.isDama()) {

            direcoes = new int[][]{
                    {-1, -1},
                    {-1, 1},
                    {1, -1},
                    {1, 1}
            };

        } else if (peca.getCor().equals("BRANCA")) {

            direcoes = new int[][]{
                    {-1, -1},
                    {-1, 1}
            };

        } else {

            direcoes = new int[][]{
                    {1, -1},
                    {1, 1}
            };
        }

        for (int[] direcao : direcoes) {

            int novaLinha = linha + direcao[0];
            int novaColuna = coluna + direcao[1];

            if (posicaoValida(novaLinha, novaColuna) &&
                    tabuleiro.getCasas()[novaLinha][novaColuna] == null) {

                return true;
            }
        }

        return false;
    }

    private boolean posicaoValida(int linha, int coluna) {

        return linha >= 0
                && linha < 8
                && coluna >= 0
                && coluna < 8;
    }

    private ResultadoJogada erro(String mensagem) {

        ResultadoJogada resultado = new ResultadoJogada();

        resultado.setValido(false);
        resultado.setMensagem(mensagem);

        return resultado;
    }
}
