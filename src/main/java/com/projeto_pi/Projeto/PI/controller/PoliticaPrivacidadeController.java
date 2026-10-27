package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.config.PoliticaPrivacidadeConfig;
import com.projeto_pi.Projeto.PI.dto.PoliticaPrivacidadeResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/politica-privacidade")
public class PoliticaPrivacidadeController {

    @GetMapping
    public ResponseEntity<PoliticaPrivacidadeResponseDTO> consultar() {

        PoliticaPrivacidadeResponseDTO politica =
                new PoliticaPrivacidadeResponseDTO(
                        PoliticaPrivacidadeConfig.VERSAO_ATUAL,

                        "Política de Privacidade e Uso",

                        "Os dados são utilizados para criar e administrar "
                                + "contas, autenticar usuários, manter os perfis, "
                                + "calcular compatibilidades entre startups e "
                                + "investidores ou mentores e viabilizar mensagens "
                                + "e agendamentos.",

                        "Podem ser tratados e-mail, senha armazenada em "
                                + "formato protegido (hash), papel e status da conta, "
                                + "dados do perfil da startup, pitch, Canvas, "
                                + "áreas de interesse, preferências de investimento, "
                                + "mensagens, agendamentos e registros do consentimento.",

                        "Informações de perfil poderão ser apresentadas a "
                                + "outros usuários autenticados quando necessário "
                                + "às funcionalidades de compatibilidade e interação. "
                                + "Senhas não devem ser exibidas. O acesso administrativo "
                                + "deve ser limitado às finalidades de gestão do sistema.",

                        "Neste projeto acadêmico, os dados são armazenados "
                                + "no PostgreSQL configurado para a aplicação. Em uma "
                                + "implantação real, deverão ser informados os provedores "
                                + "utilizados, os critérios de retenção e as medidas "
                                + "de segurança efetivamente adotadas.",

                        "O titular poderá solicitar informações sobre o "
                                + "tratamento de seus dados, correção de dados "
                                + "incompletos ou incorretos e, quando aplicável, "
                                + "eliminação ou revogação do consentimento. Os pedidos "
                                + "devem ser encaminhados ao responsável pelo projeto.",

                        "Contato do responsável: amandaellem44@gmail.com"
                );

        return ResponseEntity.ok(politica);
    }
}
