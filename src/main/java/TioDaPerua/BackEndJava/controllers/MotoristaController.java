package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.MotoristaDTO;
import TioDaPerua.BackEndJava.DTOs.EscolaBuscaDTO;
import TioDaPerua.BackEndJava.DTOs.HistoricoVinculoEscolaDTO;
import TioDaPerua.BackEndJava.DTOs.MotivoVinculoEscolaDTO;
import TioDaPerua.BackEndJava.DTOs.SolicitarVinculoEscolaDTO;
import TioDaPerua.BackEndJava.DTOs.UsuarioDTO;
import TioDaPerua.BackEndJava.DTOs.VeiculoDTO;
import TioDaPerua.BackEndJava.DTOs.DocumentoMotoristaDTO;
import TioDaPerua.BackEndJava.DTOs.VinculoEscolaDTO;
import TioDaPerua.BackEndJava.DTOs.SubmeterDocumentoMotoristaDTO;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.services.MotoristaService;
import TioDaPerua.BackEndJava.services.UsuarioService;
import TioDaPerua.BackEndJava.services.DocumentoMotoristaService;
import TioDaPerua.BackEndJava.services.VinculoEscolaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;

@RestController
@RequestMapping("/Motorista")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Motorista")
public class MotoristaController {

    @Autowired
    private MotoristaService motoristaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private DocumentoMotoristaService documentoMotoristaService;

    @Autowired
    private VinculoEscolaService vinculoEscolaService;

    @GetMapping("/Perfil")
    @PreAuthorize("hasRole('MOTORISTA')")
    public ResponseEntity<MotoristaDTO> consultarPerfil(@AuthenticationPrincipal Jwt jwt) {
        Usuario usuario = usuarioService.findByID(Long.valueOf(jwt.getSubject()));
        return ResponseEntity.ok(MotoristaDTO.toDTO(usuarioService.logarMotorista(usuario)));
    }

    @GetMapping("/{id_motorista}/Documentos")
    @PreAuthorize("@resourceAuthorization.isMotorista(authentication, #p0)")
    public ResponseEntity<List<DocumentoMotoristaDTO>> listarDocumentos(@PathVariable Long id_motorista) {
        return ResponseEntity.ok(documentoMotoristaService.listarDoMotorista(id_motorista));
    }

