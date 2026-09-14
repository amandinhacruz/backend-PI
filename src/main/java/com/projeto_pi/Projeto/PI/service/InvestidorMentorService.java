package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.entity.InvestidorMentor;
import com.projeto_pi.Projeto.PI.repository.InvestidorMentorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvestidorMentorService {

    private final InvestidorMentorRepository repository;

    public InvestidorMentorService(InvestidorMentorRepository repository) {
        this.repository = repository;
    }

    public InvestidorMentor cadastrar(InvestidorMentor investidorMentor) {
        return repository.save(investidorMentor);
    }

    public List<InvestidorMentor> listar() {
        return repository.findAll();
    }

    public InvestidorMentor buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Investidor/Mentor não encontrado"
                ));
    }

    public InvestidorMentor atualizar(
            Long id,
            InvestidorMentor investidorMentor
    ) {
        InvestidorMentor existente = buscarPorId(id);

        existente.setTipo(investidorMentor.getTipo());
        existente.setAreasInteresse(investidorMentor.getAreasInteresse());
        existente.setTicketMin(investidorMentor.getTicketMin());
        existente.setTicketMax(investidorMentor.getTicketMax());
        existente.setEstagiosPref(investidorMentor.getEstagiosPref());
        existente.setDisponibilidade(investidorMentor.getDisponibilidade());

        return repository.save(existente);
    }
}
