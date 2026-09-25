package br.com.damasonline.service;

import br.com.damasonline.model.Jogador;
import br.com.damasonline.model.Sala;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class GerenciadorDeSalas {
    private final Queue<Jogador> fila = new ConcurrentLinkedDeque<>();
    private final Map<String, Sala> salas = new ConcurrentHashMap<>();
    private final Map<String, Jogador> jogadoresPorSessao = new ConcurrentHashMap<>();

    public Jogador criarJogador(String nome, String sessionId){
        String id = UUID.randomUUID().toString();
        Jogador jogador = new Jogador(id, nome, sessionId);
        jogadoresPorSessao.put(sessionId, jogador);

        return jogador;
    }

    public synchronized void colocarNaFila(Jogador jogador){
        fila.offer(jogador);
    }

    public synchronized Sala tentarCriarSala(){
        if (fila.size() < 2){
            return null;
        }
        Jogador jogador1 = fila.poll();
        Jogador jogador2 = fila.poll();

        if (jogador1 == null || jogador2 == null){
            return null;
        }

        jogador1.setCor("BRANCA");
        jogador2.setCor("PRETA");

        String idSala = UUID.randomUUID().toString();
        jogador1.setSalaId(idSala);
        jogador2.setSalaId(idSala);

        Sala sala = new Sala(idSala, jogador1, jogador2);
        salas.put(idSala, sala);

        return sala;
    }

    public Jogador buscarJogadorPorSessao(String sessionId){
        return jogadoresPorSessao.get(sessionId);
    }

    public Sala buscarSalaDoJogador(Jogador jogador){
        if(jogador == null || jogador.getSalaId() == null){
            return null;
        }

        return salas.get(jogador.getSalaId());
    }

    public synchronized void removerDaFila(Jogador jogador){
        fila.remove(jogador);
    }

    public void removerJogador(Jogador jogador){
        if(jogador == null){
            return;
        }
        jogadoresPorSessao.remove(jogador.getSessionId());
    }
    public void removerSala(String salaId){
        Sala sala = salas.remove(salaId);

        if(sala == null){
            return;
        }

        liberarJogador(sala.getJogador1());
        liberarJogador(sala.getJogador2());
    }

    private void liberarJogador(Jogador jogador){
        jogador.setSalaId(null);
        jogadoresPorSessao.remove(jogador.getSessionId(), jogador);
    }
}
