package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.ComunidadeDTOs.Community;
import com.projeto_pi.Projeto.PI.dto.ComunidadeDTOs.LastPost;
import com.projeto_pi.Projeto.PI.dto.ComunidadeDTOs.Member;
import com.projeto_pi.Projeto.PI.dto.ComunidadeDTOs.Post;
import com.projeto_pi.Projeto.PI.dto.ComunidadeDTOs.PostRequest;
import com.projeto_pi.Projeto.PI.entity.CategoriaPostagem;
import com.projeto_pi.Projeto.PI.entity.Comunidade;
import com.projeto_pi.Projeto.PI.entity.InvestidorMentor;
import com.projeto_pi.Projeto.PI.entity.MembroComunidade;
import com.projeto_pi.Projeto.PI.entity.MensagemComunidade;
import com.projeto_pi.Projeto.PI.entity.Papel;
import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.matching.Formatos;
import com.projeto_pi.Projeto.PI.repository.ComunidadeRepository;
import com.projeto_pi.Projeto.PI.repository.FotoPerfilRepository;
import com.projeto_pi.Projeto.PI.repository.MembroComunidadeRepository;
import com.projeto_pi.Projeto.PI.repository.MensagemComunidadeRepository;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Comunidades: grupos temáticos em que startups e investidores aprovados conversam e compartilham
 * notícias, iniciativas e movimentos internos.
 *
 * Qualquer startup ou investidor aprovado vê a lista e entra quando quiser; só quem participa lê e
 * envia mensagens. Quem envia é sempre o usuário do token.
 */
@Service
public class ComunidadeService {

    static final int TEXTO_MAXIMO = 1000;
    /** Histórico carregado ao abrir o grupo e limite de cada atualização periódica. */
    static final int HISTORICO = 150;

    private final UsuarioRepository usuarios;
    private final ComunidadeRepository comunidades;
    private final MembroComunidadeRepository membros;
    private final MensagemComunidadeRepository mensagens;
    private final FotoPerfilRepository fotos;

    public ComunidadeService(UsuarioRepository usuarios, ComunidadeRepository comunidades,
                             MembroComunidadeRepository membros, MensagemComunidadeRepository mensagens,
                             FotoPerfilRepository fotos) {
        this.usuarios = usuarios;
        this.comunidades = comunidades;
        this.membros = membros;
        this.mensagens = mensagens;
        this.fotos = fotos;
    }

    @Transactional(readOnly = true)
    public List<Community> listar(String email) {
        Usuario usuario = participante(email);
        List<Comunidade> todas = comunidades.findAllByOrderByOrdemAscIdAsc();
        if (todas.isEmpty()) {
            return List.of();
        }
        Set<Long> minhas = new HashSet<>(membros.comunidadesDo(usuario.getId()));
        Map<Long, Long> quantidade = contagem(membros.contarMembros());
        Map<Long, Long> naoLidas = contagem(mensagens.contarNaoLidas(usuario.getId()));
        Map<Long, MensagemComunidade> ultimas = new HashMap<>();
        for (Object[] linha : mensagens.ultimas(todas.stream().map(Comunidade::getId).toList())) {
            ultimas.put((Long) linha[0], (MensagemComunidade) linha[1]);
        }
        List<Community> lista = new ArrayList<>();
        for (Comunidade comunidade : todas) {
            boolean participa = minhas.contains(comunidade.getId());
            lista.add(resumo(comunidade, usuario, participa, quantidade.getOrDefault(comunidade.getId(), 0L),
                    participa ? naoLidas.getOrDefault(comunidade.getId(), 0L) : 0L, ultimas.get(comunidade.getId())));
        }
        return lista;
    }

    /** Entrar duas vezes não duplica a participação nem o aviso. */
    @Transactional
    public Community entrar(String email, Long comunidadeId) {
        Usuario usuario = participante(email);
        Comunidade comunidade = comunidade(comunidadeId);
        if (membros.findByComunidade_IdAndUsuario_Id(comunidade.getId(), usuario.getId()).isEmpty()) {
            // Quem entra lê o histórico, mas só o que chegar depois conta como não lido.
            membros.saveAndFlush(new MembroComunidade(comunidade, usuario, mensagens.ultimoId(comunidade.getId())));
            MensagemComunidade aviso = mensagens.save(new MensagemComunidade(comunidade, usuario,
                    CategoriaPostagem.SISTEMA, ConversaService.nome(usuario) + " entrou na comunidade"));
            avancarLeitura(comunidade.getId(), usuario.getId(), aviso.getId());
        }
        return detalhe(comunidade, usuario);
    }

