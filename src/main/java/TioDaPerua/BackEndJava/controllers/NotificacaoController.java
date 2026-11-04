package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.NotificacaoDTO;
import TioDaPerua.BackEndJava.DTOs.UsuarioResumoDTO;
import TioDaPerua.BackEndJava.entities.Notificacao;
import TioDaPerua.BackEndJava.services.NotificacaoService;
import TioDaPerua.BackEndJava.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/Notificacao")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Notificacao")
public class NotificacaoController {

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/Enviar")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isUsuarioAutenticado(authentication, #p0.remetenteId())")
    public ResponseEntity<NotificacaoDTO> Enviar(@Valid @RequestBody NotificacaoDTO dto) {
        if (dto == null) {
            return ResponseEntity.badRequest().build();
        }
        Notificacao entity = NotificacaoDTO.toEntity(dto);
        Notificacao salva = notificacaoService.enviarNotificacao(entity, dto.remetenteId(), dto.destinatarioId());
        
        UsuarioResumoDTO remetenteResumo = usuarioService.getUsuarioResumo(salva.getRemetente());
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificacaoDTO.toDTO(salva, remetenteResumo));
    }

    @GetMapping("/Listar/{id_usuario}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isUsuarioAutenticado(authentication, #p0)")
    public ResponseEntity<List<NotificacaoDTO>> Listar(@PathVariable Long id_usuario) {
        if (id_usuario == null) {
            return ResponseEntity.badRequest().build();
        }
        List<NotificacaoDTO> notificacoes = notificacaoService.listarNaoVistasPorUsuario(id_usuario).stream()
                .map(n -> {
                    UsuarioResumoDTO remetenteResumo = usuarioService.getUsuarioResumo(n.getRemetente());
                    return NotificacaoDTO.toDTO(n, remetenteResumo);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(notificacoes);
    }

    @PutMapping("/VisualizarNotificacao/{id_notificacao}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isDestinatarioNotificacao(authentication, #p0)")
    public ResponseEntity<Void> VisualizarNotificacao(@PathVariable Long id_notificacao) {
        if (id_notificacao == null) {
            return ResponseEntity.badRequest().build();
        }
        notificacaoService.visualizarNotificacao(id_notificacao);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ListarUsuarios")
    @PreAuthorize("hasAnyRole('MOTORISTA', 'RESPONSAVEL', 'ESCOLA')")
    public ResponseEntity<List<UsuarioResumoDTO>> ListarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuariosResumo());
    }
}
