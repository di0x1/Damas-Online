const LETRAS_COLUNA = "abcdefgh";
const MENSAGEM_FIM_PADRAO = "Partida finalizada";

let socket = null;
let entradaPendente = false;

let jogadorId = null;
let salaId = null;
let nomeJogador = "";
let nomeAdversario = "";
let minhaCor = null;
let jogadorAtual = null;
let tabuleiroAtual = null;
let pecaSelecionada = null;
let destinosPossiveis = [];
let capturaObrigatoria = null;
let ultimaJogada = [];
let partidaFinalizada = true;
let tabuleiroInvertido = false;
let jogadaEnviada = false;
let desistiu = false;
let casas = {};

const elementos = {
    corpo: document.body,
    telaEntrada: document.getElementById("telaEntrada"),
    telaEspera: document.getElementById("telaEspera"),
    telaJogo: document.getElementById("telaJogo"),
    statusConexao: document.getElementById("statusConexao"),
    textoConexao: document.getElementById("textoConexao"),
    formEntrada: document.getElementById("formEntrada"),
    campoNome: document.getElementById("campoNome"),
    botaoEntrar: document.getElementById("botaoEntrar"),
    mensagemEntrada: document.getElementById("mensagemEntrada"),
    nomeEspera: document.getElementById("nomeEspera"),
    botaoSairFila: document.getElementById("botaoSairFila"),
    tabuleiro: document.getElementById("tabuleiro"),
    jogadorAdversario: document.getElementById("jogadorAdversario"),
    jogadorVoce: document.getElementById("jogadorVoce"),
    estadoPartida: document.querySelector(".estado-partida"),
    estadoTurno: document.getElementById("estadoTurno"),
    textoTurno: document.getElementById("textoTurno"),
    detalheTurno: document.getElementById("detalheTurno"),
    resultadoPartida: document.getElementById("resultadoPartida"),
    simboloResultado: document.getElementById("simboloResultado"),
    tituloResultado: document.getElementById("tituloResultado"),
    textoResultado: document.getElementById("textoResultado"),
    detalheResultado: document.getElementById("detalheResultado"),
    mensagemJogo: document.getElementById("mensagemJogo"),
    botaoDesistir: document.getElementById("botaoDesistir"),
    botaoNovaPartida: document.getElementById("botaoNovaPartida")
};

const movimentoReduzido = window.matchMedia("(prefers-reduced-motion: reduce)");

function conectar() {
    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
        return;
    }

    const protocolo = window.location.protocol === "https:" ? "wss" : "ws";
    const novoSocket = new WebSocket(`${protocolo}://${window.location.host}/ws`);
    socket = novoSocket;
    atualizarConexao("conectando");

    novoSocket.onopen = () => {
        if (novoSocket !== socket) {
            return;
        }
        atualizarConexao("conectado");
        if (entradaPendente) {
            entradaPendente = false;
            enviar({ codigo: "ENTRAR", nome: nomeJogador });
        }
    };

    novoSocket.onmessage = (evento) => {
        if (novoSocket !== socket) {
            return;
        }
        let dados;
        try {
            dados = JSON.parse(evento.data);
        } catch (erro) {
            return;
        }
        receberMensagem(dados);
    };

    novoSocket.onclose = () => {
        if (novoSocket !== socket) {
            return;
        }
        atualizarConexao("desconectado");
        tratarQuedaConexao();
    };

    novoSocket.onerror = () => {
        if (novoSocket === socket) {
            atualizarConexao("desconectado");
        }
    };
}

function enviar(dados) {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
        mostrarMensagem("Sem conexão com o servidor", "erro");
        return false;
    }
    socket.send(JSON.stringify(dados));
    return true;
}

function atualizarConexao(estado) {
    const textos = {
        conectando: "conectando",
        conectado: "conectado",
        desconectado: "desconectado"
    };
    elementos.statusConexao.className = `status-conexao status-${estado}`;
    elementos.textoConexao.textContent = textos[estado];
}

