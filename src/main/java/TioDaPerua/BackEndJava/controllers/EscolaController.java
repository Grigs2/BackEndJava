package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.EscolaDTO;
import TioDaPerua.BackEndJava.DTOs.CriarConviteMotoristaEscolaDTO;
import TioDaPerua.BackEndJava.DTOs.HistoricoVinculoEscolaDTO;
import TioDaPerua.BackEndJava.DTOs.LocalizarMotoristaEscolaDTO;
import TioDaPerua.BackEndJava.DTOs.MotivoVinculoEscolaDTO;
import TioDaPerua.BackEndJava.DTOs.MotoristaBuscaDTO;
import TioDaPerua.BackEndJava.DTOs.UsuarioDTO;
import TioDaPerua.BackEndJava.DTOs.VinculoEscolaDTO;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.services.EscolaService;
import TioDaPerua.BackEndJava.services.UsuarioService;
import TioDaPerua.BackEndJava.services.VinculoEscolaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/Escola")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Escola")
public class EscolaController {

    @Autowired
    private EscolaService escolaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private VinculoEscolaService vinculoEscolaService;

    @GetMapping("/Perfil")
    @PreAuthorize("hasRole('ESCOLA')")
    public ResponseEntity<EscolaDTO> consultarPerfil(@AuthenticationPrincipal Jwt jwt) {
        Usuario usuario = usuarioService.findByID(Long.valueOf(jwt.getSubject()));
        return ResponseEntity.ok(EscolaDTO.toDTO(usuarioService.logarEscola(usuario)));
    }

    @PutMapping("/AlterarInfos")
    @PreAuthorize("#p0 != null && #p0.usuarioDTO() != null && @resourceAuthorization.isEscola(authentication, #p0.id()) && @resourceAuthorization.isUsuarioAutenticado(authentication, #p0.usuarioDTO().id())")
    public ResponseEntity<EscolaDTO> AlterarInfos(@Valid @RequestBody EscolaDTO dto) {
        if (dto == null || dto.usuarioDTO() == null || dto.usuarioDTO().senha() == null) {
            return ResponseEntity.badRequest().build();
        }

        usuarioService.validarSenha(dto.usuarioDTO().id(), dto.usuarioDTO().senha());

        Escola atualizada = escolaService.alterarInfos(EscolaDTO.toEntity(dto));
        return ResponseEntity.ok(EscolaDTO.toDTO(atualizada));
    }

    @PostMapping("/Cadastrar")
    public ResponseEntity<EscolaDTO> cadastrar(@Valid @RequestBody EscolaDTO dto) {
        if (dto == null || dto.id() != null || dto.usuarioDTO() == null
                || dto.usuarioDTO().id() != null || dto.enderecoOperacionalDTO() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!"ESCOLA".equals(dto.usuarioDTO().tipoPerfil())) {
            return ResponseEntity.badRequest().build();
        }

        Escola escola = EscolaDTO.toEntity(dto);
        Escola escolaSalva = escolaService.cadastrarEscola(escola);
        return ResponseEntity.status(HttpStatus.CREATED).body(EscolaDTO.toDTO(escolaSalva));
    }

    @PostMapping("/Autenticar")
    public ResponseEntity<EscolaDTO> autenticar(@Valid @RequestBody UsuarioDTO dto) {
        if (dto == null || dto.email() == null || dto.senha() == null) {
            return ResponseEntity.badRequest().build();
        }

        Usuario usuario = usuarioService.autenticar(dto.email(), dto.senha());
        Escola escola = usuarioService.logarEscola(usuario);
        return ResponseEntity.ok(EscolaDTO.toDTO(escola));
    }

    @PostMapping("/Vinculos/Motoristas/Buscar")
    @PreAuthorize("hasRole('ESCOLA')")
    public ResponseEntity<List<MotoristaBuscaDTO>> buscarMotoristas(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LocalizarMotoristaEscolaDTO dto) {
        return ResponseEntity.ok(vinculoEscolaService.buscarMotoristas(
                Long.valueOf(jwt.getSubject()), dto.email(), dto.cpf()));
    }

