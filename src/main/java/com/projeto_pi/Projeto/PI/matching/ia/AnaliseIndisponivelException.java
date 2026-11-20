package com.projeto_pi.Projeto.PI.matching.ia;

/** O analista generativo não pôde responder (rede, limite, recusa, resposta inválida). */
public class AnaliseIndisponivelException extends RuntimeException {

    public AnaliseIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public AnaliseIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
