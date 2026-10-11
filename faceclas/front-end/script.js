"use strict";

/* =================================================================
   1. CONFIGURAÇÕES E DADOS GERAIS
   ================================================================= */

// Atalho para pegar elementos da tela pelo ID mais rápido
const porId = (id) => document.getElementById(id);

// Guarda as informações principais do sistema e do aluno logado
const estado = {
    aluno: null,
    csrf: "token-teste",
    historico: [],
    hoje: null,
    historicoCarregado: false,
    sessaoConhecida: true,
    ocupado: false,
    tela: "tela-login",
    acaoFacial: null,
    stream: null,
    cameraVersao: 0,
    cameraPronta: false,
};

let consultaHistorico = null;


/* =================================================================
   2. AVISOS E MENSAGENS NA TELA
   ================================================================= */

// Cria um tipo de erro personalizado para o sistema
class ErroRequisicao extends Error {
    constructor(mensagem, status = 0) {
        super(mensagem);
        this.status = status;
    }
}

// Mostra mensagens de erro, sucesso ou aviso na tela para o usuário
function mensagem(id, texto = "", tipo = "erro") {
    const elemento = porId(id);
    if (!elemento) return;
    elemento.textContent = texto;
    elemento.style.color = tipo === "sucesso" ? "var(--success)"
        : tipo === "info" ? "var(--subtext)" : "var(--danger)";
}


/* =================================================================
   3. FORMATAÇÃO, VALIDAÇÃO E DESTAQUE DE ERROS NOS CAMPOS
   ================================================================= */

// Deixa o RA maiúsculo e remove espaços ou traços
function normalizarRA(valor) {
    return (valor || "").replace(/[-\s]/g, "").toUpperCase();
}

// Confere se o RA digitado está no formato correto
function validarRA(valor) {
    const ra = normalizarRA(valor);
    if (!/^[0-9A-Z]{5,20}$/.test(ra)) {
        throw new Error("Informe um RA válido com 5 a 20 letras ou números.");
    }
    return ra;
}

// Muda o formato da data de ano-mes-dia para dia/mes/ano
function dataBrasileira(data) {
    if (!data) return "";
    const partes = data.split("-");
    return partes.length === 3 ? partes.reverse().join("/") : data;
}

// Deixa o horário bonitinho mostrando apenas horas e minutos (Ex: 08:30)
function horaCurta(hora) {
    if (!hora) return "";
    const partes = String(hora).split(":");
    return partes.length >= 2 ? partes[0].padStart(2, "0") + ":" + partes[1] : String(hora);
}

// Limpa todas as bordas vermelhas dos campos de cadastro
function limparErrosCadastro() {
    const idsCampos = [
        "cadastro-nome", "cadastro-email", "cadastro-ra", "cadastro-senha",
        "responsavel-nome", "responsavel-telefone", "responsavel-email"
    ];
    idsCampos.forEach(id => {
        const campo = porId(id);
        if (campo) campo.style.borderColor = "";
    });
}

// Pinta a borda de um campo específico de vermelho para indicar erro
function destacarErro(id) {
    const campo = porId(id);
    if (campo) campo.style.borderColor = "var(--danger)";
}


/* =================================================================
   4. SIMULAÇÃO DO SERVIDOR (MODO DE TESTE)
   ================================================================= */

// Simula respostas falsas do servidor para o site funcionar sem precisar ligar o Python
async function api(caminho, dados) {
    await new Promise(resolve => setTimeout(resolve, 200)); // Simula um tempinho de espera
    
    if (caminho === "/sessao") {
        return { csrf_token: "token-teste", aluno: estado.aluno };
    }
    if (caminho === "/login") {
        return { 
            csrf_token: "token-teste", 
            aluno: estado.aluno || { id: 999, nome: "Aluno Teste", rosto_cadastrado: true } 
        };
    }
    if (caminho === "/alunos") {
        return { 
            csrf_token: "token-teste", 
            aluno: dados 
        };
    }
    if (caminho === "/presencas") {
        const dataHoje = new Date().toISOString().split('T')[0];
        return { hoje: estado.hoje || dataHoje, presencas: estado.historico };
    }
    if (caminho === "/presenca" || caminho === "/rosto") {
        const dataHoje = new Date().toISOString().split('T')[0];
        const horaAtual = new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
        
        if (dados && dados.tipo) {
            estado.historico.push({
                tipo: dados.tipo,
                data: dataHoje,
                horario: horaAtual,
                motivo_saida: dados.motivo_saida || "",
                notificacao: "enviado"
            });
        }
        return { mensagem: "Operação realizada com sucesso!", notificacao: "enviado" };
    }
    if (caminho === "/logout") {
        return { csrf_token: "token-teste" };
    }
    return { csrf_token: "token-teste" };
}


