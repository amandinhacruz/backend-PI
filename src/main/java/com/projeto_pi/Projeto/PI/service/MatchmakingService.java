package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.MatchResponseDTO;
import com.projeto_pi.Projeto.PI.entity.InvestidorMentor;
import com.projeto_pi.Projeto.PI.entity.Startup;
import com.projeto_pi.Projeto.PI.repository.InvestidorMentorRepository;
import com.projeto_pi.Projeto.PI.repository.StartupRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MatchmakingService {

    private final StartupRepository startupRepository;
    private final InvestidorMentorRepository investidorMentorRepository;

    public MatchmakingService(
            StartupRepository startupRepository,
            InvestidorMentorRepository investidorMentorRepository) {

        this.startupRepository = startupRepository;
        this.investidorMentorRepository = investidorMentorRepository;
    }

    public List<MatchResponseDTO> encontrarMatches(Long startupId) {

        Startup startup = startupRepository.findById(startupId)
                .orElseThrow(() ->
                        new RuntimeException("Startup não encontrada"));

        List<InvestidorMentor> investidores =
                investidorMentorRepository.findAll();

        List<MatchResponseDTO> resultados = new ArrayList<>();

        for (InvestidorMentor investidor : investidores) {

            int pontuacao = calcularAfinidade(startup, investidor);

            String nivel = definirNivel(pontuacao);

            MatchResponseDTO resultado = new MatchResponseDTO(
                    startup.getId(),
                    startup.getNome(),
                    investidor.getId(),
                    investidor.getTipo().name(),
                    pontuacao,
                    nivel
            );

            resultados.add(resultado);
        }

        resultados.sort(
                (a, b) -> Integer.compare(
                        b.getPontuacao(),
                        a.getPontuacao()
                )
        );

        return resultados;
    }

    private int calcularAfinidade(
            Startup startup,
            InvestidorMentor investidor) {

        int pontuacao = 0;

        // Critério 1: segmento x área de interesse
        if (textoCompativel(
                startup.getSegmento(),
                investidor.getAreasInteresse())) {

            pontuacao += 40;
        }

        // Critério 2: estágio x preferência de estágio
        if (textoCompativel(
                startup.getEstagio(),
                investidor.getEstagiosPref())) {

            pontuacao += 30;
        }

        // Critério 3: necessidades x área de interesse
        if (textoCompativel(
                startup.getNecessidades(),
                investidor.getAreasInteresse())) {

            pontuacao += 30;
        }

        return pontuacao;
    }

    private boolean textoCompativel(
            String textoStartup,
            String textoInvestidor) {

        if (textoStartup == null || textoInvestidor == null) {
            return false;
        }

        if (textoStartup.isBlank() || textoInvestidor.isBlank()) {
            return false;
        }

        String startupNormalizado =
                textoStartup.toLowerCase().trim();

        String investidorNormalizado =
                textoInvestidor.toLowerCase().trim();

        String[] termos =
                startupNormalizado.split("[,;\\s]+");

        for (String termo : termos) {

            if (termo.length() >= 3 &&
                    investidorNormalizado.contains(termo)) {

                return true;
            }
        }

        return false;
    }

    private String definirNivel(int pontuacao) {

        if (pontuacao >= 70) {
            return "ALTA";
        }

        if (pontuacao >= 40) {
            return "MEDIA";
        }

        return "BAIXA";
    }
}