function tratarQuedaConexao() {
    const tela = elementos.corpo.dataset.tela;

    if (tela === "jogo" && !partidaFinalizada) {
        finalizarPartida({
            conexaoPerdida: true,
            mensagem: "A conexão com o servidor foi perdida."
        });
        return;
    }

    if (tela === "jogo" && entradaPendente) {
        entradaPendente = false;
        elementos.botaoNovaPartida.hidden = false;
        mostrarMensagem("Não foi possível conectar ao servidor", "erro");
        return;
    }

    if (tela === "espera") {
        mostrarTela("entrada");
    }

    if (elementos.corpo.dataset.tela === "entrada") {
        liberarFormulario();
        if (entradaPendente || tela === "espera") {
            mostrarErroEntrada("Não foi possível conectar ao servidor.");
        }
    }

    entradaPendente = false;
}

function mostrarTela(nome) {
    const telas = {
        entrada: elementos.telaEntrada,
        espera: elementos.telaEspera,
        jogo: elementos.telaJogo
    };

    Object.entries(telas).forEach(([chave, tela]) => {
        const ativa = chave === nome;
        tela.hidden = !ativa;
        tela.classList.toggle("tela-ativa", ativa);
    });

    elementos.corpo.dataset.tela = nome;

    if (nome === "entrada") {
        document.title = "Damas Online";
        elementos.campoNome.focus();
    }
    if (nome === "espera") {
        document.title = "Aguardando adversário · Damas Online";
        elementos.botaoSairFila.focus();
    }
}

function entrar(evento) {
    evento.preventDefault();

    const nome = elementos.campoNome.value.trim();

    if (!nome) {
        mostrarErroEntrada("Digite seu nome para entrar.");
        elementos.campoNome.focus();
        return;
    }

    nomeJogador = nome;
    elementos.campoNome.removeAttribute("aria-invalid");
    elementos.mensagemEntrada.textContent = "";
    elementos.botaoEntrar.disabled = true;
    elementos.botaoEntrar.textContent = "Entrando…";

    entrarNaFila();
}

function entrarNaFila() {
    if (socket && socket.readyState === WebSocket.OPEN) {
        enviar({ codigo: "ENTRAR", nome: nomeJogador });
        return;
    }
    entradaPendente = true;
    conectar();
}

function mostrarErroEntrada(texto) {
    elementos.mensagemEntrada.textContent = texto;
    elementos.campoNome.setAttribute("aria-invalid", "true");
}

function liberarFormulario() {
    elementos.botaoEntrar.disabled = false;
    elementos.botaoEntrar.textContent = "Entrar na partida";
}

function sairDaFila() {
    const socketAntigo = socket;
    socket = null;
    entradaPendente = false;
    if (socketAntigo) {
        socketAntigo.close();
    }
    liberarFormulario();
    mostrarTela("entrada");
    conectar();
}

function receberMensagem(dados) {
    switch (dados.codigo) {
        case "AGUARDANDO":
            aguardarAdversario(dados);
            break;
        case "INICIAR":
            iniciarPartida(dados);
            break;
        case "ATUALIZAR":
            atualizarPartida(dados);
            break;
        case "FINALIZAR":
            finalizarPartida(dados);
            break;
        case "ERRO":
            tratarErro(dados.mensagem);
            break;
    }
}

function aguardarAdversario(dados) {
    jogadorId = dados.jogadorId;
    nomeJogador = dados.nome || nomeJogador;
    elementos.nomeEspera.textContent = nomeJogador;
    liberarFormulario();
    mostrarTela("espera");
}

