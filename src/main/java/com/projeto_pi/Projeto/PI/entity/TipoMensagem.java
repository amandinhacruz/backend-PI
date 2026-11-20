package com.projeto_pi.Projeto.PI.entity;

public enum TipoMensagem {
    /** Mensagem escrita por um dos participantes. */
    TEXTO,
    /** Convite de reunião enviado pelo investidor (ligado a um Agendamento). */
    CONVITE,
    /** Aviso automático: reunião confirmada, recusada ou cancelada. */
    SISTEMA
}
