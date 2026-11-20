package com.projeto_pi.Projeto.PI.controller;

import com.projeto_pi.Projeto.PI.dto.*;
import com.projeto_pi.Projeto.PI.entity.*;
import com.projeto_pi.Projeto.PI.service.UsuarioService;
import java.io.IOException;
import java.util.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/auth/cadastro")
public class MultipartRegistrationController {
    private final UsuarioService service;
    private final JsonMapper mapper;
    private final com.projeto_pi.Projeto.PI.service.PerfilService perfis;
    public MultipartRegistrationController(UsuarioService service, JsonMapper mapper,
                                           com.projeto_pi.Projeto.PI.service.PerfilService perfis) {
        this.service = service; this.mapper = mapper; this.perfis = perfis;
    }

    /** Cadastro do investidor com foto de perfil. Listas (segments, stages) vêm como campos repetidos. */
    @PostMapping(value = "/investidor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<UsuarioResponseDTO> investidor(
            @RequestParam org.springframework.util.MultiValueMap<String,String> fields,
            @RequestPart(required = false) MultipartFile photo) {
        if (photo != null && !photo.isEmpty()) com.projeto_pi.Projeto.PI.service.Arquivos.foto(photo);
        Map<String,String> simples = new HashMap<>();
        fields.forEach((key, values) -> { if (!key.equals("segments") && !key.equals("stages") && !values.isEmpty()) simples.put(key, values.get(0)); });
        Map<String,Object> values = normalize(simples);
        for (String lista : List.of("segments", "stages")) {
            List<String> itens = fields.getOrDefault(lista, List.of()).stream().filter(v -> !v.isBlank()).toList();
            if (!itens.isEmpty()) values.put(lista, itens);
        }
        Usuario user = service.cadastrarInvestidorMentor(mapper.convertValue(values, CadastroInvestidorMentorDTO.class));
        if (photo != null && !photo.isEmpty()) perfis.salvarFoto(user.getId(), photo);
        return ResponseEntity.status(201).body(new UsuarioResponseDTO(user.getId(), user.getEmail(), user.getPapel(), user.getStatus()));
    }
    @PostMapping(value = "/startup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<UsuarioResponseDTO> startup(@RequestParam Map<String,String> fields,
            @RequestPart(required = false) MultipartFile pitchDeck,
            @RequestPart(required = false) MultipartFile businessCanvas,
            @RequestPart(required = false) MultipartFile businessModelCanvas,
            @RequestPart(required = false) MultipartFile photo) throws IOException {
        validate(pitchDeck); validate(businessCanvas); validate(businessModelCanvas);
        if (photo != null && !photo.isEmpty()) com.projeto_pi.Projeto.PI.service.Arquivos.foto(photo);
        Usuario user = service.cadastrarStartup(mapper.convertValue(normalize(fields), CadastroStartupDTO.class));
        Startup s = user.getStartup();
        if (pitchDeck != null && !pitchDeck.isEmpty()) {
            s.setPitchDeckData(pitchDeck.getBytes()); s.setPitchDeckName(pitchDeck.getOriginalFilename());
        }
        MultipartFile canvas = businessModelCanvas != null ? businessModelCanvas : businessCanvas;
        if (canvas != null && !canvas.isEmpty()) {
            s.setBusinessCanvasData(canvas.getBytes()); s.setBusinessCanvasName(canvas.getOriginalFilename());
        }
        if (photo != null && !photo.isEmpty()) perfis.salvarFoto(user.getId(), photo);
        return ResponseEntity.status(201).body(new UsuarioResponseDTO(user.getId(), user.getEmail(), user.getPapel(), user.getStatus()));
    }
    private Map<String,Object> normalize(Map<String,String> fields) {
        Map<String,Object> values = new HashMap<>();
        fields.forEach((key,value) -> {
            if (Set.of("terms","privacy","dataProcessing","aceitaTermos","cientePrivacidade","autorizaTratamento","consentimentoLGPD").contains(key))
                values.put(key, value.equals("on") || value.equals("true"));
            else values.put(key, value.isEmpty() ? null : value);
        });
        return values;
    }
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) return;
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!name.matches(".*\\.(pdf|ppt|pptx)$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie PDF, PPT ou PPTX");
        if (file.getSize() > 10 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo excede 10 MB");
    }
}
