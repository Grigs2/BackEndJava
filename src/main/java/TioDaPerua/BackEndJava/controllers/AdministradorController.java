package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.DocumentoMotoristaDTO;
import TioDaPerua.BackEndJava.DTOs.RevisarDocumentoDTO;
import TioDaPerua.BackEndJava.DTOs.AtribuirResponsavelCriadorDTO;
import TioDaPerua.BackEndJava.DTOs.DependenteResponsavelCriadorPendenteDTO;
import TioDaPerua.BackEndJava.DTOs.AdminAuditoriaDTO;
import TioDaPerua.BackEndJava.DTOs.AdminEscolaResumoDTO;
import TioDaPerua.BackEndJava.DTOs.AdminReativarContaDTO;
import TioDaPerua.BackEndJava.DTOs.AdminSuspenderContaDTO;
import TioDaPerua.BackEndJava.DTOs.AdminUsuarioResumoDTO;
import TioDaPerua.BackEndJava.DTOs.AdminVinculoMotoristaEscolaDTO;
import TioDaPerua.BackEndJava.DTOs.AdminVinculoResponsavelDTO;
import TioDaPerua.BackEndJava.enums.StatusVinculoEscola;
import TioDaPerua.BackEndJava.services.AdminGestaoService;
import TioDaPerua.BackEndJava.services.AuditoriaAdministrativaService;
import TioDaPerua.BackEndJava.services.DocumentoMotoristaService;
import TioDaPerua.BackEndJava.services.RedeResponsaveisService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;

import java.time.Instant;

import java.util.List;

@RestController
@RequestMapping("/Admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdministradorController {

    private final DocumentoMotoristaService documentoMotoristaService;
    private final RedeResponsaveisService redeResponsaveisService;
    private final AdminGestaoService adminGestaoService;
    private final AuditoriaAdministrativaService auditoriaService;

    public AdministradorController(DocumentoMotoristaService documentoMotoristaService,
                                   RedeResponsaveisService redeResponsaveisService,
                                   AdminGestaoService adminGestaoService,
                                   AuditoriaAdministrativaService auditoriaService) {
        this.documentoMotoristaService = documentoMotoristaService;
        this.redeResponsaveisService = redeResponsaveisService;
        this.adminGestaoService = adminGestaoService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/Documentos/Pendentes")
    public ResponseEntity<List<DocumentoMotoristaDTO>> listarDocumentosPendentes(
            @AuthenticationPrincipal Jwt jwt) {
        List<DocumentoMotoristaDTO> documentos = documentoMotoristaService.listarPendentes();
        auditoriaService.registrarConsulta(
                Long.valueOf(jwt.getSubject()), "DOCUMENTOS_PENDENTES_CONSULTADOS", "DOCUMENTO_MOTORISTA");
        return ResponseEntity.ok(documentos);
    }

    @PutMapping("/Documentos/{idDocumento}/Revisar")
    public ResponseEntity<DocumentoMotoristaDTO> revisarDocumento(
            @PathVariable Long idDocumento,
            @Valid @RequestBody RevisarDocumentoDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(documentoMotoristaService.revisar(
                Long.valueOf(jwt.getSubject()), idDocumento, dto));
    }

    @GetMapping("/Dependentes/ResponsavelCriadorPendentes")
    public ResponseEntity<List<DependenteResponsavelCriadorPendenteDTO>> listarResponsaveisCriadoresPendentes(
            @AuthenticationPrincipal Jwt jwt) {
        List<DependenteResponsavelCriadorPendenteDTO> pendencias =
                redeResponsaveisService.listarDependentesComResponsavelCriadorIndefinido();
        auditoriaService.registrarConsulta(
                Long.valueOf(jwt.getSubject()), "RESPONSAVEIS_CRIADORES_PENDENTES_CONSULTADOS", "DEPENDENTE");
        return ResponseEntity.ok(pendencias);
    }

    @PutMapping("/Dependentes/{idDependente}/ResponsavelCriador")
    public ResponseEntity<Void> atribuirResponsavelCriador(
            @PathVariable Long idDependente,
            @Valid @RequestBody AtribuirResponsavelCriadorDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        if (dto == null) {
            return ResponseEntity.badRequest().build();
        }
        redeResponsaveisService.atribuirResponsavelCriador(
                idDependente, dto.responsavelId(), Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/Usuarios")
    public ResponseEntity<Page<AdminUsuarioResumoDTO>> pesquisarUsuarios(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String tipoPerfil,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean suspensa,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(adminGestaoService.pesquisarUsuarios(
                Long.valueOf(jwt.getSubject()), id, tipoPerfil, email, suspensa, pagina, tamanho));
    }

    @GetMapping("/Escolas")
    public ResponseEntity<Page<AdminEscolaResumoDTO>> pesquisarEscolas(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cnpj,
            @RequestParam(required = false) Boolean suspensa,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(adminGestaoService.pesquisarEscolas(
                Long.valueOf(jwt.getSubject()), nome, cnpj, suspensa, pagina, tamanho));
    }

    @GetMapping("/Vinculos/MotoristaEscola")
    public ResponseEntity<Page<AdminVinculoMotoristaEscolaDTO>> pesquisarVinculosMotoristaEscola(
            @RequestParam(required = false) Long escolaId,
            @RequestParam(required = false) Long motoristaId,
            @RequestParam(required = false) StatusVinculoEscola status,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(adminGestaoService.pesquisarVinculosMotoristaEscola(
                Long.valueOf(jwt.getSubject()), escolaId, motoristaId, status, pagina, tamanho));
    }

    @GetMapping("/Vinculos/ResponsavelDependente")
    public ResponseEntity<Page<AdminVinculoResponsavelDTO>> pesquisarVinculosResponsavelDependente(
            @RequestParam(required = false) Long dependenteId,
            @RequestParam(required = false) Long responsavelId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(adminGestaoService.pesquisarVinculosResponsavelDependente(
                Long.valueOf(jwt.getSubject()), dependenteId, responsavelId, pagina, tamanho));
    }

    @GetMapping("/Auditoria")
    public ResponseEntity<Page<AdminAuditoriaDTO>> pesquisarAuditoria(
            @RequestParam(required = false) Long atorId,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) String tipoAlvo,
            @RequestParam(required = false) Long alvoId,
            @RequestParam(required = false) Instant inicio,
            @RequestParam(required = false) Instant fim,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(adminGestaoService.pesquisarAuditoria(
                Long.valueOf(jwt.getSubject()), atorId, acao, tipoAlvo, alvoId, inicio, fim, pagina, tamanho));
    }

    @PutMapping("/Usuarios/{idUsuario}/Suspender")
    public ResponseEntity<Void> suspenderUsuario(
            @PathVariable Long idUsuario,
            @Valid @RequestBody AdminSuspenderContaDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        adminGestaoService.suspenderUsuario(Long.valueOf(jwt.getSubject()), idUsuario, dto.motivo());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/Usuarios/{idUsuario}/Reativar")
    public ResponseEntity<Void> reativarUsuario(
            @PathVariable Long idUsuario,
            @Valid @RequestBody(required = false) AdminReativarContaDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        adminGestaoService.reativarUsuario(
                Long.valueOf(jwt.getSubject()), idUsuario, dto == null ? null : dto.motivo());
        return ResponseEntity.noContent().build();
    }
}
