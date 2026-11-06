package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.config.PoliticaPrivacidadeConfig;
import com.projeto_pi.Projeto.PI.dto.PoliticaPrivacidadeResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/politica-privacidade")
public class PoliticaPrivacidadeController {

    private static final String CONTEUDO = """
            VÉRTICE - CONTRATO DE ADESÃO: TERMOS DE USO, POLÍTICA DE PRIVACIDADE E CONSENTIMENTO LGPD (v1.0)

            PREÂMBULO

            Pelo presente instrumento particular de contrato de adesão, de um lado:

            VÉRTICE, plataforma de matchmaking para startups e investidores anjo do Nordeste, mantida pela equipe do Projeto Integrador do 5º período de Análise e Desenvolvimento de Sistemas da Faculdade SENAC Pernambuco (Unidade de Extensão Full Stack, 2026.2), com sede acadêmica em Recife/PE, composta por Amanda Ellem da Cruz Freitas, Gilberto Quintino de Santana Filho, Isabel Vitória Alves Siqueira, Ronald da Paixão e Silva e Vinicius Manoel Lima de Araujo, doravante denominada VÉRTICE ou CONTROLADORA, nos termos do art. 5º, VI, da Lei nº 13.709/2018 (LGPD);

            e, de outro lado, a pessoa física que realiza seu cadastro na plataforma, identificada pelo e-mail e pelos demais dados que informar, atuando em nome próprio ou como representante da startup cadastrada, doravante denominada USUÁRIO ou TITULAR;

            em conjunto, as PARTES, resolvem celebrar este contrato, que se rege pelas cláusulas seguintes, considerando que:

            I – a Vértice conecta startups em estágio inicial a investidores anjo e mentores, com foco no ecossistema do Porto Digital (Bairro do Recife) e do Nordeste;

            II – para isso, a Vértice trata dados pessoais e informações de negócio dos usuários;

            III – a LGPD exige que todo tratamento tenha finalidade determinada, base legal e transparência para o titular;

            IV – o Usuário adere a este contrato por aceite eletrônico, sem poder negociar suas cláusulas, o que impõe que elas sejam claras, destacadas e interpretadas a seu favor em caso de dúvida.


            CLÁUSULA 1ª — DO OBJETO

            1.1. Este contrato regula o uso da plataforma Vértice pelo Usuário e o tratamento de dados pessoais necessário a esse uso, em conformidade com a LGPD.

            1.2. Integram este contrato, como anexos, o Resumo em linguagem simples (Anexo I) e o Termo de Consentimento (Anexo II). O Anexo I é apenas explicativo e não substitui as cláusulas; havendo divergência, prevalecem as cláusulas.

            1.3. Ao marcar a caixa de aceite no cadastro, o Usuário declara que leu e compreendeu este contrato e que concorda com ele.

            1.4. Gratuidade. O uso da plataforma é gratuito nesta fase. Se houver cobrança no futuro, este contrato será atualizado na forma da Cláusula 14ª e a cobrança dependerá de novo aceite.


            CLÁUSULA 2ª — DAS DEFINIÇÕES

            2.1. Para os fins deste contrato, considera-se:

            I – dado pessoal: informação relacionada a pessoa natural identificada ou identificável, como o e-mail;

            II – dado pessoal sensível: dado sobre origem racial ou étnica, convicção religiosa, opinião política, saúde, vida sexual, dado genético ou biométrico, entre outros definidos no art. 5º, II, da LGPD;

            III – titular: a pessoa natural a quem se referem os dados pessoais, no caso o Usuário;

            IV – controlador: quem decide como os dados são tratados, no caso a Vértice;

            V – operador: quem trata dados em nome do controlador, como o prestador de hospedagem em nuvem;

            VI – tratamento: toda operação com dados pessoais, como coleta, armazenamento, consulta, compartilhamento e eliminação;

            VII – consentimento: manifestação livre, informada e inequívoca pela qual o titular concorda com o tratamento de seus dados para uma finalidade determinada;

            VIII – encarregado: pessoa indicada pelo controlador para atender os titulares e a ANPD;

            IX – ANPD: Autoridade Nacional de Proteção de Dados.


            CLÁUSULA 3ª — DO CADASTRO E DOS PERFIS DE USO

            3.1. A plataforma possui três perfis de uso:

            I – Startup (empreendedor): cadastra a startup, apresenta pitch e modelo de negócio (Canvas) e busca mentoria ou investimento;

            II – Investidor anjo ou mentor: informa áreas de interesse, faixa de ticket e disponibilidade, avalia startups compatíveis e inicia contato;

            III – Administrador: aprova cadastros, modera conteúdo e acompanha métricas gerais da plataforma.

            3.2. Para aderir a este contrato, o Usuário declara que:

            I – tem 18 anos ou mais e plena capacidade civil;

            II – informa dados verdadeiros, completos e atualizados;

            III – quando cadastrar uma startup, tem poderes para representá-la e para fornecer as informações de negócio apresentadas.

            3.3. Todo cadastro depende de aprovação de um administrador e pode ficar pendente, ser aprovado, rejeitado ou suspenso.

            3.4. A conta é pessoal e intransferível. O Usuário responde pela guarda de sua senha e deve avisar a Vértice, pelo canal da Cláusula 16ª, se suspeitar de uso indevido.


            CLÁUSULA 4ª — DOS DADOS PESSOAIS TRATADOS

            4.1. Para a execução deste contrato, a Vértice trata as seguintes categorias de dados:

            Conta e acesso:
            E-mail, senha (guardada somente em formato criptografado irreversível), perfil de acesso e situação do cadastro (pendente, aprovado, rejeitado ou suspenso). Origem: Usuário.

            Perfil da startup:
            Nome da startup, segmento, estágio, necessidades, pitch e Canvas. Origem: Usuário.

            Perfil de investidor ou mentor:
            Tipo (investidor ou mentor), áreas de interesse, faixa de ticket (mínimo e máximo), estágios de preferência e disponibilidade. Origem: Usuário.

            Compatibilidade:
            Pontuação e nível de afinidade entre perfis. Origem: Calculado pela plataforma.

            Comunicação:
            Mensagens e agendamentos de reunião (data, pauta e situação). Origem: Usuário e outra parte.

            Avaliações:
            Nota e comentário após uma interação. Origem: Usuário.

            Consentimento:
            Aceite, data e hora e versão deste contrato aceita. Origem: Plataforma.

            Dados técnicos e auditoria:
            Data e hora de acessos e ações, endereço IP gerado pela infraestrutura e ações administrativas. Origem: Plataforma.

            4.2. Dados sensíveis. A Vértice não solicita dados pessoais sensíveis. O Usuário deve evitar inseri-los em campos livres, como pitch, Canvas e mensagens.

            4.3. Dados de terceiros. O Usuário não deve inserir dados pessoais de outras pessoas (sócios, clientes, funcionários) sem autorização delas e responde por essa inserção.


            CLÁUSULA 5ª — DAS FINALIDADES E DAS BASES LEGAIS

            5.1. Cada tratamento de dados tem finalidade determinada e base legal prevista no art. 7º da LGPD:

            Criar, autenticar e manter conta; aprovar o cadastro:
            Dados: conta e acesso.
            Base legal: Execução de contrato ou de procedimentos preliminares a pedido do titular (art. 7º, V).

            Calcular compatibilidade entre startups e investidores ou mentores:
            Dados: perfis e compatibilidade.
            Base legal: Execução de contrato (art. 7º, V).

            Exibir o perfil a usuários aprovados com perfil compatível:
            Dados: perfil da startup ou do investidor/mentor.
            Base legal: Consentimento (art. 7º, I).

            Permitir mensagens, agendamento de reuniões e avaliações:
            Dados: comunicação e avaliações.
            Base legal: Execução de contrato (art. 7º, V).

            Gerar métricas gerais do ecossistema (matches, conversas e reuniões):
            Dados: dados de uso, de forma agregada.
            Base legal: Legítimo interesse (art. 7º, IX).

            Moderar cadastros e conteúdo:
            Dados: perfis, conteúdo publicado e denúncias.
            Base legal: Legítimo interesse (art. 7º, IX).

            Garantir segurança, prevenir fraudes e manter registro de auditoria:
            Dados: dados técnicos e de auditoria.
            Base legal: Legítimo interesse (art. 7º, IX) e cumprimento de obrigação legal (art. 7º, II), quando aplicável.

            Guardar a prova do consentimento:
            Dados: consentimento.
            Base legal: Exercício regular de direitos (art. 7º, VI, e art. 8º, § 2º).

            5.2. A Vértice não trata os dados do Usuário para finalidade diferente das listadas. Se isso mudar, informará o Usuário e, quando a lei exigir, pedirá novo consentimento.

            5.3. Compatibilidade automatizada. A pontuação de compatibilidade é calculada automaticamente, comparando segmento, estágio e necessidades da startup com as áreas de interesse e os estágios de preferência dos investidores e mentores. Ela é apenas uma sugestão: não impede contato nem decide nada pelo Usuário. O Usuário pode pedir, pelo canal da Cláusula 16ª, informações claras e adequadas sobre os critérios e a revisão do resultado (art. 20 da LGPD), e a Vértice responde no prazo da cláusula 9.3, observados os segredos comercial e industrial.

            5.4. Se a Vértice passar a usar inteligência artificial para aprimorar a compatibilidade, este contrato será atualizado antes, na forma da Cláusula 14ª.


            CLÁUSULA 6ª — DO COMPARTILHAMENTO DE DADOS

            6.1. A Vértice não vende nem aluga dados do Usuário e não os compartilha para fins de publicidade.

            6.2. O compartilhamento ocorre somente nas seguintes hipóteses:

            I – com outros usuários: o perfil da startup ou do investidor/mentor, incluindo pitch e Canvas, pode ser visto por usuários aprovados do perfil oposto com quem haja compatibilidade; mensagens e agendamentos são visíveis apenas aos participantes da conversa;

            II – com a equipe administradora: administradores acessam cadastros, conteúdo e métricas para aprovar, moderar e manter a plataforma, nos limites da Cláusula 7ª;

            III – com operadores: prestadores de hospedagem, banco de dados e serviços de nuvem que tratam dados em nome da Vértice, sob dever de confidencialidade e segurança;

            IV – com autoridades: dados cadastrais, a autoridade administrativa com competência legal para requisitá-los; registros de acesso e conteúdo de comunicações privadas, somente mediante ordem judicial (art. 10, §§ 1º a 3º, do Marco Civil da Internet); e, em qualquer caso, obrigação regulatória aplicável.

            6.3. As avaliações servem para melhorar a plataforma e são acessadas pela administração. Se passarem a ser exibidas a outros usuários, este contrato será atualizado antes.

            6.4. Em apresentações acadêmicas do projeto, como o Projetaí, a Vértice usa dados fictícios ou anonimizados e não exibe dados reais de usuários sem autorização.


            CLÁUSULA 7ª — DAS OBRIGAÇÕES DA VÉRTICE

            7.1. A Vértice adota medidas técnicas e administrativas para proteger os dados contra acesso não autorizado, perda e uso indevido (art. 46 da LGPD), entre elas:

            I – comunicação protegida por HTTPS;

            II – criptografia em repouso dos dados pessoais e de negócio sensíveis, como planos e informações de startups;

            III – guarda de senhas somente em formato irreversível (BCrypt);

            IV – acesso por perfil (startup, investidor/mentor e administrador) e autenticação por token com prazo de validade;

            V – acesso interno restrito aos integrantes da equipe que precisam dos dados para sua função.

            7.2. Em caso de incidente de segurança que possa acarretar risco ou dano relevante ao titular, a Vértice comunicará a ANPD e os titulares afetados em prazo razoável, na forma do art. 48 da LGPD.

            7.3. Moderação. Administradores aprovam cadastros e moderam o conteúdo publicado. A Vértice respeita o sigilo das comunicações privadas e não lê mensagens de forma rotineira. O conteúdo de uma conversa só é acessado quando um dos participantes a denunciar ou a encaminhar à administração, ou mediante ordem judicial (art. 7º, III, do Marco Civil da Internet), e o acesso fica registrado.

            7.4. Auditoria. Toda ação administrativa (aprovar, rejeitar, suspender ou remover conteúdo) é registrada em log de auditoria com autor, ação e data e hora.

            7.5. O Usuário pode contestar decisões de moderação pelo canal da Cláusula 16ª, e a decisão será reavaliada. Ao remover conteúdo ou suspender a conta, a Vértice informa ao Usuário os motivos e os elementos que permitam a contestação (art. 20 do Marco Civil da Internet).

            7.6. Registros de acesso. A Vértice guarda os registros de acesso à plataforma (data, hora e endereço IP) em ambiente controlado e sigiloso, pelo prazo da Cláusula 10ª, e só os fornece a autoridades mediante ordem judicial (arts. 10 e 15 do Marco Civil da Internet).

            7.7. Local de armazenamento e fase acadêmica. Nesta fase acadêmica, os dados ficam em banco de dados PostgreSQL configurado para a aplicação. O provedor de hospedagem, a região dos servidores e as medidas de segurança efetivamente adotadas serão informados neste contrato antes do uso com usuários reais, na forma da cláusula 14.2. Medidas ainda em implementação, como a criptografia em repouso (cláusula 7.1, II), serão concluídas antes desse uso. Até lá, o ambiente é de teste e o Usuário não deve inserir informações estratégicas reais.


            CLÁUSULA 8ª — DAS OBRIGAÇÕES DO USUÁRIO

            8.1. Conduta. É vedado ao Usuário publicar conteúdo ilícito, discriminatório, ofensivo ou enganoso, assumir identidade falsa, enviar mensagens não solicitadas em massa (spam), assediar outros usuários ou tentar fraudar a plataforma.

            8.2. Confidencialidade. Pitch, Canvas, planos e demais informações de negócio a que o Usuário tiver acesso pela plataforma são confidenciais. O Usuário deve usá-los somente para avaliar a oportunidade e não pode copiá-los nem divulgá-los sem autorização de quem os publicou.

            8.3. A plataforma não substitui acordo de confidencialidade (NDA). As partes devem formalizar um, fora da plataforma, antes de trocar informações estratégicas.

            8.4. Conteúdo do Usuário. O conteúdo publicado continua sendo do Usuário. Ao publicá-lo, o Usuário autoriza a Vértice a exibi-lo aos usuários compatíveis, nos limites deste contrato.

            8.5. O descumprimento desta cláusula pode levar à suspensão ou ao encerramento da conta, na forma da Cláusula 13ª, sem prejuízo da responsabilidade civil do Usuário por danos causados a terceiros.

            8.6. Propriedade intelectual. A marca Vértice, o código-fonte, o layout, os textos e os demais elementos da plataforma pertencem à equipe do projeto, ressalvados os direitos de terceiros e de bibliotecas de código aberto, e não podem ser copiados, modificados ou distribuídos sem autorização. O conteúdo publicado pelo Usuário continua sendo dele (cláusula 8.4).


            CLÁUSULA 9ª — DOS DIREITOS DO TITULAR

            9.1. O Usuário pode exercer a qualquer momento, sem custo, os direitos do art. 18 da LGPD:

            I – confirmar se seus dados são tratados e acessá-los;

            II – corrigir dados incompletos, inexatos ou desatualizados;

            III – pedir anonimização, bloqueio ou eliminação de dados desnecessários, excessivos ou tratados em desconformidade com a lei;

            IV – pedir a portabilidade dos dados a outro fornecedor, conforme a regulamentação da ANPD;

            V – pedir a eliminação dos dados tratados com base em seu consentimento;

            VI – saber com quem seus dados foram compartilhados;

            VII – saber que pode não dar o consentimento e quais as consequências da recusa;

            VIII – revogar o consentimento, de forma simples e gratuita;

            IX – pedir a revisão de decisões tomadas apenas por tratamento automatizado e informações sobre seus critérios (art. 20);

            X – opor-se a tratamento feito sem seu consentimento que viole a lei.

            9.2. Os pedidos são feitos pelo canal da Cláusula 16ª, com indicação do e-mail da conta e do direito que se deseja exercer. A Vértice pode solicitar informações para confirmar a identidade do solicitante.

            9.3. A confirmação da existência de tratamento e o acesso aos dados são atendidos em até 15 dias, e os demais pedidos no menor prazo possível (art. 19).

            9.4. O Usuário pode apresentar petição à ANPD contra a Vértice (art. 18, § 1º).


            CLÁUSULA 10ª — DA RETENÇÃO E DA ELIMINAÇÃO DOS DADOS

            10.1. A Vértice guarda os dados apenas pelo tempo necessário às finalidades da Cláusula 5ª:

            Conta, perfil, mensagens, agendamentos e avaliações:
            Enquanto a conta existir. Após pedido de exclusão, eliminados em até 30 dias.

            Cadastro rejeitado:
            Até 30 dias após a decisão, para permitir contestação, e depois eliminado.

            Registro de consentimento e log de auditoria:
            5 anos, para comprovar o cumprimento da LGPD e o exercício regular de direitos.

            Métricas agregadas ou anonimizadas:
            Podem ser mantidas, pois não identificam o Usuário.

            Registros de acesso (data, hora e IP):
            6 meses, conforme a referência do art. 15 do Marco Civil da Internet, ou por prazo maior se houver ordem judicial.

            10.2. A eliminação não alcança dados que a lei obrigue a manter ou que sejam necessários ao exercício regular de direitos (art. 16 da LGPD).

            10.3. Ao excluir a conta, as mensagens do Usuário são removidas ou anonimizadas nas conversas.

            10.4. Se a operação da Vértice for encerrada, o Usuário será avisado com antecedência e seus dados serão eliminados ou anonimizados, salvo as exceções legais.


            CLÁUSULA 11ª — DA NATUREZA ACADÊMICA, DA AUSÊNCIA DE GARANTIA E DA RESPONSABILIDADE

            11.1. A Vértice é desenvolvida em contexto acadêmico e está em fase de validação com usuários de teste.

            11.2. A Vértice não é corretora, consultoria nem intermediária financeira, não executa operações de investimento e não garante matches, mentorias, aportes ou qualquer resultado de negócio. Qualquer negociação ocorre fora da plataforma, sob responsabilidade das partes envolvidas.

            11.3. A compatibilidade é uma sugestão. A Vértice não verifica toda informação publicada pelos usuários nem avalia a viabilidade de negócios, e o Usuário deve fazer sua própria análise antes de qualquer decisão de investimento ou parceria.

            11.4. A Vértice busca manter a plataforma disponível de forma contínua, mas pode haver interrupções para manutenção ou por falhas técnicas.

            11.5. A Vértice não responde pelas negociações firmadas entre usuários nem pela veracidade das informações por eles publicadas, mas responde pelos defeitos do próprio serviço e pelo tratamento irregular de dados pessoais, nos termos dos arts. 42 a 45 da LGPD e, havendo relação de consumo, do art. 14 do CDC.


            CLÁUSULA 12ª — DO CONSENTIMENTO (CLÁUSULA DESTACADA)

            12.1. O USUÁRIO CONSENTE, DE FORMA LIVRE, INFORMADA E INEQUÍVOCA, COM A EXIBIÇÃO DO SEU PERFIL (STARTUP OU INVESTIDOR/MENTOR, INCLUINDO PITCH E CANVAS) A USUÁRIOS APROVADOS DO PERFIL OPOSTO COM QUEM HAJA COMPATIBILIDADE, FINALIDADE ESPECÍFICA SEM A QUAL A PLATAFORMA NÃO CUMPRE SEU OBJETO. AS DEMAIS FINALIDADES (CRIAR E MANTER A CONTA, CALCULAR COMPATIBILIDADE, MENSAGENS, AGENDAMENTOS E AVALIAÇÕES) DECORREM DA EXECUÇÃO DO CONTRATO, NA FORMA DA CLÁUSULA 5ª.

            12.2. A exibição do perfil a outros usuários é condição para o fornecimento do serviço. A Vértice não funciona sem o tratamento da cláusula 12.1. Por isso, a recusa do consentimento impede a conclusão do cadastro (art. 9º, § 3º, da LGPD). Se o Usuário recusar, o cadastro não é criado e seus dados não são gravados.

            12.3. Revogação. O Usuário pode revogar o consentimento a qualquer momento, de forma gratuita, pelo canal da Cláusula 16ª (art. 8º, § 5º). A revogação não afeta os tratamentos já realizados e leva ao encerramento da conta, nos termos da Cláusula 13ª.

            12.4. Prova do consentimento. A Vértice registra a data, a hora e a versão deste contrato aceita pelo Usuário, para demonstrar que o consentimento foi obtido de acordo com a lei (art. 8º, § 2º).

            12.5. O consentimento é dado por finalidades determinadas. Autorizações genéricas são nulas (art. 8º, § 4º). Os tratamentos fundados em outras bases legais, como a execução de contrato e o legítimo interesse, listados na Cláusula 5ª, não dependem deste consentimento e seguem as regras próprias dessas bases.


            CLÁUSULA 13ª — DA VIGÊNCIA, DA REVOGAÇÃO E DA RESCISÃO

            13.1. Este contrato vigora por prazo indeterminado, a partir do aceite eletrônico, até o encerramento da conta ou da operação da plataforma.

            13.2. O Usuário pode encerrar o contrato a qualquer momento, sem multa, excluindo a conta ou pedindo o encerramento pelo canal da Cláusula 16ª.

            13.3. A Vértice pode suspender ou encerrar a conta em caso de descumprimento da Cláusula 8ª, de risco à segurança da plataforma ou de determinação legal, com aviso prévio ao Usuário, indicação dos motivos e possibilidade de contestação (cláusula 7.5). Em caso de urgência por risco à segurança ou de ordem legal, o aviso é feito logo depois da medida.

            13.4. A revogação do consentimento (cláusula 12.3) encerra a conta, porque a exibição do perfil e a compatibilidade são essenciais ao serviço.

            13.5. Sobrevivem ao encerramento as obrigações de confidencialidade (cláusula 8.2), a retenção prevista na Cláusula 10ª e o foro da Cláusula 17ª.


            CLÁUSULA 14ª — DAS ALTERAÇÕES CONTRATUAIS

            14.1. A Vértice pode alterar este contrato para refletir mudanças na plataforma ou na lei, com aviso ao Usuário com pelo menos 15 dias de antecedência. Cada versão tem número e data, e a versão aceita pelo Usuário fica registrada em sua conta. Se a alteração reduzir direitos ou ampliar obrigações do Usuário, depende de novo aceite, e o Usuário que discordar pode encerrar a conta sem ônus (cláusula 13.2).

            14.2. Quando a alteração mudar a finalidade do tratamento, sua forma e duração, a identificação da controladora ou o compartilhamento de dados, a Vértice informará o Usuário com destaque sobre o teor da mudança. Se o consentimento for exigido, o Usuário poderá revogá-lo caso discorde (art. 9º, § 2º, da LGPD).

            14.3. O silêncio do Usuário não vale como consentimento. Quando a lei exigir novo consentimento, o acesso às funcionalidades afetadas dependerá de novo aceite.


            CLÁUSULA 15ª — DAS DISPOSIÇÕES GERAIS

            15.1. Menores de idade. A Vértice é destinada a maiores de 18 anos. O cadastro de pessoa menor de idade será suspenso e seus dados eliminados.

            15.2. Transferência internacional. Se algum operador tratar dados fora do Brasil, a transferência ocorrerá apenas nas hipóteses do art. 33 da LGPD, com garantias adequadas de proteção, e o Usuário será informado.

            15.3. Armazenamento no navegador. A Vértice guarda no navegador do Usuário apenas o necessário para manter a sessão, como o token de autenticação. Não usa cookies de publicidade nem ferramentas de rastreamento de terceiros.

            15.4. Contrato de adesão. Este contrato é de adesão. Havendo relação de consumo, aplica-se o Código de Defesa do Consumidor (Lei nº 8.078/1990), e as cláusulas que limitam direitos do Usuário aparecem em destaque (art. 54, § 4º). As cláusulas ambíguas ou contraditórias são interpretadas de modo mais favorável ao Usuário (art. 47 do CDC e art. 423 do Código Civil). É nula a cláusula abusiva (art. 51 do CDC) e a que estipule renúncia antecipada do Usuário a direito resultante da natureza do negócio (art. 424 do Código Civil).

            15.5. A invalidade de uma cláusula não afeta as demais. A tolerância quanto ao descumprimento de qualquer cláusula não implica renúncia nem novação.


            CLÁUSULA 16ª — DAS COMUNICAÇÕES E DO ENCARREGADO

            16.1. Todas as solicitações do Usuário sobre seus dados pessoais e sobre este contrato devem ser dirigidas ao encarregado, nos termos do art. 41 da LGPD:

            Encarregado pelo tratamento de dados:
            Amanda Ellem da Cruz Freitas, integrante da equipe do projeto.

            Canal de contato:
            amandaellem2023@gmail.com

            16.2. A Vértice se comunica com o Usuário pelo e-mail cadastrado e por avisos na plataforma. O Usuário deve manter o e-mail atualizado.


            CLÁUSULA 17ª — DA LEGISLAÇÃO APLICÁVEL E DO FORO

            17.1. Este contrato é regido pela legislação brasileira, em especial pela Lei nº 13.709/2018 (LGPD), pela Lei nº 12.965/2014 (Marco Civil da Internet), pelo Código Civil e, havendo relação de consumo, pelo Código de Defesa do Consumidor (Lei nº 8.078/1990).

            17.2. Fica eleito o foro da Comarca do Recife, Pernambuco, Havendo relação de consumo, prevalece o foro do domicílio do Usuário (art. 101, I, do CDC).


            FECHO E ACEITE ELETRÔNICO

            E, por estarem de acordo, as partes celebram este contrato por meio eletrônico, com fundamento no art. 107 do Código Civil e no art. 10, § 2º, da Medida Provisória nº 2.200-2/2001, reconhecendo o aceite eletrônico como válido e suficiente para comprovar a autoria e a integridade do ajuste.

            Recife/PE, na data e hora do aceite registradas pela plataforma.

            Registro da adesão:
            Usuário: E-mail da conta cadastrada.
            Data e hora do aceite: Registradas pela plataforma no momento do cadastro.
            Versão aceita: 1.0.
            Meio de aceite: Caixa de consentimento marcada na última etapa do cadastro.

            VÉRTICE — Equipe do Projeto Integrador, Faculdade SENAC Pernambuco

            USUÁRIO — aceite eletrônico conforme o registro acima


            ANEXO I — RESUMO EM LINGUAGEM SIMPLES

            Este resumo ajuda na leitura, mas não substitui as cláusulas do contrato.

            • O que coletamos: e-mail e senha, o perfil da sua startup ou de investidor/mentor, mensagens, agendamentos, avaliações e registros técnicos de uso (Cláusula 4ª).

            • Para que usamos: criar sua conta, calcular compatibilidade, exibir seu perfil a quem pode se interessar, permitir contato e reuniões e manter a plataforma segura (Cláusula 5ª).

            • Quem vê seu perfil: usuários aprovados com perfil compatível. Suas mensagens só são vistas por você e pela outra parte, salvo as exceções de moderação (Cláusulas 6ª e 7ª).

            • Venda de dados: não vendemos, não alugamos e não usamos seus dados para publicidade (cláusula 6.1).

            • Segurança: senhas guardadas em formato irreversível, dados protegidos em trânsito e em repouso e registro das ações administrativas (Cláusula 7ª).

            • Seus direitos: acesso, correção, exclusão, portabilidade e revogação do consentimento, quando quiser (Cláusula 9ª).

            • Natureza do projeto: a Vértice é um projeto acadêmico. Ela não intermedia investimentos nem garante resultados (Cláusula 11ª).

            • Aceite: sem marcar a caixa de consentimento, o cadastro não é concluído (Cláusula 12ª).
            """;

    @GetMapping
    public ResponseEntity<PoliticaPrivacidadeResponseDTO> consultar() {

        PoliticaPrivacidadeResponseDTO politica =
                new PoliticaPrivacidadeResponseDTO(
                        PoliticaPrivacidadeConfig.VERSAO_ATUAL,
                        "Vértice - Contrato de Adesão: Termos de Uso, "
                                + "Política de Privacidade e Consentimento LGPD",
                        CONTEUDO,
                        true
                );

        return ResponseEntity.ok(politica);
    }
}