    @Transactional
    public Community sair(String email, Long comunidadeId) {
        Usuario usuario = participante(email);
        Comunidade comunidade = comunidade(comunidadeId);
        membros.findByComunidade_IdAndUsuario_Id(comunidade.getId(), usuario.getId()).ifPresent(membro -> {
            membros.delete(membro);
            mensagens.save(new MensagemComunidade(comunidade, usuario, CategoriaPostagem.SISTEMA,
                    ConversaService.nome(usuario) + " saiu da comunidade"));
        });
        membros.flush();
        return detalhe(comunidade, usuario);
    }

    /**
     * Histórico do grupo (as {@value #HISTORICO} mais recentes) ou, com {@code depois}, só o que chegou
     * desde a última consulta. Tudo o que existe até agora passa a contar como lido.
     */
    @Transactional
    public List<Post> mensagens(String email, Long comunidadeId, Long depois) {
        Usuario usuario = participante(email);
        MembroComunidade membro = membro(usuario, comunidadeId);
        Long comunidade = membro.getComunidade().getId();
        List<MensagemComunidade> lista;
        if (depois == null) {
            lista = new ArrayList<>(mensagens.recentes(comunidade, PageRequest.of(0, HISTORICO)));
            java.util.Collections.reverse(lista);
        } else {
            lista = mensagens.depoisDe(comunidade, depois, PageRequest.of(0, HISTORICO));
        }
        Long ultimo = mensagens.ultimoId(comunidade);
        if (ultimo != null && (membro.getUltimaLidaId() == null || ultimo > membro.getUltimaLidaId())) {
            membro.setUltimaLidaId(ultimo);
        }
        Set<Long> comFoto = lista.isEmpty() ? Set.of()
                : fotos.comFoto(lista.stream().map(m -> m.getRemetente().getId()).distinct().toList());
        return lista.stream().map(m -> postagem(m, usuario, comFoto)).toList();
    }

    @Transactional
    public Post enviar(String email, Long comunidadeId, PostRequest pedido) {
        Usuario usuario = participante(email);
        MembroComunidade membro = membro(usuario, comunidadeId);
        String texto = pedido == null || pedido.text() == null ? "" : pedido.text().strip();
        if (texto.isEmpty()) {
            throw erro(HttpStatus.BAD_REQUEST, "A mensagem não pode estar vazia");
        }
        if (texto.length() > TEXTO_MAXIMO) {
            throw erro(HttpStatus.BAD_REQUEST, "A mensagem deve ter no máximo " + TEXTO_MAXIMO + " caracteres");
        }
        MensagemComunidade salva = mensagens.save(
                new MensagemComunidade(membro.getComunidade(), usuario, categoria(pedido.category()), texto));
        membro.setUltimaLidaId(salva.getId());
        return postagem(salva, usuario, fotos.comFoto(List.of(usuario.getId())));
    }

    /** Quem está no grupo (só para quem também participa). */
    @Transactional(readOnly = true)
    public List<Member> membros(String email, Long comunidadeId) {
        Usuario usuario = participante(email);
        membro(usuario, comunidadeId);
        List<MembroComunidade> lista = membros.membros(comunidadeId);
        Set<Long> comFoto = lista.isEmpty() ? Set.of()
                : fotos.comFoto(lista.stream().map(m -> m.getUsuario().getId()).toList());
        return lista.stream()
                .map(m -> new Member(m.getUsuario().getId(), ConversaService.nome(m.getUsuario()),
                        papel(m.getUsuario()), comFoto.contains(m.getUsuario().getId()),
                        m.getUsuario().getId().equals(usuario.getId()), m.getEntrouEm()))
                .toList();
    }

    /* ============================================================================ apoio */

