package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.NotificacaoDTO;
import TioDaPerua.BackEndJava.DTOs.UsuarioResumoDTO;
import TioDaPerua.BackEndJava.entities.Notificacao;
import TioDaPerua.BackEndJava.services.NotificacaoService;
import TioDaPerua.BackEndJava.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/Notificacao")
public class NotificacaoController {

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/Enviar")
    public ResponseEntity<NotificacaoDTO> Enviar(@RequestBody NotificacaoDTO dto) {
        if (dto == null) {
            return ResponseEntity.badRequest().build();
        }
        Notificacao entity = NotificacaoDTO.toEntity(dto);
        Notificacao salva = notificacaoService.enviarNotificacao(entity, dto.remetenteId(), dto.destinatarioId());
        
        UsuarioResumoDTO remetenteResumo = usuarioService.getUsuarioResumo(salva.getRemetente());
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificacaoDTO.toDTO(salva, remetenteResumo));
    }

    @GetMapping("/Listar/{id_usuario}")
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
    public ResponseEntity<Void> VisualizarNotificacao(@PathVariable Long id_notificacao) {
        if (id_notificacao == null) {
            return ResponseEntity.badRequest().build();
        }
        notificacaoService.visualizarNotificacao(id_notificacao);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ListarUsuarios")
    public ResponseEntity<List<UsuarioResumoDTO>> ListarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuariosResumo());
    }
}
