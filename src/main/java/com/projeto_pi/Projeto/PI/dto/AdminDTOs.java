package com.projeto_pi.Projeto.PI.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Contratos do painel administrativo (Adm/Pages no front). */
public final class AdminDTOs {
    private AdminDTOs() {}

    /** Item da fila de moderação. Não expõe senha, CPF/CNPJ nem arquivos. */
    public record CadastroAdmin(
            Long id, String tipo, String nome, String responsavel,
            String email, String status, LocalDateTime enviadoEm) {}

    public record Engajamento(String rotulo, int percentual) {}

    public record Metricas(
            long startups, long startupsNoMes,
            long investidores, long investidoresNoMes,
            long pendentes,
            long conexoes, long conversas, long mensagens,
            long reunioes, long reunioesAceitas,
            List<Engajamento> engajamento,
            long matches, long pedidosConexao, long pedidosPendentes, long recomendacoesPassadas,
            long matchesComIa, long avaliacoes, Double notaMedia,
            long startupsAprovadas, long investidoresAprovados, long reunioesAguardando,
            long matchesComReuniao) {}

    public record Auditoria(Long id, String acao, String alvo, String ator, LocalDateTime data) {}
}
