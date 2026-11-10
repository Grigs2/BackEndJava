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

    @Autowired
    private TioDaPerua.BackEndJava.services.PushService pushService;

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
    public ResponseEntity<List<NotificacaoDTO>> Listar(
            @PathVariable Long id_usuario,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        if (id_usuario == null) {
            return ResponseEntity.badRequest().build();
        }
        // Sem parâmetros mantém o comportamento antigo (todas as não lidas). Com parâmetros: central paginada,
        // status=TODAS|NAO_LIDAS, grupo=VIAGEM|ALERTA|SOLICITACAO|AVISO, pagina a partir de 0.
        boolean central = status != null || grupo != null || pagina != null || tamanho != null;
        List<TioDaPerua.BackEndJava.entities.Notificacao> encontradas = central
                ? notificacaoService.listar(id_usuario, !"TODAS".equalsIgnoreCase(status), grupo,
                        pagina == null ? 0 : pagina, tamanho == null ? NotificacaoService.TAMANHO_PAGINA_PADRAO : tamanho)
                : notificacaoService.listarNaoVistasPorUsuario(id_usuario);
        List<NotificacaoDTO> notificacoes = encontradas.stream()
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

    /** Contador do sino: quantas notificações não lidas o usuário autenticado tem. */
    @GetMapping("/NaoLidas/Contagem")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, Long>> contarNaoLidas(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        return ResponseEntity.ok(java.util.Map.of("naoLidas",
                notificacaoService.contarNaoLidas(Long.valueOf(jwt.getSubject()))));
    }

    /** Marca todas as notificações do usuário autenticado como lidas. */
    @PutMapping("/MarcarTodasLidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.Map<String, Integer>> marcarTodasLidas(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        return ResponseEntity.ok(java.util.Map.of("marcadas",
                notificacaoService.marcarTodasLidas(Long.valueOf(jwt.getSubject()))));
    }

    /** Registra o aparelho do usuário autenticado para receber push. Corpo: { expoToken, plataforma }. */
    @PostMapping("/Dispositivos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> registrarDispositivo(
            @Valid @RequestBody TioDaPerua.BackEndJava.DTOs.DispositivoPushDTO dto,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        pushService.registrar(Long.valueOf(jwt.getSubject()), dto.expoToken(), dto.plataforma());
        return ResponseEntity.noContent().build();
    }

    /** Para de enviar push para o aparelho (ao sair da conta). Corpo: { expoToken }. */
    @PostMapping("/Dispositivos/Remover")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removerDispositivo(
            @Valid @RequestBody TioDaPerua.BackEndJava.DTOs.DispositivoPushDTO dto,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        pushService.remover(Long.valueOf(jwt.getSubject()), dto.expoToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ListarUsuarios")
    @PreAuthorize("hasAnyRole('MOTORISTA', 'RESPONSAVEL', 'ESCOLA')")
    public ResponseEntity<List<UsuarioResumoDTO>> ListarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuariosResumo());
    }
}
