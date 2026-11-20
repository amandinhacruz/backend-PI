package com.projeto_pi.Projeto.PI.matching;

import com.projeto_pi.Projeto.PI.entity.InvestidorMentor;
import com.projeto_pi.Projeto.PI.matching.Criterios.Faixa;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** O que o investidor declarou no cadastro e que o matchmaking usa. */
public record PreferenciasInvestidor(
        List<String> segmentos,
        List<String> estagios,
        Faixa ticket,
        String cidade,
        String uf,
        String sobre,
        String carreira,
        String busca) {

    public static PreferenciasInvestidor de(InvestidorMentor perfil) {
        return new PreferenciasInvestidor(
                Criterios.lista(perfil.getAreasInteresse()),
                Criterios.lista(perfil.getEstagiosPref()),
                new Faixa(perfil.getTicketMin(), perfil.getTicketMax()),
                perfil.getCity(),
                perfil.getState(),
                perfil.getAboutInvestor(),
                perfil.getCareer(),
                perfil.getLookingFor());
    }

    /** Texto livre da tese, comparado com o pitch das startups. */
    public String textoTese() {
        return Stream.of(busca, sobre, carreira, String.join(" ", segmentos))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }
}
