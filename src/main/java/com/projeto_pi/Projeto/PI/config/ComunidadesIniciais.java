package com.projeto_pi.Projeto.PI.config;

import com.projeto_pi.Projeto.PI.entity.Comunidade;
import com.projeto_pi.Projeto.PI.repository.ComunidadeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * As seis comunidades da plataforma, criadas ao iniciar o servidor (em qualquer banco, inclusive
 * produção). Só insere as que ainda não existem, pelo slug; não altera as já criadas.
 */
@Component
public class ComunidadesIniciais implements ApplicationRunner {

    record Modelo(String slug, String nome, String descricao) {
    }

    static final List<Modelo> COMUNIDADES = List.of(
            new Modelo("desenvolvimento", "Desenvolvimento & Produto",
                    "Engenharia, arquitetura, MVP e boas práticas de produto. Startups mostram como constroem; "
                            + "investidores entendem a maturidade técnica por trás de cada solução."),
            new Modelo("ciberseguranca", "Cibersegurança & LGPD",
                    "Proteção de dados, LGPD, privacidade e resposta a incidentes. Segurança como critério de "
                            + "confiança para quem constrói e para quem investe."),
            new Modelo("ia-dados", "Inteligência Artificial & Dados",
                    "IA generativa, ciência de dados e automação aplicadas a negócios reais. Casos de uso, "
                            + "ferramentas e tendências que estão mudando o mercado."),
            new Modelo("captacao", "Captação & Venture Capital",
                    "Rodadas, valuation, term sheets, editais e fomento. O ponto de encontro para falar de "
                            + "investimento do pitch ao fechamento."),
            new Modelo("impacto-esg", "Impacto & ESG",
                    "Negócios de impacto, sustentabilidade e governança. Iniciativas que unem retorno "
                            + "financeiro, impacto social e responsabilidade ambiental."),
            new Modelo("ecossistema", "Ecossistema & Eventos",
                    "Eventos, hackathons, programas de aceleração e movimentos do ecossistema. Compartilhe "
                            + "notícias, conquistas e o que está acontecendo na sua empresa."));

    private final ComunidadeRepository comunidades;

    public ComunidadesIniciais(ComunidadeRepository comunidades) {
        this.comunidades = comunidades;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (int i = 0; i < COMUNIDADES.size(); i++) {
            Modelo modelo = COMUNIDADES.get(i);
            if (comunidades.findBySlug(modelo.slug()).isEmpty()) {
                comunidades.save(new Comunidade(modelo.slug(), modelo.nome(), modelo.descricao(), i + 1));
            }
        }
    }
}