function tratarErro(texto) {
    const mensagem = texto || "Algo deu errado";
    jogadaEnviada = false;

    if (elementos.corpo.dataset.tela === "jogo" && partidaFinalizada) {
        elementos.botaoNovaPartida.hidden = false;
        mostrarMensagem(mensagem, "erro");
        return;
    }

    if (elementos.corpo.dataset.tela !== "jogo") {
        mostrarTela("entrada");
        liberarFormulario();
        mostrarErroEntrada(mensagem);
        return;
    }

    pecaSelecionada = capturaObrigatoria && minhaVez() ? { ...capturaObrigatoria } : null;
    destinosPossiveis = pecaSelecionada ? calcularDestinos(pecaSelecionada.linha, pecaSelecionada.coluna) : [];
    desenharPecas();
    mostrarMensagem(mensagem, "erro");
}

function iniciarPartida(dados) {
    jogadorId = dados.jogadorId;
    salaId = dados.salaId;
    nomeJogador = dados.nomeJogador;
    nomeAdversario = dados.nomeAdversario;
    minhaCor = dados.cor;
    jogadorAtual = dados.jogadorAtual;
    tabuleiroAtual = dados.tabuleiro;
    tabuleiroInvertido = minhaCor === "PRETA";
    pecaSelecionada = null;
    destinosPossiveis = [];
    capturaObrigatoria = null;
    ultimaJogada = [];
    partidaFinalizada = false;
    jogadaEnviada = false;
    desistiu = false;

    preencherJogador(elementos.jogadorVoce, nomeJogador, minhaCor, "Você");
    preencherJogador(elementos.jogadorAdversario, nomeAdversario, corAdversario(), "Adversário");
    elementos.jogadorVoce.classList.remove("jogador-vencedor");
    elementos.jogadorAdversario.classList.remove("jogador-vencedor");

    elementos.estadoTurno.hidden = false;
    elementos.resultadoPartida.hidden = true;
    elementos.botaoDesistir.disabled = false;
    elementos.botaoNovaPartida.hidden = true;
    elementos.tabuleiro.classList.remove("tabuleiro-encerrado");

    montarTabuleiro();
    desenharPecas();
    atualizarTurno();
    mostrarTela("jogo");

    if (minhaVez()) {
        mostrarMensagem("Você começa. Escolha uma peça.", "info");
    } else {
        mostrarMensagem(`${nomeAdversario} começa com as brancas.`, "info");
    }
}

function preencherJogador(painel, nome, cor, papel) {
    painel.querySelector(".jogador-nome").textContent = nome;
    painel.querySelector(".jogador-detalhe").textContent = `${nomeDaCor(cor)} · ${papel}`;
    const marcador = painel.querySelector(".jogador-marcador");
    marcador.classList.toggle("peca-branca", cor === "BRANCA");
    marcador.classList.toggle("peca-preta", cor === "PRETA");
}

function atualizarPartida(dados) {
    const quemJogou = jogadorAtual;
    const fuiEu = quemJogou === jogadorId;
    const corDeQuemJogou = fuiEu ? minhaCor : corAdversario();

    const casasAlteradas = calcularUltimaJogada(tabuleiroAtual, dados.tabuleiro, corDeQuemJogou);
    if (casasAlteradas.length > 0) {
        ultimaJogada = casasAlteradas;
    }

    tabuleiroAtual = dados.tabuleiro;
    jogadorAtual = dados.jogadorAtual;
    jogadaEnviada = false;

    capturaObrigatoria = dados.continuarCaptura && dados.linhaCapturaObrigatoria >= 0
        ? { linha: dados.linhaCapturaObrigatoria, coluna: dados.colunaCapturaObrigatoria }
        : null;

    pecaSelecionada = capturaObrigatoria && minhaVez() ? { ...capturaObrigatoria } : null;
    destinosPossiveis = pecaSelecionada ? calcularDestinos(pecaSelecionada.linha, pecaSelecionada.coluna) : [];

    desenharPecas();
    atualizarTurno();
    mostrarResultadoDaJogada(dados, fuiEu);
}

