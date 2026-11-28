package com.projeto_pi.Projeto.PI.service;

import com.projeto_pi.Projeto.PI.dto.TimelineDTOs.Author;
import com.projeto_pi.Projeto.PI.dto.TimelineDTOs.Comment;
import com.projeto_pi.Projeto.PI.dto.TimelineDTOs.Post;
import com.projeto_pi.Projeto.PI.dto.TimelineDTOs.PostRequest;
import com.projeto_pi.Projeto.PI.entity.ComentarioPublicacao;
import com.projeto_pi.Projeto.PI.entity.CurtidaPublicacao;
import com.projeto_pi.Projeto.PI.entity.Papel;
import com.projeto_pi.Projeto.PI.entity.Publicacao;
import com.projeto_pi.Projeto.PI.entity.StatusUsuario;
import com.projeto_pi.Projeto.PI.entity.Usuario;
import com.projeto_pi.Projeto.PI.repository.ComentarioPublicacaoRepository;
import com.projeto_pi.Projeto.PI.repository.CurtidaPublicacaoRepository;
import com.projeto_pi.Projeto.PI.repository.FotoPerfilRepository;
import com.projeto_pi.Projeto.PI.repository.PublicacaoRepository;
import com.projeto_pi.Projeto.PI.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Timeline: startups e investidores aprovados publicam textos, curtem, comentam e compartilham.
 * Quem publica é sempre o usuário do token. Publicações e comentários de contas suspensas
 * deixam de aparecer; excluir uma publicação só a marca como removida.
 */
@Service
public class TimelineService {

    static final int TEXTO_MAXIMO = 500;
    static final int COMENTARIO_MAXIMO = 500;
    static final int PAGINA = 20;

    private final UsuarioRepository usuarios;
    private final PublicacaoRepository publicacoes;
    private final CurtidaPublicacaoRepository curtidas;
    private final ComentarioPublicacaoRepository comentarios;
    private final FotoPerfilRepository fotos;

    public TimelineService(UsuarioRepository usuarios, PublicacaoRepository publicacoes,
                           CurtidaPublicacaoRepository curtidas, ComentarioPublicacaoRepository comentarios,
                           FotoPerfilRepository fotos) {
        this.usuarios = usuarios;
        this.publicacoes = publicacoes;
        this.curtidas = curtidas;
        this.comentarios = comentarios;
        this.fotos = fotos;
    }

    /* ================================================================ leitura */

    /** Página do feed, da mais recente para a mais antiga; {@code antes} continua de onde parou. */
    @Transactional(readOnly = true)
    public List<Post> feed(String email, Long antes, Long depois) {
        Usuario leitor = participante(email);
        List<Publicacao> lista = depois != null
                ? publicacoes.novas(depois, PageRequest.of(0, 50))
                : publicacoes.feed(antes == null ? Long.MAX_VALUE : antes, PageRequest.of(0, PAGINA));
        return montar(lista, leitor);
    }

    /** Publicações de um usuário (para o perfil dele). */
    @Transactional(readOnly = true)
    public List<Post> doUsuario(String email, Long usuarioId, Long antes) {
        Usuario leitor = participante(email);
        return montar(publicacoes.doAutor(usuarioId, antes == null ? Long.MAX_VALUE : antes,
                PageRequest.of(0, PAGINA)), leitor);
    }

    @Transactional(readOnly = true)
    public Post uma(String email, Long id) {
        Usuario leitor = participante(email);
        return montar(List.of(visivel(id)), leitor).get(0);
    }

    @Transactional(readOnly = true)
    public List<Comment> comentarios(String email, Long publicacaoId) {
        Usuario leitor = participante(email);
        Publicacao publicacao = visivel(publicacaoId);
        List<ComentarioPublicacao> lista = comentarios.daPublicacao(publicacao.getId());
        Set<Long> comFoto = fotosDe(lista.stream().map(ComentarioPublicacao::getAutor).toList());
        return lista.stream().map(c -> comentario(c, leitor, comFoto)).toList();
    }

    /* ================================================================ escrita */

