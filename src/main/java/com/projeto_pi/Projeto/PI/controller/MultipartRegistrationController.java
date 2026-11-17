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
    public MultipartRegistrationController(UsuarioService service, JsonMapper mapper) {
        this.service = service; this.mapper = mapper;
    }
    @PostMapping(value = "/startup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<UsuarioResponseDTO> startup(@RequestParam Map<String,String> fields,
            @RequestPart(required = false) MultipartFile pitchDeck,
            @RequestPart(required = false) MultipartFile businessCanvas,
            @RequestPart(required = false) MultipartFile businessModelCanvas) throws IOException {
        validate(pitchDeck); validate(businessCanvas); validate(businessModelCanvas);
        Usuario user = service.cadastrarStartup(mapper.convertValue(normalize(fields), CadastroStartupDTO.class));
        Startup s = user.getStartup();
        if (pitchDeck != null && !pitchDeck.isEmpty()) {
            s.setPitchDeckData(pitchDeck.getBytes()); s.setPitchDeckName(pitchDeck.getOriginalFilename());
        }
        MultipartFile canvas = businessModelCanvas != null ? businessModelCanvas : businessCanvas;
        if (canvas != null && !canvas.isEmpty()) {
            s.setBusinessCanvasData(canvas.getBytes()); s.setBusinessCanvasName(canvas.getOriginalFilename());
        }
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
