package br.com.damasonline.webSoket;

import br.com.damasonline.dto.MensagemJogo;
import br.com.damasonline.dto.ResultadoJogada;
import br.com.damasonline.model.Jogador;
import br.com.damasonline.model.Sala;
import br.com.damasonline.service.GerenciadorDeSalas;
import br.com.damasonline.service.JogoService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JogoWebSocketHandler extends TextWebSocketHandler {

    private final GerenciadorDeSalas gerenciadorDeSalas;
    private final JogoService jogoService;
    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessoes = new ConcurrentHashMap<>();

    public JogoWebSocketHandler(
            GerenciadorDeSalas gerenciadorDeSalas,
            JogoService jogoService,
            ObjectMapper objectMapper) {

        this.gerenciadorDeSalas = gerenciadorDeSalas;
        this.jogoService = jogoService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessoes.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message) throws Exception {

        MensagemJogo mensagem = objectMapper.readValue(
                message.getPayload(),
                MensagemJogo.class
        );

        if (mensagem.getCodigo() == null) {
            enviarErro(session, "Mensagem sem código");
            return;
        }

        switch (mensagem.getCodigo()) {

            case "ENTRAR" -> entrar(session, mensagem);

            case "MOVIMENTO" -> movimento(session, mensagem);

            case "DESISTIR" -> desistir(session);

            default -> enviarErro(session, "Código de mensagem inválido");
        }
    }

    private void entrar(
            WebSocketSession session,
            MensagemJogo mensagem) throws Exception {

        Jogador jaExiste = gerenciadorDeSalas
                .buscarJogadorPorSessao(session.getId());

        if (jaExiste != null) {
            enviarErro(session, "Você já entrou no jogo");
            return;
        }

        if (mensagem.getNome() == null ||
                mensagem.getNome().isBlank()) {

            enviarErro(session, "Digite seu nome");
            return;
        }

        Jogador jogador = gerenciadorDeSalas.criarJogador(
                mensagem.getNome().trim(),
                session.getId()
        );

        gerenciadorDeSalas.colocarNaFila(jogador);

        Map<String, Object> esperando = new HashMap<>();

        esperando.put("codigo", "AGUARDANDO");
        esperando.put("jogadorId", jogador.getId());
        esperando.put("nome", jogador.getNome());

        enviar(session, esperando);

        Sala sala = gerenciadorDeSalas.tentarCriarSala();

        if (sala != null) {
            iniciarSala(sala);
        }
    }

    private void iniciarSala(Sala sala) throws Exception {

        Jogador jogador1 = sala.getJogador1();
        Jogador jogador2 = sala.getJogador2();

        enviarInicio(sala, jogador1, jogador2);
        enviarInicio(sala, jogador2, jogador1);
    }

    private void enviarInicio(
            Sala sala,
            Jogador jogador,
            Jogador adversario) throws Exception {

        WebSocketSession session = sessoes.get(
                jogador.getSessionId()
        );

        if (session == null) {
            return;
        }

        Map<String, Object> resposta = new HashMap<>();

        resposta.put("codigo", "INICIAR");
        resposta.put("jogadorId", jogador.getId());
        resposta.put("nomeJogador", jogador.getNome());
        resposta.put("nomeAdversario", adversario.getNome());
        resposta.put("salaId", sala.getId());
        resposta.put("cor", jogador.getCor());
        resposta.put("jogadorAtual", sala.getJogadorAtual());
        resposta.put("tabuleiro", sala.getTabuleiro().getCasas());

        enviar(session, resposta);
    }

    private void movimento(
            WebSocketSession session,
            MensagemJogo mensagem) throws Exception {

        Jogador jogador = gerenciadorDeSalas
                .buscarJogadorPorSessao(session.getId());

        if (jogador == null) {
            enviarErro(session, "Jogador não encontrado");
            return;
        }

        Sala sala = gerenciadorDeSalas
                .buscarSalaDoJogador(jogador);

        if (sala == null) {
            enviarErro(session, "Sala não encontrada");
            return;
        }

        ResultadoJogada resultado = jogoService.mover(
                sala,
                jogador,
                mensagem
        );

        if (!resultado.isValido()) {
            enviarErro(session, resultado.getMensagem());
            return;
        }

        if (resultado.isFinalizada()) {

            Map<String, Object> resposta = new HashMap<>();

            resposta.put("codigo", "FINALIZAR");
            resposta.put("mensagem", "Partida finalizada");
            resposta.put("vencedorId", resultado.getVencedorId());
            resposta.put("tabuleiro", sala.getTabuleiro().getCasas());

            enviarParaSala(sala, resposta);

            gerenciadorDeSalas.removerSala(sala.getId());

            return;
        }

        Map<String, Object> resposta = new HashMap<>();

        resposta.put("codigo", "ATUALIZAR");
        resposta.put("tabuleiro", sala.getTabuleiro().getCasas());
        resposta.put("jogadorAtual", sala.getJogadorAtual());
        resposta.put("captura", resultado.isCaptura());
        resposta.put("promocao", resultado.isPromocao());
        resposta.put("continuarCaptura", resultado.isContinuarCaptura());
        resposta.put(
                "linhaCapturaObrigatoria",
                sala.getLinhaCapturaObrigatoria()
        );
        resposta.put(
                "colunaCapturaObrigatoria",
                sala.getColunaCapturaObrigatoria()
        );

        enviarParaSala(sala, resposta);
    }

    private void desistir(WebSocketSession session) throws Exception {

        Jogador jogador = gerenciadorDeSalas
                .buscarJogadorPorSessao(session.getId());

        if (jogador == null) {
            return;
        }

        Sala sala = gerenciadorDeSalas.buscarSalaDoJogador(jogador);

        if (sala == null) {

            gerenciadorDeSalas.removerDaFila(jogador);

            return;
        }

        Jogador vencedor = sala.outroJogador(jogador.getId());

        sala.getLock().lock();

        try {

            sala.setEstado("FINALIZADA");

            Map<String, Object> resposta = new HashMap<>();

            resposta.put("codigo", "FINALIZAR");
            resposta.put("mensagem", jogador.getNome() + " desistiu");
            resposta.put("vencedorId", vencedor.getId());
            resposta.put("tabuleiro", sala.getTabuleiro().getCasas());

            enviarParaSala(sala, resposta);

        } finally {

            sala.getLock().unlock();
        }

        gerenciadorDeSalas.removerSala(sala.getId());
    }

    private void enviarParaSala(
            Sala sala,
            Map<String, Object> dados) throws Exception {

        WebSocketSession sessao1 = sessoes.get(
                sala.getJogador1().getSessionId()
        );

        WebSocketSession sessao2 = sessoes.get(
                sala.getJogador2().getSessionId()
        );

        enviar(sessao1, dados);
        enviar(sessao2, dados);
    }

    private void enviarErro(
            WebSocketSession session,
            String mensagem) throws Exception {

        Map<String, Object> resposta = new HashMap<>();

        resposta.put("codigo", "ERRO");
        resposta.put("mensagem", mensagem);

        enviar(session, resposta);
    }

    private void enviar(
            WebSocketSession session,
            Object dados) throws Exception {

        if (session == null || !session.isOpen()) {
            return;
        }

        String json = objectMapper.writeValueAsString(dados);

        synchronized (session) {
            session.sendMessage(new TextMessage(json));
        }
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) throws Exception {

        sessoes.remove(session.getId());

        Jogador jogador = gerenciadorDeSalas
                .buscarJogadorPorSessao(session.getId());

        if (jogador == null) {
            return;
        }

        gerenciadorDeSalas.removerDaFila(jogador);

        Sala sala = gerenciadorDeSalas.buscarSalaDoJogador(jogador);

        if (sala != null) {

            sala.getLock().lock();

            try {

                if (sala.getEstado().equals("EM_ANDAMENTO")) {

                    sala.setEstado("FINALIZADA");

                    Jogador vencedor = sala.outroJogador(
                            jogador.getId()
                    );

                    Map<String, Object> resposta = new HashMap<>();

                    resposta.put("codigo", "FINALIZAR");
                    resposta.put(
                            "mensagem",
                            jogador.getNome() + " desconectou"
                    );
                    resposta.put("vencedorId", vencedor.getId());
                    resposta.put(
                            "tabuleiro",
                            sala.getTabuleiro().getCasas()
                    );

                    WebSocketSession sessaoVencedor = sessoes.get(
                            vencedor.getSessionId()
                    );

                    enviar(sessaoVencedor, resposta);
                }

            } finally {

                sala.getLock().unlock();
            }

            gerenciadorDeSalas.removerSala(sala.getId());
        }

        gerenciadorDeSalas.removerJogador(jogador);
    }
}
