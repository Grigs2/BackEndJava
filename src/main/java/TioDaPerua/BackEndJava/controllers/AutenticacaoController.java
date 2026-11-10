package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.AutenticacaoResponseDTO;
import TioDaPerua.BackEndJava.DTOs.AlterarSenhaDTO;
import TioDaPerua.BackEndJava.DTOs.LoginRequestDTO;
import TioDaPerua.BackEndJava.DTOs.RefreshTokenRequestDTO;
import TioDaPerua.BackEndJava.DTOs.SolicitarRedefinicaoSenhaDTO;
import TioDaPerua.BackEndJava.DTOs.RedefinirSenhaDTO;
import TioDaPerua.BackEndJava.DTOs.MensagemGenericaDTO;
import TioDaPerua.BackEndJava.services.AutenticacaoService;
import TioDaPerua.BackEndJava.services.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/Autenticacao")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Autenticacao")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;
    private final PasswordResetService passwordResetService;

    public AutenticacaoController(AutenticacaoService autenticacaoService,
                                  PasswordResetService passwordResetService) {
        this.autenticacaoService = autenticacaoService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/Entrar")
    public ResponseEntity<AutenticacaoResponseDTO> entrar(@Valid @RequestBody LoginRequestDTO dto) {
        if (dto == null || dto.email() == null || dto.senha() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(autenticacaoService.autenticar(dto.email(), dto.senha()));
    }

    @PostMapping("/Renovar")
    public ResponseEntity<AutenticacaoResponseDTO> renovar(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        if (dto == null || dto.refreshToken() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(autenticacaoService.renovar(dto.refreshToken()));
    }

    @PostMapping("/Sair")
    public ResponseEntity<Void> sair(@Valid @RequestBody RefreshTokenRequestDTO dto,
                                     @AuthenticationPrincipal Jwt jwt) {
        if (dto == null || dto.refreshToken() == null) {
            return ResponseEntity.badRequest().build();
        }
        autenticacaoService.encerrarSessao(
                dto.refreshToken(),
                jwt.getClaimAsString("sid"),
                Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    /** Aceite da versão vigente dos Termos de Uso e da Política de Privacidade (quando termosPendentes = true). */
    @PostMapping("/AceitarTermos")
    public ResponseEntity<Void> aceitarTermos(@AuthenticationPrincipal Jwt jwt) {
        autenticacaoService.aceitarTermos(Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/AlterarSenha")
    public ResponseEntity<Void> alterarSenha(@Valid @RequestBody AlterarSenhaDTO dto,
                                             @AuthenticationPrincipal Jwt jwt) {
        if (dto == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        autenticacaoService.alterarSenha(
                Long.valueOf(jwt.getSubject()), dto.senhaAtual(), dto.novaSenha());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/SolicitarRedefinicaoSenha")
    public ResponseEntity<MensagemGenericaDTO> solicitarRedefinicaoSenha(
            @Valid @RequestBody SolicitarRedefinicaoSenhaDTO dto,
            HttpServletRequest request) {
        if (dto == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        passwordResetService.solicitarRedefinicao(dto.email(), request.getRemoteAddr());
        return ResponseEntity.accepted().body(new MensagemGenericaDTO(
                "Se houver uma conta e o pedido puder ser processado, enviaremos instruções para redefinir a senha."));
    }

    @PostMapping("/RedefinirSenha")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        passwordResetService.redefinirSenha(dto.token(), dto.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
