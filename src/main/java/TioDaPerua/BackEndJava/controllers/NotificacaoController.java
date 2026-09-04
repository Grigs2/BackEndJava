package TioDaPerua.BackEndJava.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import TioDaPerua.BackEndJava.DTOs.NotificacaoDTO;
import TioDaPerua.BackEndJava.DTOs.UsuarioResumoDTO;
import TioDaPerua.BackEndJava.entities.Notificacao;
import TioDaPerua.BackEndJava.services.NotificacaoService;
import TioDaPerua.BackEndJava.services.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/Notificacao")
public class NotificacaoController {

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/Enviar")
    public ResponseEntity<NotificacaoDTO> Enviar(@NotNull(message = "Input está nullo")@Valid @RequestBody NotificacaoDTO dto) {
        Notificacao entity = NotificacaoDTO.toEntity(dto);
        Notificacao salva = notificacaoService.enviarNotificacao(entity, dto.remetenteId(), dto.destinatarioId());
        
        UsuarioResumoDTO remetenteResumo = usuarioService.getUsuarioResumo(salva.getRemetente());
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificacaoDTO.toDTO(salva, remetenteResumo));
    }

    @GetMapping("/Listar/{id_usuario}")
    public ResponseEntity<List<NotificacaoDTO>> Listar(@NotNull(message = "ID do usuário está nullo") @PathVariable Long id_usuario) {
        List<NotificacaoDTO> notificacoes = notificacaoService.listarNaoVistasPorUsuario(id_usuario).stream()
                .map(n -> {
                    UsuarioResumoDTO remetenteResumo = usuarioService.getUsuarioResumo(n.getRemetente());
                    return NotificacaoDTO.toDTO(n, remetenteResumo);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(notificacoes);
    }

    @PutMapping("/VisualizarNotificacao/{id_notificacao}")
    public ResponseEntity<Void> VisualizarNotificacao(@NotNull(message = "ID da notificação está nullo") @PathVariable Long id_notificacao) {

        notificacaoService.visualizarNotificacao(id_notificacao);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ListarUsuarios")
    public ResponseEntity<List<UsuarioResumoDTO>> ListarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuariosResumo());
    }
}