    private Community detalhe(Comunidade comunidade, Usuario usuario) {
        boolean participa = membros.findByComunidade_IdAndUsuario_Id(comunidade.getId(), usuario.getId()).isPresent();
        long quantidade = contagem(membros.contarMembros()).getOrDefault(comunidade.getId(), 0L);
        long naoLidas = participa ? contagem(mensagens.contarNaoLidas(usuario.getId()))
                .getOrDefault(comunidade.getId(), 0L) : 0L;
        MensagemComunidade ultima = mensagens.ultimas(List.of(comunidade.getId())).stream()
                .map(linha -> (MensagemComunidade) linha[1]).findFirst().orElse(null);
        return resumo(comunidade, usuario, participa, quantidade, naoLidas, ultima);
    }

    private Community resumo(Comunidade comunidade, Usuario usuario, boolean participa, long quantidade,
                             long naoLidas, MensagemComunidade ultima) {
        LastPost ultimaMensagem = ultima == null ? null : new LastPost(
                Formatos.cortar(ultima.getConteudo(), 120), ConversaService.nome(ultima.getRemetente()),
                ultima.getCategoria().name(), ultima.getEnviadaEm(),
                ultima.getRemetente().getId().equals(usuario.getId()));
        return new Community(comunidade.getId().toString(), comunidade.getSlug(), comunidade.getNome(),
                comunidade.getDescricao(), quantidade, participa, naoLidas, ultimaMensagem);
    }

    private Post postagem(MensagemComunidade mensagem, Usuario usuario, Set<Long> comFoto) {
        Usuario autor = mensagem.getRemetente();
        String remetente = mensagem.getCategoria() == CategoriaPostagem.SISTEMA ? "system"
                : autor.getId().equals(usuario.getId()) ? "me" : "member";
        return new Post(mensagem.getId().toString(), remetente, autor.getId(), ConversaService.nome(autor),
                papel(autor), comFoto.contains(autor.getId()), mensagem.getCategoria().name(),
                mensagem.getConteudo(), mensagem.getEnviadaEm());
    }

    private void avancarLeitura(Long comunidadeId, Long usuarioId, Long mensagemId) {
        membros.findByComunidade_IdAndUsuario_Id(comunidadeId, usuarioId).ifPresent(m -> m.setUltimaLidaId(mensagemId));
    }

    /** "Startup" ou o tipo do investidor (anjo, mentor...), exibido junto ao nome no grupo. */
    static String papel(Usuario usuario) {
        if (usuario.getPapel() == Papel.STARTUP) {
            return "Startup";
        }
        InvestidorMentor perfil = usuario.getInvestidorMentor();
        return perfil == null ? "Investidor(a)" : MatchmakingService.papelDe(perfil);
    }

    private static CategoriaPostagem categoria(String valor) {
        if (valor == null || valor.isBlank()) {
            return CategoriaPostagem.MENSAGEM;
        }
        try {
            CategoriaPostagem categoria = CategoriaPostagem.valueOf(valor.strip().toUpperCase(Locale.ROOT));
            if (categoria != CategoriaPostagem.SISTEMA) {
                return categoria;
            }
        } catch (IllegalArgumentException ignorada) {
            // cai no erro abaixo
        }
        throw erro(HttpStatus.BAD_REQUEST, "Categoria inválida: use MENSAGEM, NOTICIA, INICIATIVA ou MOVIMENTO");
    }

    private static Map<Long, Long> contagem(List<Object[]> linhas) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] linha : linhas) {
            mapa.put((Long) linha[0], ((Number) linha[1]).longValue());
        }
        return mapa;
    }

    private Comunidade comunidade(Long id) {
        return comunidades.findById(id).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Comunidade não encontrada"));
    }

    private MembroComunidade membro(Usuario usuario, Long comunidadeId) {
        Comunidade comunidade = comunidade(comunidadeId);
        return membros.findByComunidade_IdAndUsuario_Id(comunidade.getId(), usuario.getId())
                .orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Entre na comunidade para ver e enviar mensagens"));
    }

    private Usuario participante(String email) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email == null ? "" : email)
                .orElseThrow(() -> erro(HttpStatus.FORBIDDEN, "Acesso não permitido"));
        if (usuario.getStatus() != StatusUsuario.APROVADO
                || (usuario.getPapel() != Papel.INVESTIDOR && usuario.getPapel() != Papel.STARTUP)) {
            throw erro(HttpStatus.FORBIDDEN, "Acesso não permitido");
        }
        return usuario;
    }

    private static ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
