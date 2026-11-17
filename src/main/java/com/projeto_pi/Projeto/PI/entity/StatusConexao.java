package com.projeto_pi.Projeto.PI.entity;

/** Ciclo de vida de uma conexão entre investidor e startup. */
public enum StatusConexao {
    /** O investidor pediu conexão; aguarda a resposta da startup. */
    SOLICITADA,
    /** A startup firmou a conexão: é o "match" contado no RF09. */
    FIRMADA,
    /** A startup negou o pedido. */
    NEGADA,
    /** O investidor passou a startup; ela sai da fila dele e a startup não é avisada. */
    DISPENSADA
}