    @PostMapping("/Vinculos")
    @PreAuthorize("hasRole('ESCOLA')")
    public ResponseEntity<VinculoEscolaDTO> convidarMotorista(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CriarConviteMotoristaEscolaDTO dto) {
        VinculoEscolaDTO vinculo = vinculoEscolaService.convidarMotorista(
                Long.valueOf(jwt.getSubject()), dto.motoristaId());
        return ResponseEntity.status(HttpStatus.CREATED).body(vinculo);
    }

    @GetMapping("/Vinculos")
    @PreAuthorize("hasRole('ESCOLA')")
    public ResponseEntity<List<VinculoEscolaDTO>> listarVinculos(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(vinculoEscolaService.listarDaEscola(Long.valueOf(jwt.getSubject())));
    }

    @GetMapping("/Vinculos/Pendentes")
    @PreAuthorize("hasRole('ESCOLA')")
    public ResponseEntity<List<VinculoEscolaDTO>> listarPendentes(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(vinculoEscolaService.listarPendentesDaEscola(Long.valueOf(jwt.getSubject())));
    }

    @GetMapping("/Vinculos/{idVinculo}/Historico")
    @PreAuthorize("hasRole('ESCOLA')")
    public ResponseEntity<List<HistoricoVinculoEscolaDTO>> listarHistorico(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long idVinculo) {
        return ResponseEntity.ok(vinculoEscolaService.listarHistorico(
                Long.valueOf(jwt.getSubject()), idVinculo));
    }

    @PostMapping("/Vinculos/{idVinculo}/Aprovar")
    @PreAuthorize("hasRole('ESCOLA')")
    public ResponseEntity<VinculoEscolaDTO> aprovarVinculo(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long idVinculo,
            @Valid @RequestBody(required = false) MotivoVinculoEscolaDTO dto) {
        return ResponseEntity.ok(vinculoEscolaService.responderComoEscola(
                Long.valueOf(jwt.getSubject()), idVinculo, true, dto == null ? null : dto.motivo()));
    }

    @PostMapping("/Vinculos/{idVinculo}/Recusar")
    @PreAuthorize("hasRole('ESCOLA')")
    public ResponseEntity<VinculoEscolaDTO> recusarVinculo(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long idVinculo,
            @Valid @RequestBody(required = false) MotivoVinculoEscolaDTO dto) {
        return ResponseEntity.ok(vinculoEscolaService.responderComoEscola(
                Long.valueOf(jwt.getSubject()), idVinculo, false, dto == null ? null : dto.motivo()));
    }

    @PostMapping("/Vinculos/{idVinculo}/Suspender")
    @PreAuthorize("hasRole('ESCOLA')")
    public ResponseEntity<VinculoEscolaDTO> suspenderVinculo(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long idVinculo,
            @Valid @RequestBody(required = false) MotivoVinculoEscolaDTO dto) {
        return ResponseEntity.ok(vinculoEscolaService.suspender(
                Long.valueOf(jwt.getSubject()), idVinculo, dto == null ? null : dto.motivo()));
    }

    @PostMapping("/Vinculos/{idVinculo}/Reativar")
    @PreAuthorize("hasRole('ESCOLA')")
    public ResponseEntity<VinculoEscolaDTO> reativarVinculo(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long idVinculo,
            @Valid @RequestBody(required = false) MotivoVinculoEscolaDTO dto) {
        return ResponseEntity.ok(vinculoEscolaService.reativar(
                Long.valueOf(jwt.getSubject()), idVinculo, dto == null ? null : dto.motivo()));
    }

    @PostMapping("/Vinculos/{idVinculo}/Encerrar")
    @PreAuthorize("hasRole('ESCOLA')")
    public ResponseEntity<VinculoEscolaDTO> encerrarVinculo(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long idVinculo,
            @Valid @RequestBody(required = false) MotivoVinculoEscolaDTO dto) {
        return ResponseEntity.ok(vinculoEscolaService.encerrar(
                Long.valueOf(jwt.getSubject()), idVinculo, dto == null ? null : dto.motivo()));
    }
}