function mostrarResultadoDaJogada(dados, fuiEu) {
    if (dados.continuarCaptura) {
        if (fuiEu) {
            const texto = dados.promocao ? "Peça promovida. Continue a captura" : "Peça capturada. Continue a captura";
            mostrarMensagem(texto, "aviso");
        } else {
            mostrarMensagem(`${nomeAdversario} continua a captura`, "info");
        }
        return;
    }

    if (dados.promocao) {
        if (fuiEu) {
            mostrarMensagem("Sua peça virou dama", "sucesso");
        } else {
            mostrarMensagem(`A peça de ${nomeAdversario} virou dama`, "info");
        }
        return;
    }

    if (dados.captura) {
        if (fuiEu) {
            mostrarMensagem("Peça capturada", "sucesso");
        } else {
            mostrarMensagem(`${nomeAdversario} capturou uma peça sua`, "erro");
        }
        return;
    }

    mostrarMensagem(minhaVez() ? "Sua vez" : `Vez de ${nomeAdversario}`, "info");
}

function calcularUltimaJogada(anterior, atual, corDeQuemJogou) {
    const alteradas = [];
    if (!anterior || !atual) {
        return alteradas;
    }

    for (let linha = 0; linha < 8; linha++) {
        for (let coluna = 0; coluna < 8; coluna++) {
            const antes = anterior[linha][coluna];
            const depois = atual[linha][coluna];
            const saiu = antes && antes.cor === corDeQuemJogou && !depois;
            const chegou = !antes && depois && depois.cor === corDeQuemJogou;
            if (saiu || chegou) {
                alteradas.push(chaveCasa(linha, coluna));
            }
        }
    }

    return alteradas;
}

function montarTabuleiro() {
    elementos.tabuleiro.textContent = "";
    casas = {};

    for (let linhaVisual = 0; linhaVisual < 8; linhaVisual++) {
        for (let colunaVisual = 0; colunaVisual < 8; colunaVisual++) {
            const linha = tabuleiroInvertido ? 7 - linhaVisual : linhaVisual;
            const coluna = tabuleiroInvertido ? 7 - colunaVisual : colunaVisual;
            const escura = (linha + coluna) % 2 !== 0;

            const casa = document.createElement(escura ? "button" : "div");
            casa.className = `casa ${escura ? "casa-escura" : "casa-clara"}`;
            casa.dataset.peca = "";

            if (escura) {
                casa.type = "button";
                casa.addEventListener("click", () => clicarCasa(linha, coluna));
            } else {
                casa.setAttribute("aria-hidden", "true");
            }

            if (colunaVisual === 0) {
                casa.appendChild(criarCoordenada("coordenada-linha", String(8 - linha)));
            }
            if (linhaVisual === 7) {
                casa.appendChild(criarCoordenada("coordenada-coluna", LETRAS_COLUNA[coluna]));
            }

            elementos.tabuleiro.appendChild(casa);
            casas[chaveCasa(linha, coluna)] = casa;
        }
    }
}

function criarCoordenada(classe, texto) {
    const coordenada = document.createElement("span");
    coordenada.className = `coordenada ${classe}`;
    coordenada.setAttribute("aria-hidden", "true");
    coordenada.textContent = texto;
    return coordenada;
}

