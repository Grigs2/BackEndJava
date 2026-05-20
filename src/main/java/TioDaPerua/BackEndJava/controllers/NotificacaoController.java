package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.NotificacaoDTO;
import TioDaPerua.BackEndJava.entities.Notificacao;
import TioDaPerua.BackEndJava.services.NotificacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/Notificacao")
public class NotificacaoController {

    @Autowired
    private NotificacaoService notificacaoService;

    @PostMapping("/Enviar")
    public NotificacaoDTO Enviar(@RequestBody NotificacaoDTO dto) {
        if (dto == null) return null;
        Notificacao entity = NotificacaoDTO.toEntity(dto);
        return NotificacaoDTO.toDTO(notificacaoService.enviarNotificacao(entity, dto.remetenteId(), dto.destinatarioId()));
    }

    @GetMapping("/Listar/{id_usuario}")
    public List<NotificacaoDTO> Listar(@PathVariable Long id_usuario) {
        if (id_usuario == null) return null;
        return notificacaoService.listarPorUsuario(id_usuario).stream()
                .map(NotificacaoDTO::toDTO)
                .collect(Collectors.toList());
    }
}