    @PostMapping("/{id_motorista}/Documentos")
    @PreAuthorize("@resourceAuthorization.isMotorista(authentication, #p0)")
    public ResponseEntity<DocumentoMotoristaDTO> submeterDocumento(
            @PathVariable Long id_motorista,
            @Valid @RequestBody SubmeterDocumentoMotoristaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentoMotoristaService.submeter(id_motorista, dto));
    }

    @PutMapping("/AlterarInfos")
    @PreAuthorize("#p0 != null && #p0.usuarioDTO() != null && @resourceAuthorization.isMotorista(authentication, #p0.id()) && @resourceAuthorization.isUsuarioAutenticado(authentication, #p0.usuarioDTO().id())")
    public ResponseEntity<MotoristaDTO> AlterarInfos(@Valid @RequestBody MotoristaDTO dto) {
        if (dto == null || dto.usuarioDTO() == null || dto.usuarioDTO().senha() == null) {
            return ResponseEntity.badRequest().build();
        }

        usuarioService.validarSenha(dto.usuarioDTO().id(), dto.usuarioDTO().senha());

        Motorista atualizado = motoristaService.alterarInfos(MotoristaDTO.toEntity(dto));
        return ResponseEntity.ok(MotoristaDTO.toDTO(atualizado));
    }

    @PostMapping("/Cadastrar")
    public ResponseEntity<MotoristaDTO> Cadastrar(@Valid @RequestBody MotoristaDTO dto) {
        if (dto == null || dto.id() != null || dto.usuarioDTO() == null
                || dto.usuarioDTO().id() != null || dto.veiculoDTO() != null
                || dto.enderecoOperacionalDTO() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!"MOTORISTA".equals(dto.usuarioDTO().tipoPerfil())) {
            return ResponseEntity.badRequest().build();
        }

        Motorista motoristaSalvo = motoristaService.cadastraMotorista(MotoristaDTO.toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(MotoristaDTO.toDTO(motoristaSalvo));
    }

    @PostMapping("/CadastrarVeiculo/{id_motorista}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isMotorista(authentication, #p0)")
    public ResponseEntity<MotoristaDTO> CadastrarVeiculo(@PathVariable Long id_motorista, @Valid @RequestBody VeiculoDTO dto) {
        if (dto == null || dto.id() != null || id_motorista == null) {
            return ResponseEntity.badRequest().build();
        }
        Motorista motoristaComVeiculo = motoristaService.cadastrarVeiculo(id_motorista, VeiculoDTO.toEntity(dto));
        return ResponseEntity.ok(MotoristaDTO.toDTO(motoristaComVeiculo));
    }

    @PutMapping("/{id_motorista}/Veiculo")
    @PreAuthorize("@resourceAuthorization.isMotorista(authentication, #p0)")
    public ResponseEntity<MotoristaDTO> alterarVeiculo(
            @PathVariable Long id_motorista,
            @Valid @RequestBody VeiculoDTO dto) {
        if (dto == null) {
            return ResponseEntity.badRequest().build();
        }
        Motorista motorista = motoristaService.alterarVeiculo(id_motorista, VeiculoDTO.toEntity(dto));
        return ResponseEntity.ok(MotoristaDTO.toDTO(motorista));
    }

    @PostMapping("/Autenticar")
    public ResponseEntity<MotoristaDTO> autenticar(@Valid @RequestBody UsuarioDTO dto) {
        if (dto == null || dto.email() == null || dto.senha() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (dto.email().isEmpty() || dto.senha().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Usuario usuarioLogado = usuarioService.autenticar(dto.email(), dto.senha());
        Motorista motorista = usuarioService.logarMotorista(usuarioLogado);
        return ResponseEntity.ok(MotoristaDTO.toDTO(motorista));
    }

    @GetMapping("/Escolas")
    @PreAuthorize("hasRole('MOTORISTA')")
    public ResponseEntity<List<EscolaBuscaDTO>> buscarEscolas(@RequestParam String termo) {
        return ResponseEntity.ok(vinculoEscolaService.buscarEscolas(termo));
    }

    @PostMapping("/Escolas/Vinculos")
    @PreAuthorize("hasRole('MOTORISTA')")
    public ResponseEntity<VinculoEscolaDTO> solicitarVinculo(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SolicitarVinculoEscolaDTO dto) {
        VinculoEscolaDTO vinculo = vinculoEscolaService.solicitarVinculo(
                Long.valueOf(jwt.getSubject()), dto.cnpj());
        return ResponseEntity.status(HttpStatus.CREATED).body(vinculo);
    }

    @GetMapping("/Vinculos")
    @PreAuthorize("hasRole('MOTORISTA')")
    public ResponseEntity<List<VinculoEscolaDTO>> listarVinculos(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(vinculoEscolaService.listarDoMotorista(Long.valueOf(jwt.getSubject())));
    }

    @GetMapping("/Vinculos/Pendentes")
    @PreAuthorize("hasRole('MOTORISTA')")
    public ResponseEntity<List<VinculoEscolaDTO>> listarConvitesPendentes(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(vinculoEscolaService.listarPendentesDoMotorista(
                Long.valueOf(jwt.getSubject())));
    }

    @GetMapping("/Vinculos/{idVinculo}/Historico")
    @PreAuthorize("hasRole('MOTORISTA')")
    public ResponseEntity<List<HistoricoVinculoEscolaDTO>> listarHistorico(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long idVinculo) {
        return ResponseEntity.ok(vinculoEscolaService.listarHistorico(
                Long.valueOf(jwt.getSubject()), idVinculo));
    }

    @PostMapping("/Vinculos/{idVinculo}/Aceitar")
    @PreAuthorize("hasRole('MOTORISTA')")
    public ResponseEntity<VinculoEscolaDTO> aceitarConvite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long idVinculo,
            @Valid @RequestBody(required = false) MotivoVinculoEscolaDTO dto) {
        return ResponseEntity.ok(vinculoEscolaService.responderComoMotorista(
                Long.valueOf(jwt.getSubject()), idVinculo, true, dto == null ? null : dto.motivo()));
    }

    @PostMapping("/Vinculos/{idVinculo}/Recusar")
    @PreAuthorize("hasRole('MOTORISTA')")
    public ResponseEntity<VinculoEscolaDTO> recusarConvite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long idVinculo,
            @Valid @RequestBody(required = false) MotivoVinculoEscolaDTO dto) {
        return ResponseEntity.ok(vinculoEscolaService.responderComoMotorista(
                Long.valueOf(jwt.getSubject()), idVinculo, false, dto == null ? null : dto.motivo()));
    }

    @PostMapping("/Vinculos/{idVinculo}/Encerrar")
    @PreAuthorize("hasRole('MOTORISTA')")
    public ResponseEntity<VinculoEscolaDTO> encerrarVinculo(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long idVinculo,
            @Valid @RequestBody(required = false) MotivoVinculoEscolaDTO dto) {
        return ResponseEntity.ok(vinculoEscolaService.encerrar(
                Long.valueOf(jwt.getSubject()), idVinculo, dto == null ? null : dto.motivo()));
    }
}