function desenharPecas() {
    if (!tabuleiroAtual) {
        return;
    }

    const podeJogar = minhaVez() && !partidaFinalizada;
    elementos.tabuleiro.classList.toggle("tabuleiro-minha-vez", podeJogar);
    elementos.tabuleiro.classList.toggle("tabuleiro-com-selecao", podeJogar && pecaSelecionada !== null);

    for (let linha = 0; linha < 8; linha++) {
        for (let coluna = 0; coluna < 8; coluna++) {
            const casa = casas[chaveCasa(linha, coluna)];
            if (!casa || casa.classList.contains("casa-clara")) {
                continue;
            }

            const peca = tabuleiroAtual[linha] ? tabuleiroAtual[linha][coluna] : null;
            const assinatura = peca ? `${peca.cor}${peca.dama ? "-DAMA" : ""}` : "";
            let elementoPeca = casa.querySelector(".peca");

            if (casa.dataset.peca !== assinatura) {
                if (elementoPeca) {
                    elementoPeca.remove();
                    elementoPeca = null;
                }
                if (peca) {
                    elementoPeca = criarPeca(peca, casa.dataset.desenhada === "sim");
                    casa.appendChild(elementoPeca);
                }
                casa.dataset.peca = assinatura;
            }
            casa.dataset.desenhada = "sim";

            const selecionada = pecaSelecionada !== null
                && pecaSelecionada.linha === linha
                && pecaSelecionada.coluna === coluna;

            const destino = destinosPossiveis.find((item) => item.linha === linha && item.coluna === coluna) || null;

            casa.classList.toggle("casa-vazia", !peca);
            casa.classList.toggle("casa-selecionada", selecionada);
            casa.classList.toggle("casa-ultima", ultimaJogada.includes(chaveCasa(linha, coluna)));
            casa.classList.toggle("casa-jogada-possivel", destino !== null && !destino.captura);
            casa.classList.toggle("casa-jogada-captura", destino !== null && destino.captura);

            if (elementoPeca) {
                elementoPeca.classList.toggle("peca-selecionada", selecionada);
                elementoPeca.classList.toggle("peca-jogavel", podeJogar && peca.cor === minhaCor);
            }

            casa.setAttribute("aria-label", descreverCasa(linha, coluna, peca, selecionada, destino));
            casa.setAttribute("aria-pressed", selecionada ? "true" : "false");
        }
    }

    atualizarContagem();
}

function criarPeca(peca, animar) {
    const elementoPeca = document.createElement("span");
    elementoPeca.className = `peca ${peca.cor === "BRANCA" ? "peca-branca" : "peca-preta"}`;
    if (peca.dama) {
        elementoPeca.classList.add("peca-dama");
    }
    if (animar) {
        elementoPeca.classList.add("peca-chegando");
        elementoPeca.addEventListener("animationend", () => elementoPeca.classList.remove("peca-chegando"), { once: true });
    }
    elementoPeca.setAttribute("aria-hidden", "true");
    return elementoPeca;
}

function descreverCasa(linha, coluna, peca, selecionada, destino) {
    const nomeCasa = `${LETRAS_COLUNA[coluna]}${8 - linha}`;
    if (!peca) {
        const jogada = destino ? `, ${destino.captura ? "captura" : "jogada"} possível` : "";
        return `${nomeCasa}, vazia${jogada}`;
    }
    const tipo = peca.dama ? "dama" : "peça";
    const cor = peca.cor === "BRANCA" ? "branca" : "preta";
    return `${nomeCasa}, ${tipo} ${cor}${selecionada ? ", selecionada" : ""}`;
}

function atualizarContagem() {
    let brancas = 0;
    let pretas = 0;

    tabuleiroAtual.forEach((linha) => {
        (linha || []).forEach((peca) => {
            if (!peca) {
                return;
            }
            if (peca.cor === "BRANCA") {
                brancas++;
            } else if (peca.cor === "PRETA") {
                pretas++;
            }
        });
    });

    const minhas = minhaCor === "BRANCA" ? brancas : pretas;
    const dele = minhaCor === "BRANCA" ? pretas : brancas;
    elementos.jogadorVoce.querySelector(".jogador-pecas").textContent = minhas;
    elementos.jogadorAdversario.querySelector(".jogador-pecas").textContent = dele;
}

