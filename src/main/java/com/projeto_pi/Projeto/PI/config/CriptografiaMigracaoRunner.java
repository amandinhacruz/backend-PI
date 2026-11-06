package com.projeto_pi.Projeto.PI.config;

import com.projeto_pi.Projeto.PI.security.CriptografiaUtil;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class CriptografiaMigracaoRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public CriptografiaMigracaoRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // Preparação das colunas de valores monetários.
        alterarParaTexto("investidores_mentores", "ticket_min");
        alterarParaTexto("investidores_mentores", "ticket_max");

// Preparação das colunas de dados das startups.
        alterarParaTexto("startups", "segmento");
        alterarParaTexto("startups", "estagio");
        alterarParaTexto("startups", "necessidades");
        alterarParaTexto("startups", "pitch");
        alterarParaTexto("startups", "canvas");

// Preparação das colunas de investidores e mentores.
        alterarParaTexto("investidores_mentores", "areas_interesse");
        alterarParaTexto("investidores_mentores", "estagios_pref");
        alterarParaTexto("investidores_mentores", "disponibilidade");

// Preparação das colunas de mensagens e agendamentos.
        alterarParaTexto("mensagens", "conteudo");
        alterarParaTexto("agendamentos", "assunto");


// MIGRAÇÃO DOS DADOS

// Dados das startups.
        migrarColuna("startups", "segmento");
        migrarColuna("startups", "estagio");
        migrarColuna("startups", "necessidades");
        migrarColuna("startups", "pitch");
        migrarColuna("startups", "canvas");

// Dados dos investidores e mentores.
        migrarColuna("investidores_mentores", "areas_interesse");
        migrarColuna("investidores_mentores", "ticket_min");
        migrarColuna("investidores_mentores", "ticket_max");
        migrarColuna("investidores_mentores", "estagios_pref");
        migrarColuna("investidores_mentores", "disponibilidade");

// Dados das mensagens e dos agendamentos.
        migrarColuna("mensagens", "conteudo");
        migrarColuna("agendamentos", "assunto");

    }

    private void alterarParaTexto(String tabela, String coluna) {

        if (!colunaExiste(tabela, coluna)) {
            return;
        }

        String sql = "ALTER TABLE " + tabela
                + " ALTER COLUMN " + coluna
                + " TYPE TEXT USING " + coluna + "::text";

        jdbcTemplate.execute(sql);
    }

    private void migrarColuna(String tabela, String coluna) {

        if (!colunaExiste(tabela, coluna)) {
            return;
        }

        String consulta = "SELECT id, " + coluna
                + " AS valor FROM " + tabela;

        List<Map<String, Object>> registros =
                jdbcTemplate.queryForList(consulta);

        for (Map<String, Object> registro : registros) {

            Object objeto = registro.get("valor");

            if (objeto == null) {
                continue;
            }

            String valor = objeto.toString();

            if (CriptografiaUtil.estaCriptografado(valor)) {
                continue;
            }

            String valorCriptografado =
                    CriptografiaUtil.criptografar(valor);

            Number id = (Number) registro.get("id");

            String atualizacao = "UPDATE " + tabela
                    + " SET " + coluna + " = ? WHERE id = ?";

            jdbcTemplate.update(
                    atualizacao,
                    valorCriptografado,
                    id.longValue()
            );
        }
    }

    private boolean colunaExiste(String tabela, String coluna) {

        Boolean existe = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = current_schema()
                    AND table_name = ?
                    AND column_name = ?
                )
                """,
                Boolean.class,
                tabela,
                coluna
        );

        return Boolean.TRUE.equals(existe);
    }
}