/* =================================================================
   5. CONTROLO DA CÂMARA E DAS TELAS
   ================================================================= */

// Desliga a câmara e limpa a memória dela
function pararCamera() {
    estado.cameraVersao += 1;
    estado.cameraPronta = false;
    if (estado.stream) estado.stream.getTracks().forEach((track) => track.stop());
    estado.stream = null;
    const camera = porId("camera");
    if (camera && camera.srcObject) {
        camera.srcObject.getTracks().forEach((track) => track.stop());
        camera.srcObject = null;
    }
    const canvas = porId("canvas-rosto");
    if (canvas) {
        canvas.width = 0;
        canvas.height = 0;
    }
}

// Mostra apenas a tela que a gente quer e esconde as outras
function mostrarTela(id) {
    if (id !== "tela-facial") pararCamera();
    ["tela-login", "tela-cadastro", "tela-facial", "tela-principal"].forEach((tela) => {
        const el = porId(tela);
        if (el) el.classList.toggle("escondida", tela !== id);
    });
    estado.tela = id;
    atualizarBotoes();
}

// Separa apenas os registos feitos no dia de hoje
function registrosHoje() {
    return estado.historico.filter((registro) => registro.data === estado.hoje);
}

// Libera ou bloqueia os botões dependendo do que o aluno está a fazer
function atualizarBotoes() {
    const bloqueado = estado.ocupado;
    ["btn-login", "btn-cadastrar"].forEach((id) => {
        const el = porId(id);
        if (el) el.disabled = bloqueado || !estado.sessaoConhecida;
    });
    ["btn-primeiro-acesso", "btn-voltar-login", "btn-atualizar-login",
        "btn-sair", "btn-cancelar-facial", "btn-reabrir-camera",
        "btn-atualizar", "btn-confirmar-saida"].forEach((id) => {
        const el = porId(id);
        if (el) el.disabled = bloqueado;
    });
    const btnFacial = porId("btn-cadastrar-facial");
    if (btnFacial) btnFacial.disabled = bloqueado || !estado.cameraPronta;
    
    const registros = registrosHoje();
    const entrada = registros.some((registro) => registro.tipo === "entrada");
    const saida = registros.some((registro) => registro.tipo === "saida");
    const indisponivel = bloqueado || !estado.aluno || !estado.historicoCarregado;
    
    const btnEntrada = porId("btn-entrada");
    const btnSaida = porId("btn-saida");
    if (btnEntrada) btnEntrada.disabled = indisponivel || entrada;
    if (btnSaida) btnSaida.disabled = indisponivel || !entrada || saida;
}

// Executa uma ação na tela mostrando um aviso de "carregando" se precisar
async function executar(idMensagem, acao) {
    if (estado.ocupado) return;
    estado.ocupado = true;
    atualizarBotoes();
    mensagem(idMensagem);
    try {
        await acao();
    } catch (erro) {
        const destino = {
            "tela-login": "mensagem-login",
            "tela-cadastro": "mensagem-cadastro",
            "tela-facial": "mensagem-facial",
            "tela-principal": "mensagem-principal",
        }[estado.tela] || idMensagem;
        mensagem(destino, erro.message || "Não foi possível concluir. Tente novamente.");
    } finally {
        estado.ocupado = false;
        atualizarBotoes();
    }
}


/* =================================================================
   6. LOGIN, HISTÓRICO E RECONHECIMENTO FACIAL
   ================================================================= */

// Desconecta o aluno e volta para a tela de login
function limparSessao() {
    estado.aluno = null;
    estado.sessaoConhecida = true;
    estado.historico = [];
    estado.hoje = null;
    estado.historicoCarregado = false;
    estado.acaoFacial = null;
    if (porId("login-senha")) porId("login-senha").value = "";
    if (porId("cadastro-senha")) porId("cadastro-senha").value = "";
    mostrarTela("tela-login");
}