function clicarCasa(linha, coluna) {
    if (partidaFinalizada || !tabuleiroAtual) {
        return;
    }

    if (!minhaVez()) {
        mostrarMensagem("Não é sua vez", "erro");
        return;
    }

    if (jogadaEnviada) {
        return;
    }

    const peca = tabuleiroAtual[linha][coluna];

    if (peca && peca.cor === minhaCor) {
        selecionarPeca(linha, coluna);
        return;
    }

    if (peca) {
        mostrarMensagem("Essa peça é do adversário", "erro");
        return;
    }

    if (!pecaSelecionada) {
        mostrarMensagem("Escolha uma peça", "info");
        return;
    }

    enviarMovimento(linha, coluna);
}

function selecionarPeca(linha, coluna) {
    if (capturaObrigatoria) {
        if (capturaObrigatoria.linha !== linha || capturaObrigatoria.coluna !== coluna) {
            mostrarMensagem("Continue a captura com a peça destacada", "erro");
        }
        pecaSelecionada = { ...capturaObrigatoria };
        destinosPossiveis = calcularDestinos(pecaSelecionada.linha, pecaSelecionada.coluna);
        desenharPecas();
        return;
    }

    const mesmaPeca = pecaSelecionada && pecaSelecionada.linha === linha && pecaSelecionada.coluna === coluna;

    if (mesmaPeca) {
        pecaSelecionada = null;
        destinosPossiveis = [];
        mostrarMensagem("Escolha uma peça", "info");
    } else {
        const peca = pecaEm(linha, coluna);
        pecaSelecionada = { linha, coluna };
        destinosPossiveis = calcularDestinos(linha, coluna);

        if (destinosPossiveis.length === 0 && existeCapturaParaCor(peca.cor)) {
            mostrarMensagem("Existe uma captura obrigatória com outra peça", "aviso");
        } else if (destinosPossiveis.length === 0) {
            mostrarMensagem("Essa peça não tem jogadas possíveis", "aviso");
        } else {
            mostrarMensagem("Peça selecionada. Escolha o destino", "info");
        }
    }

    desenharPecas();
}

function enviarMovimento(linhaDestino, colunaDestino) {
    const enviado = enviar({
        codigo: "MOVIMENTO",
        jogadorId,
        salaId,
        linhaOrigem: pecaSelecionada.linha,
        colunaOrigem: pecaSelecionada.coluna,
        linhaDestino,
        colunaDestino
    });

    if (enviado) {
        jogadaEnviada = true;
    }
}

function atualizarTurno() {
    const vez = minhaVez();

    elementos.estadoPartida.classList.toggle("estado-minha-vez", vez);
    elementos.jogadorVoce.classList.toggle("jogador-ativo", vez);
    elementos.jogadorAdversario.classList.toggle("jogador-ativo", !vez);

    const textoAnterior = elementos.textoTurno.textContent;
    const textoNovo = vez ? "Sua vez" : `Vez de ${nomeAdversario}`;
    elementos.textoTurno.textContent = textoNovo;

    if (vez) {
        elementos.detalheTurno.textContent = capturaObrigatoria
            ? "Continue a captura com a mesma peça"
            : `Mova uma peça ${minhaCor === "BRANCA" ? "branca" : "preta"}`;
    } else {
        elementos.detalheTurno.textContent = capturaObrigatoria
            ? "O adversário continua a captura"
            : "Aguardando a jogada do adversário";
    }

    document.title = vez ? "Sua vez · Damas Online" : "Damas Online";

    if (textoAnterior !== textoNovo && !movimentoReduzido.matches && elementos.textoTurno.animate) {
        elementos.textoTurno.animate(
            [
                { opacity: 0, transform: "translateY(4px)" },
                { opacity: 1, transform: "none" }
            ],
            { duration: 200, easing: "ease-out" }
        );
    }
}

function mostrarMensagem(texto, tipo) {
    elementos.mensagemJogo.className = `mensagem-jogo mensagem-${tipo || "info"}`;
    elementos.mensagemJogo.textContent = texto;
}

