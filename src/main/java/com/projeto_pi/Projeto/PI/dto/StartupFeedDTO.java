package com.projeto_pi.Projeto.PI.dto;
import com.projeto_pi.Projeto.PI.entity.Startup;
public record StartupFeedDTO(String id, String name, String location, String segment,
        String stage, String description, String tone, Long userId, boolean hasPhoto) {
    public static StartupFeedDTO from(com.projeto_pi.Projeto.PI.matching.StartupResumo s, boolean hasPhoto) {
        return new StartupFeedDTO(s.id().toString(), text(s.nome()), s.local(), text(s.segmento()),
            text(s.estagio()), text(s.resumo()), "bg-[#7250f4]", s.usuarioId(), hasPhoto);
    }
    public static StartupFeedDTO from(Startup s) {
        String city = text(s.getCity()), state = text(s.getState());
        return new StartupFeedDTO(s.getId().toString(), text(s.getNome()),
            city + (!city.isEmpty() && !state.isEmpty() ? "/" : "") + state,
            text(s.getSegmento()), text(s.getEstagio()), text(s.getSummary()), "bg-[#7250f4]", null, false);
    }
    private static String text(String value) { return value == null ? "" : value; }
}