// Atualiza o nome do aluno e a lista de entradas/saídas na tela principal
function atualizarStatus() {
    const nomeAluno = porId("nome-aluno");
    if (nomeAluno) nomeAluno.textContent = estado.aluno ? estado.aluno.nome : "Aluno";
    
    const hoje = registrosHoje();
    for (const tipo of ["entrada", "saida"]) {
        const statusEl = porId("status-" + tipo);
        if (statusEl) {
            const registro = hoje.find((item) => item.tipo === tipo);
            statusEl.textContent = !estado.historicoCarregado
                ? "Atualize os registros" : registro
                    ? "Registrada às " + horaCurta(registro.horario) : "Ainda não registrada";
        }
    }
    
    const lista = porId("lista-historico");
    if (!lista) return;
    lista.replaceChildren();
    
    const notificacoes = {
        pendente: "Aviso por e-mail na fila.",
        enviando: "Aviso por e-mail em processamento.",
        enviado: "Aviso aceito pelo servidor de e-mail.",
        falhou: "Não foi possível enviar o aviso por e-mail. Avise a equipe.",
    };
    
    if (!estado.historicoCarregado || !estado.historico.length) {
        const item = document.createElement("p");
        item.textContent = estado.historicoCarregado
            ? "Nenhum registro realizado." : "Não foi possível carregar os registros. Clique em Atualizar.";
        lista.appendChild(item);
    } else {
        estado.historico.forEach((registro) => {
            const item = document.createElement("p");
            item.className = "historico-item";
            const tipo = registro.tipo === "entrada" ? "Entrada" : "Saída";
            let texto = tipo + " registrada em " + dataBrasileira(registro.data)
                + " às " + horaCurta(registro.horario) + ".";
            if (registro.motivo_saida) texto += " Motivo: " + registro.motivo_saida + ".";
            if (notificacoes[registro.notificacao]) texto += " " + notificacoes[registro.notificacao];
            item.textContent = texto;
            lista.appendChild(item);
        });
    }
    atualizarBotoes();
}

// Puxa do servidor o histórico de dias anteriores do aluno
async function carregarHistorico() {
    if (!estado.aluno) return;
    if (consultaHistorico) return consultaHistorico;
    const alunoId = estado.aluno.id;
    consultaHistorico = (async () => {
        try {
            const retorno = await api("/presencas");
            estado.historico = retorno.presencas;
            estado.hoje = retorno.hoje;
            estado.historicoCarregado = true;
        } catch (erro) {
            if (estado.aluno && estado.aluno.id === alunoId) estado.historicoCarregado = false;
            throw erro;
        } finally {
            if (estado.aluno && estado.aluno.id === alunoId) atualizarStatus();
        }
    })();
    try {
        await consultaHistorico;
    } finally {
        consultaHistorico = null;
    }
}

// Vai para a tela principal ou pede o cadastro do rosto se não tiver
async function irParaConta() {
    if (!estado.aluno.rosto_cadastrado) {
        await prepararFacial("cadastro");
        return;
    }
    mostrarTela("tela-principal");
    await carregarHistorico();
}

// Liga a câmara do telemóvel ou computador
async function abrirCamera() {
    pararCamera();
    const versao = estado.cameraVersao;
    mensagem("mensagem-facial", "Abrindo a câmera...", "info");
    atualizarBotoes();
    try {
        const stream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: "user", width: { ideal: 640 }, height: { ideal: 480 } },
            audio: false,
        });
        if (estado.tela !== "tela-facial" || versao !== estado.cameraVersao) {
            stream.getTracks().forEach((track) => track.stop());
            return;
        }
        estado.stream = stream;
        const camera = porId("camera");
        if (!camera) return;
        camera.srcObject = stream;
        await camera.play();
        estado.cameraPronta = true;
        mensagem("mensagem-facial", "Câmera pronta. Deixe somente seu rosto na imagem.", "sucesso");
    } catch {
        if (versao !== estado.cameraVersao) return;
        pararCamera();
        mensagem("mensagem-facial", "Câmera simulada ou indisponível. Clique em cadastrar/confirmar para prosseguir.", "info");
        estado.cameraPronta = true; 
    } finally {
        atualizarBotoes();
    }
}