    @Transactional
    public Post publicar(String email, PostRequest pedido) {
        Usuario autor = participante(email);
        String texto = pedido == null || pedido.text() == null ? "" : pedido.text().strip();
        if (texto.length() > TEXTO_MAXIMO) {
            throw erro(HttpStatus.BAD_REQUEST, "A publicação deve ter no máximo " + TEXTO_MAXIMO + " caracteres");
        }
        Publicacao compartilhada = null;
        if (pedido != null && pedido.sharedPostId() != null && !pedido.sharedPostId().isBlank()) {
            compartilhada = visivel(id(pedido.sharedPostId()));
            // Compartilhar um compartilhamento simples é compartilhar a original.
            if (compartilhada.compartilhamentoSimples()) {
                compartilhada = compartilhada.getCompartilhada();
                if (compartilhada == null || !aparece(compartilhada)) {
                    throw erro(HttpStatus.NOT_FOUND, "Publicação não encontrada");
                }
            }
            if (texto.isEmpty() && publicacoes.compartilhamentoSimples(autor.getId(), compartilhada.getId()).isPresent()) {
                throw erro(HttpStatus.CONFLICT, "Você já compartilhou esta publicação");
            }
        } else if (texto.isEmpty()) {
            throw erro(HttpStatus.BAD_REQUEST, "Escreva algo para publicar");
        }
        Publicacao salva = publicacoes.save(new Publicacao(autor, texto.isEmpty() ? null : texto, compartilhada));
        return montar(List.of(salva), autor).get(0);
    }

    /** Desfaz o compartilhamento simples que o usuário fez da publicação. */
    @Transactional
    public Post descompartilhar(String email, Long originalId) {
        Usuario usuario = participante(email);
        publicacoes.compartilhamentoSimples(usuario.getId(), originalId).ifPresent(Publicacao::remover);
        publicacoes.flush();
        return montar(List.of(visivel(originalId)), usuario).get(0);
    }

    @Transactional
    public void excluir(String email, Long id) {
        Usuario usuario = participante(email);
        Publicacao publicacao = visivel(id);
        if (!publicacao.getAutor().getId().equals(usuario.getId())) {
            throw erro(HttpStatus.FORBIDDEN, "Só quem publicou pode excluir");
        }
        publicacao.remover();
    }

    @Transactional
    public Post curtir(String email, Long id, boolean curtir) {
        Usuario usuario = participante(email);
        Publicacao publicacao = alvoDeInteracao(id);
        var existente = curtidas.findByPublicacao_IdAndUsuario_Id(publicacao.getId(), usuario.getId());
        if (curtir && existente.isEmpty()) {
            curtidas.saveAndFlush(new CurtidaPublicacao(publicacao, usuario));
        } else if (!curtir) {
            existente.ifPresent(c -> {
                curtidas.delete(c);
                curtidas.flush();
            });
        }
        return montar(List.of(publicacao), usuario).get(0);
    }

    @Transactional
    public Comment comentar(String email, Long id, String texto) {
        Usuario autor = participante(email);
        Publicacao publicacao = alvoDeInteracao(id);
        String limpo = texto == null ? "" : texto.strip();
        if (limpo.isEmpty()) {
            throw erro(HttpStatus.BAD_REQUEST, "O comentário não pode estar vazio");
        }
        if (limpo.length() > COMENTARIO_MAXIMO) {
            throw erro(HttpStatus.BAD_REQUEST, "O comentário deve ter no máximo " + COMENTARIO_MAXIMO + " caracteres");
        }
        ComentarioPublicacao salvo = comentarios.save(new ComentarioPublicacao(publicacao, autor, limpo));
        return comentario(salvo, autor, fotosDe(List.of(autor)));
    }

