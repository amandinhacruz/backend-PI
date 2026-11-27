package com.projeto_pi.Projeto.PI.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Contratos da aba "Crescimento". Meses no formato "yyyy-MM".
 * mode: COM_FATURAMENTO ou SEM_FATURAMENTO; revenueCriterion: FATURADA ou RECEBIDA;
 * businessModel: B2B, B2C, B2B2C, MARKETPLACE ou OUTRO; milestoneStatus: NAO_INICIADO, EM_ANDAMENTO ou CONCLUIDO.
 */
public final class CrescimentoDTOs {

    private CrescimentoDTOs() {
    }

    public record Settings(String mode, String revenueCriterion, String businessModel, List<String> indicators,
                           String milestone, String milestoneDeadline, LocalDateTime updatedAt) {
    }

    public record SettingsRequest(String mode, String revenueCriterion, String businessModel, List<String> indicators,
                                  String milestone, String milestoneDeadline) {
    }

    public record UpdateRequest(BigDecimal revenue, BigDecimal directCosts, BigDecimal operatingExpenses,
                                BigDecimal expenses, BigDecimal cash,
                                Boolean newFunding, BigDecimal fundingAmount, Boolean newDebt, BigDecimal debtAmount,
                                Map<String, BigDecimal> indicators, String milestoneStatus, Integer milestoneProgress,
                                String comment) {
    }

    /**
     * Atualização de um mês com os indicadores calculados.
     * grossProfit = receita - custos diretos; operatingResult = lucro bruto - despesas operacionais
     * (sem faturamento: - gastos); burn = quanto o caixa encolhe por mês pela operação;
     * runwayMonths = caixa / burn (nulo quando não há queima).
     */
    public record MonthlyUpdate(String month, String mode, String revenueCriterion,
                                BigDecimal revenue, BigDecimal directCosts, BigDecimal operatingExpenses,
                                BigDecimal grossProfit, BigDecimal grossMarginPercent, BigDecimal revenueGrowthPercent,
                                BigDecimal expenses, BigDecimal operatingResult, BigDecimal burn, BigDecimal cash,
                                BigDecimal runwayMonths,
                                boolean newFunding, BigDecimal fundingAmount, boolean newDebt, BigDecimal debtAmount,
                                Map<String, BigDecimal> indicators, String milestone, String milestoneStatus,
                                Integer milestoneProgress, String comment,
                                LocalDateTime submittedAt, LocalDateTime editedAt) {
    }

    /**
     * Painel completo. {@code settings} nulo: a startup ainda não configurou.
     * {@code nextMonth}/{@code pendingMonths}: só para a própria startup (o que falta enviar).
     */
    public record Dashboard(Long startupId, String startupName, boolean owner, Settings settings,
                            List<MonthlyUpdate> updates, String nextMonth, List<String> pendingMonths) {
    }
}