// Prepara a tela de reconhecimento facial (para entrar, sair ou cadastrar)
async function prepararFacial(tipo, motivo = "") {
    estado.acaoFacial = { tipo, motivo };
    const tituloFacial = porId("titulo-facial");
    const subtituloFacial = porId("subtitulo-facial");
    const btnCadastrarFacial = porId("btn-cadastrar-facial");
    const btnCancelarFacial = porId("btn-cancelar-facial");

    if (tituloFacial) tituloFacial.textContent = tipo === "cadastro" ? "Cadastro facial" : "Confirmar " + tipo;
    if (subtituloFacial) subtituloFacial.textContent = "Modo de teste ativado.";
    if (btnCadastrarFacial) btnCadastrarFacial.textContent = "Confirmar";
    if (btnCancelarFacial) btnCancelarFacial.textContent = "Cancelar";
    
    mostrarTela("tela-facial");
    void abrirCamera();
}

// Tira a foto (simulada para testes)
function capturarFoto() {
    return "data:image/jpeg;base64,mock";
}

// Pega a localização do GPS (simulada para testes)
async function localizar() {
    return { latitude: 0, longitude: 0, precisao: 1 };
}

// Sai da conta do aluno
async function sair() {
    pararCamera();
    limparSessao();
    mensagem("mensagem-login", "Você saiu da conta.", "info");
}


/* =================================================================
   7. BOTÕES E AÇÕES DO UTILIZADOR (CLICKS)
   ================================================================= */

// Atalho de teste para pular a tela de login direto para o painel
const btnAtalhoTeste = porId("btn-atalho-teste");
if (btnAtalhoTeste) {
    btnAtalhoTeste.addEventListener("click", () => {
        const dataHoje = new Date().toISOString().split('T')[0];
        estado.aluno = { id: 999, nome: "Aluno Teste", rosto_cadastrado: true };
        estado.historicoCarregado = true;
        estado.hoje = dataHoje;
        estado.historico = [];
        mostrarTela("tela-principal");
        atualizarStatus();
    });
}

// Botão de ir para a tela de criar conta (Primeiro acesso)
const btnPrimeiroAcesso = porId("btn-primeiro-acesso");
if (btnPrimeiroAcesso) {
    btnPrimeiroAcesso.addEventListener("click", () => {
        const cadRa = porId("cadastro-ra");
        const logRa = porId("login-ra");
        const cadSenha = porId("cadastro-senha");
        if (cadRa && logRa) cadRa.value = normalizarRA(logRa.value);
        if (cadSenha) cadSenha.value = "";
        limparErrosCadastro();
        mensagem("mensagem-cadastro");
        mostrarTela("tela-cadastro");
    });
}

// Botão de voltar para a tela de login a partir do cadastro
const btnVoltarLogin = porId("btn-voltar-login");
if (btnVoltarLogin) {
    btnVoltarLogin.addEventListener("click", () => {
        const cadSenha = porId("cadastro-senha");
        if (cadSenha) cadSenha.value = "";
        limparErrosCadastro();
        mostrarTela("tela-login");
    });
}

// Botão de fazer login
const btnLogin = porId("btn-login");
if (btnLogin) {
    btnLogin.addEventListener("click", () => {
        void executar("mensagem-login", async () => {
            estado.aluno = { id: 999, nome: "Aluno Teste", rosto_cadastrado: true };
            await irParaConta();
        });
    });
}

// Botão de criar conta com validação que pinta de vermelho os campos incorretos
const btnCadastrar = porId("btn-cadastrar");
if (btnCadastrar) {
    btnCadastrar.addEventListener("click", () => {
        void executar("mensagem-cadastro", async () => {
            limparErrosCadastro(); // Limpa marcações vermelhas anteriores

            // Pega os valores preenchidos nos inputs
            const nome = porId("cadastro-nome") ? porId("cadastro-nome").value.trim() : "";
            const email = porId("cadastro-email") ? porId("cadastro-email").value.trim() : "";
            const raInput = porId("cadastro-ra") ? porId("cadastro-ra").value : "";
            const senha = porId("cadastro-senha") ? porId("cadastro-senha").value : "";
            const nomeResp = porId("responsavel-nome") ? porId("responsavel-nome").value.trim() : "";
            const telResp = porId("responsavel-telefone") ? porId("responsavel-telefone").value.replace(/\D/g, "") : "";
            const emailResp = porId("responsavel-email") ? porId("responsavel-email").value.trim() : "";

            // Validações que destacam o campo exato em vermelho se falhar
            if (!nome || nome.length < 3) {
                destacarErro("cadastro-nome");
                throw new Error("Preencha o nome completo do aluno (mínimo de 3 letras).");
            }
            if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                destacarErro("cadastro-email");
                throw new Error("Preencha um e-mail válido para o aluno.");
            }
            try {
                validarRA(raInput);
            } catch (erroRa) {
                destacarErro("cadastro-ra");
                throw erroRa;
            }
            if (!nomeResp || nomeResp.length < 3) {
                destacarErro("responsavel-nome");
                throw new Error("Preencha o nome do responsável.");
            }
            if (!telResp || !/^\d{10,11}$/.test(telResp)) {
                destacarErro("responsavel-telefone");
                throw new Error("Informe o telefone do responsável com DDD (10 ou 11 dígitos).");
            }
            if (!emailResp || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailResp)) {
                destacarErro("responsavel-email");
                throw new Error("Preencha um e-mail válido para o responsável.");
            }
            if (!senha || senha.trim().length < 8 || senha.length > 128) {
                destacarErro("cadastro-senha");
                throw new Error("A senha precisa ter entre 8 e 128 caracteres.");
            }

            // Se tudo estiver certo, envia os dados
            const dados = { nome, email, RA: normalizarRA(raInput), senha, nome_responsavel: nomeResp, telefone_responsavel: telResp, email_responsavel: emailResp };
            const retorno = await api("/alunos", dados);
            
            if (!retorno.aluno) throw new Error("Não foi possível concluir o cadastro.");
            
            estado.aluno = { id: 999, nome: dados.nome, rosto_cadastrado: false };
            if (porId("cadastro-senha")) porId("cadastro-senha").value = "";
            limparErrosCadastro();
            
            await irParaConta();
        });
    });
}