function desistir() {
    if (partidaFinalizada) {
        return;
    }

    if (!window.confirm("Desistir da partida? O adversário será declarado vencedor.")) {
        return;
    }

    if (enviar({ codigo: "DESISTIR", jogadorId, salaId })) {
        desistiu = true;
        elementos.botaoDesistir.disabled = true;
    }
}

function finalizarPartida(dados) {
    partidaFinalizada = true;
    jogadaEnviada = false;
    pecaSelecionada = null;
    destinosPossiveis = [];
    capturaObrigatoria = null;

    if (dados.tabuleiro) {
        const corDeQuemJogou = jogadorAtual === jogadorId ? minhaCor : corAdversario();
        const casasAlteradas = calcularUltimaJogada(tabuleiroAtual, dados.tabuleiro, corDeQuemJogou);
        if (casasAlteradas.length > 0) {
            ultimaJogada = casasAlteradas;
        }
        tabuleiroAtual = dados.tabuleiro;
    }

    desenharPecas();

    const venci = !dados.conexaoPerdida && dados.vencedorId === jogadorId;
    const perdi = !dados.conexaoPerdida && !venci;

    elementos.estadoTurno.hidden = true;
    elementos.resultadoPartida.hidden = false;
    elementos.resultadoPartida.classList.toggle("resultado-vitoria", venci);
    elementos.simboloResultado.hidden = !venci;
    elementos.estadoPartida.classList.remove("estado-minha-vez");

    elementos.jogadorVoce.classList.remove("jogador-ativo");
    elementos.jogadorAdversario.classList.remove("jogador-ativo");
    elementos.jogadorVoce.classList.toggle("jogador-vencedor", venci);
    elementos.jogadorAdversario.classList.toggle("jogador-vencedor", perdi);

    if (dados.conexaoPerdida) {
        elementos.tituloResultado.textContent = "Partida interrompida";
        elementos.textoResultado.textContent = "Você foi desconectado.";
        elementos.detalheResultado.textContent = dados.mensagem;
    } else if (venci) {
        elementos.tituloResultado.textContent = "Vitória";
        elementos.textoResultado.textContent = "Você venceu a partida.";
        elementos.detalheResultado.textContent = detalheDoFim(dados.mensagem, venci);
    } else {
        elementos.tituloResultado.textContent = "Partida encerrada";
        elementos.textoResultado.textContent = `${nomeAdversario} venceu.`;
        elementos.detalheResultado.textContent = detalheDoFim(dados.mensagem, venci);
    }

    elementos.tabuleiro.classList.add("tabuleiro-encerrado");
    elementos.botaoDesistir.disabled = true;
    elementos.botaoNovaPartida.hidden = false;
    elementos.mensagemJogo.textContent = "";
    document.title = venci ? "Vitória · Damas Online" : "Partida encerrada · Damas Online";
}

function detalheDoFim(mensagem, venci) {
    if (desistiu && !venci) {
        return "Você desistiu.";
    }
    if (mensagem && mensagem !== MENSAGEM_FIM_PADRAO) {
        return `${mensagem}.`;
    }
    return venci
        ? `${nomeAdversario} ficou sem peças ou sem jogadas.`
        : "Você ficou sem peças ou sem jogadas.";
}

function novaPartida() {
    elementos.botaoNovaPartida.hidden = true;
    elementos.nomeEspera.textContent = nomeJogador;
    tabuleiroAtual = null;
    entrarNaFila();
}

function minhaVez() {
    return jogadorId !== null && jogadorAtual === jogadorId;
}

function corAdversario() {
    return minhaCor === "BRANCA" ? "PRETA" : "BRANCA";
}

function nomeDaCor(cor) {
    return cor === "BRANCA" ? "Brancas" : "Pretas";
}

function chaveCasa(linha, coluna) {
    return `${linha}-${coluna}`;
}

const DIRECOES = [[-1, -1], [-1, 1], [1, -1], [1, 1]];

function posicaoValida(linha, coluna) {
    return linha >= 0 && linha < 8 && coluna >= 0 && coluna < 8;
}