    /** Quem comentou ou o autor da publicação pode apagar o comentário. */
    @Transactional
    public void excluirComentario(String email, Long comentarioId) {
        Usuario usuario = participante(email);
        ComentarioPublicacao comentario = comentarios.findById(comentarioId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Comentário não encontrado"));
        if (!podeApagar(comentario, usuario)) {
            throw erro(HttpStatus.FORBIDDEN, "Você não pode apagar este comentário");
        }
        comentarios.delete(comentario);
    }

    /* ================================================================ montagem */

    private List<Post> montar(List<Publicacao> lista, Usuario leitor) {
        // Compartilhamentos simples cuja original sumiu não aparecem no feed.
        List<Publicacao> exibidas = lista.stream()
                .filter(p -> !p.compartilhamentoSimples() || aparece(p.getCompartilhada()))
                .toList();
        // Publicações cujos contadores são exibidos: as próprias e as originais compartilhadas.
        Set<Long> ids = new LinkedHashSet<>();
        List<Usuario> autores = new ArrayList<>();
        for (Publicacao p : exibidas) {
            ids.add(p.getId());
            autores.add(p.getAutor());
            Publicacao original = p.getCompartilhada();
            if (original != null && aparece(original)) {
                ids.add(original.getId());
                autores.add(original.getAutor());
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        Contadores c = new Contadores(
                contagem(curtidas.contar(ids)), contagem(comentarios.contar(ids)),
                contagem(publicacoes.contarCompartilhamentos(ids)),
                new HashSet<>(curtidas.curtidasPor(leitor.getId(), ids)),
                new HashSet<>(publicacoes.compartilhadasPor(leitor.getId(), ids)),
                fotosDe(autores));
        List<Post> resultado = new ArrayList<>();
        for (Publicacao p : exibidas) {
            Publicacao original = p.getCompartilhada();
            boolean disponivel = original != null && aparece(original);
            Post compartilhada = disponivel ? post(original, leitor, c, null, false) : null;
            resultado.add(post(p, leitor, c, compartilhada, original != null && !disponivel));
        }
        return resultado;
    }

    private record Contadores(Map<Long, Long> curtidas, Map<Long, Long> comentarios, Map<Long, Long> compartilhamentos,
                              Set<Long> curtidasPorMim, Set<Long> compartilhadasPorMim, Set<Long> comFoto) {
    }

    private Post post(Publicacao p, Usuario leitor, Contadores c, Post compartilhada, boolean indisponivel) {
        Long id = p.getId();
        return new Post(id.toString(), autor(p.getAutor(), c.comFoto()), p.getConteudo(), p.getCriadaEm(),
                p.getAutor().getId().equals(leitor.getId()),
                c.curtidas().getOrDefault(id, 0L), c.comentarios().getOrDefault(id, 0L),
                c.compartilhamentos().getOrDefault(id, 0L),
                c.curtidasPorMim().contains(id), c.compartilhadasPorMim().contains(id),
                p.compartilhamentoSimples(), compartilhada, indisponivel);
    }

    private Comment comentario(ComentarioPublicacao c, Usuario leitor, Set<Long> comFoto) {
        return new Comment(c.getId().toString(), c.getPublicacao().getId().toString(), autor(c.getAutor(), comFoto),
                c.getConteudo(), c.getCriadoEm(), c.getAutor().getId().equals(leitor.getId()), podeApagar(c, leitor));
    }

    static Author autor(Usuario u, Set<Long> comFoto) {
        boolean startup = u.getPapel() == Papel.STARTUP;
        String perfil = startup
                ? (u.getStartup() == null ? null : u.getStartup().getId().toString())
                : (u.getInvestidorMentor() == null ? null : u.getInvestidorMentor().getId().toString());
        return new Author(u.getId(), u.getPapel().name(), perfil, ConversaService.nome(u),
                ComunidadeService.papel(u), comFoto.contains(u.getId()));
    }

    /* ================================================================ apoio */

    private static boolean podeApagar(ComentarioPublicacao c, Usuario usuario) {
        return c.getAutor().getId().equals(usuario.getId())
                || c.getPublicacao().getAutor().getId().equals(usuario.getId());
    }

    private static boolean aparece(Publicacao p) {
        return p != null && !p.isRemovida() && p.getAutor().getStatus() == StatusUsuario.APROVADO;
    }

    private Publicacao visivel(Long id) {
        return publicacoes.findById(id).filter(TimelineService::aparece)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Publicação não encontrada"));
    }

    /** Curtir ou comentar um compartilhamento simples vale para a publicação original. */
    private Publicacao alvoDeInteracao(Long id) {
        Publicacao publicacao = visivel(id);
        if (publicacao.compartilhamentoSimples()) {
            Publicacao original = publicacao.getCompartilhada();
            if (!aparece(original)) {
                throw erro(HttpStatus.NOT_FOUND, "Publicação não encontrada");
            }
            return original;
        }
        return publicacao;
    }

    private Set<Long> fotosDe(List<Usuario> autores) {
        List<Long> ids = autores.stream().map(Usuario::getId).distinct().toList();
        return ids.isEmpty() ? Set.of() : fotos.comFoto(ids);
    }

    private static Map<Long, Long> contagem(List<Object[]> linhas) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] linha : linhas) {
            mapa.put((Long) linha[0], ((Number) linha[1]).longValue());
        }
        return mapa;
    }

    private static Long id(String texto) {
        try {
            return Long.valueOf(texto.strip());
        } catch (NumberFormatException e) {
            throw erro(HttpStatus.NOT_FOUND, "Publicação não encontrada");
        }
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