// Botão de registrar entrada
const btnEntrada = porId("btn-entrada");
if (btnEntrada) {
    btnEntrada.addEventListener("click", () => {
        void executar("mensagem-principal", async () => {
            await carregarHistorico();
            await prepararFacial("entrada");
        });
    });
}

// Botão de registrar saída (abre o campo para digitar o motivo)
const btnSaida = porId("btn-saida");
if (btnSaida) {
    btnSaida.addEventListener("click", () => {
        void executar("mensagem-principal", async () => {
            await carregarHistorico();
            const campoMotivo = porId("campo-motivo-saida");
            if (campoMotivo) campoMotivo.classList.remove("escondida");
            mensagem("mensagem-principal", "Informe o motivo e confirme a saída.", "info");
        });
    });
}

// Confirma a saída antecipada
const btnConfirmarSaida = porId("btn-confirmar-saida");
if (btnConfirmarSaida) {
    btnConfirmarSaida.addEventListener("click", () => {
        const motivoInput = porId("motivo-saida");
        const motivo = motivoInput ? motivoInput.value.trim() : "";
        void prepararFacial("saida", motivo);
    });
}

// Botão para confirmar o reconhecimento facial
const btnCadastrarFacial = porId("btn-cadastrar-facial");
if (btnCadastrarFacial) {
    btnCadastrarFacial.addEventListener("click", () => {
        void executar("mensagem-facial", async () => {
            const acao = estado.acaoFacial || { tipo: "entrada" };
            await api("/presenca", { tipo: acao.tipo, motivo_saida: acao.motivo || "" });
            mostrarTela("tela-principal");
            const campoMotivo = porId("campo-motivo-saida");
            if (campoMotivo) campoMotivo.classList.add("escondida");
            await carregarHistorico();
            mensagem("mensagem-principal", "Ação registrada com sucesso!", "sucesso");
        });
    });
}

// Botão para cancelar a tela da câmara
const btnCancelarFacial = porId("btn-cancelar-facial");
if (btnCancelarFacial) {
    btnCancelarFacial.addEventListener("click", () => {
        mostrarTela("tela-principal");
        estado.acaoFacial = null;
    });
}

// Botão de sair da conta
const btnSair = porId("btn-sair");
if (btnSair) btnSair.addEventListener("click", () => { void executar("mensagem-principal", sair); });

// Botão para atualizar a lista de registros manualmente
const btnAtualizar = porId("btn-atualizar");
if (btnAtualizar) {
    btnAtualizar.addEventListener("click", () => {
        void executar("mensagem-principal", async () => {
            await carregarHistorico();
            mensagem("mensagem-principal", "Registros atualizados.", "sucesso");
        });
    });
}


/* =================================================================
   8. LIGAR O SISTEMA
   ================================================================= */

// Executado automaticamente assim que a página abre para preparar tudo
void executar("mensagem-login", async () => {
    estado.sessaoConhecida = true;
    estado.historicoCarregado = true;
    atualizarBotoes();
});