function pecaEm(linha, coluna) {
    return tabuleiroAtual[linha] ? tabuleiroAtual[linha][coluna] : null;
}

function capturasDaPeca(linha, coluna, peca) {
    const destinos = [];

    for (const [passoLinha, passoColuna] of DIRECOES) {
        let linhaAtual = linha + passoLinha;
        let colunaAtual = coluna + passoColuna;

        if (peca.dama) {
            while (posicaoValida(linhaAtual, colunaAtual) && !pecaEm(linhaAtual, colunaAtual)) {
                linhaAtual += passoLinha;
                colunaAtual += passoColuna;
            }
        }

        if (!posicaoValida(linhaAtual, colunaAtual)) {
            continue;
        }

        const alvo = pecaEm(linhaAtual, colunaAtual);
        if (!alvo || alvo.cor === peca.cor) {
            continue;
        }

        let linhaDestino = linhaAtual + passoLinha;
        let colunaDestino = colunaAtual + passoColuna;

        if (peca.dama) {
            while (posicaoValida(linhaDestino, colunaDestino) && !pecaEm(linhaDestino, colunaDestino)) {
                destinos.push({ linha: linhaDestino, coluna: colunaDestino, captura: true });
                linhaDestino += passoLinha;
                colunaDestino += passoColuna;
            }
        } else if (posicaoValida(linhaDestino, colunaDestino) && !pecaEm(linhaDestino, colunaDestino)) {
            destinos.push({ linha: linhaDestino, coluna: colunaDestino, captura: true });
        }
    }

    return destinos;
}

function movimentosSimplesDaPeca(linha, coluna, peca) {
    const destinos = [];
    const direcoes = peca.dama
        ? DIRECOES
        : peca.cor === "BRANCA" ? [[-1, -1], [-1, 1]] : [[1, -1], [1, 1]];

    for (const [passoLinha, passoColuna] of direcoes) {
        if (peca.dama) {
            let linhaAtual = linha + passoLinha;
            let colunaAtual = coluna + passoColuna;
            while (posicaoValida(linhaAtual, colunaAtual) && !pecaEm(linhaAtual, colunaAtual)) {
                destinos.push({ linha: linhaAtual, coluna: colunaAtual, captura: false });
                linhaAtual += passoLinha;
                colunaAtual += passoColuna;
            }
        } else {
            const linhaAtual = linha + passoLinha;
            const colunaAtual = coluna + passoColuna;
            if (posicaoValida(linhaAtual, colunaAtual) && !pecaEm(linhaAtual, colunaAtual)) {
                destinos.push({ linha: linhaAtual, coluna: colunaAtual, captura: false });
            }
        }
    }

    return destinos;
}

function existeCapturaParaCor(cor) {
    for (let linha = 0; linha < 8; linha++) {
        for (let coluna = 0; coluna < 8; coluna++) {
            const peca = pecaEm(linha, coluna);
            if (peca && peca.cor === cor && capturasDaPeca(linha, coluna, peca).length > 0) {
                return true;
            }
        }
    }
    return false;
}

function calcularDestinos(linha, coluna) {
    const peca = pecaEm(linha, coluna);
    if (!peca) {
        return [];
    }

    const capturas = capturasDaPeca(linha, coluna, peca);

    if (capturaObrigatoria || existeCapturaParaCor(peca.cor)) {
        return capturas;
    }

    return movimentosSimplesDaPeca(linha, coluna, peca);
}

elementos.formEntrada.addEventListener("submit", entrar);
elementos.campoNome.addEventListener("input", () => {
    elementos.campoNome.removeAttribute("aria-invalid");
    elementos.mensagemEntrada.textContent = "";
});
elementos.botaoSairFila.addEventListener("click", sairDaFila);
elementos.botaoDesistir.addEventListener("click", desistir);
elementos.botaoNovaPartida.addEventListener("click", novaPartida);

conectar();
