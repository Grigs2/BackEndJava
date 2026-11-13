package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.*;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.services.EscolaService;
import TioDaPerua.BackEndJava.services.ResponsavelService;
import TioDaPerua.BackEndJava.services.SolicitacaoService;
import TioDaPerua.BackEndJava.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import TioDaPerua.BackEndJava.services.DependenteService;
import TioDaPerua.BackEndJava.services.EnderecoDependenteService;
import TioDaPerua.BackEndJava.services.RedeResponsaveisService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/Responsavel")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Responsavel")
public class ResponsavelController {

    @org.springframework.beans.factory.annotation.Autowired
    private TioDaPerua.BackEndJava.services.ContraparteService contraparteService;

    /** Dados do motorista de uma solicitação (pop-up antes de aceitar). CPF, e-mail e placa vêm mascarados. */
    @GetMapping("/Solicitacoes/{idSolicitacao}/Contraparte")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ResponseEntity<TioDaPerua.BackEndJava.DTOs.ContraparteDTO> contraparteDaSolicitacao(
            @PathVariable Long idSolicitacao,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(contraparteService.motoristaDaSolicitacao(idSolicitacao, Long.valueOf(jwt.getSubject())));
    }


    @Autowired
    private ResponsavelService responsavelService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SolicitacaoService solicitacaoService;

    @Autowired
    private EscolaService escolaService;

    @Autowired
    private DependenteService dependenteService;

    @Autowired
    private EnderecoDependenteService enderecoDependenteService;

    @Autowired
    private RedeResponsaveisService redeResponsaveisService;

    @Autowired
    private TioDaPerua.BackEndJava.services.BuscaMotoristaService buscaMotoristaService;

    @Autowired
    private TioDaPerua.BackEndJava.services.HistoricoService historicoService;

    @Autowired
    private TioDaPerua.BackEndJava.services.AusenciaService ausenciaService;

    @Autowired
    private TioDaPerua.BackEndJava.services.AvaliacaoService avaliacaoService;

    /** Histórico de viagens do aluno (padrão: últimos 30 dias; datas em AAAA-MM-DD). */
    @GetMapping("/Dependente/{idDependente}/Historico")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0)")
    public ResponseEntity<List<HistoricoAlunoDTO>> historicoDoAluno(
            @PathVariable Long idDependente,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate de,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate ate) {
        return ResponseEntity.ok(historicoService.doAluno(idDependente, de, ate));
    }

    /** Ausências avisadas a partir de hoje. */
    @GetMapping("/Dependente/{idDependente}/Ausencias")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0)")
    public ResponseEntity<List<AusenciaDTO>> listarAusencias(@PathVariable Long idDependente) {
        return ResponseEntity.ok(ausenciaService.listar(idDependente));
    }

    /** Avisa que o aluno não vai em uma data (trecho TODOS, IDA ou VOLTA). O motorista é notificado. */
    @PostMapping("/Dependente/{idDependente}/Ausencias")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0)")
    public ResponseEntity<AusenciaDTO> avisarAusencia(
            @PathVariable Long idDependente,
            @Valid @RequestBody AusenciaDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ausenciaService.registrar(
                idDependente, Long.valueOf(jwt.getSubject()), dto.data(), dto.trecho(), dto.motivo()));
    }

    /** Cancela a ausência enquanto a viagem daquele dia ainda não foi preparada. */
    @DeleteMapping("/Dependente/{idDependente}/Ausencias/{idAusencia}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0)")
    public ResponseEntity<Void> cancelarAusencia(@PathVariable Long idDependente, @PathVariable Long idAusencia) {
        ausenciaService.cancelar(idDependente, idAusencia);
        return ResponseEntity.noContent().build();
    }

    /**
     * Busca motoristas por nome e/ou proximidade (mesmo bairro/cidade do aluno ou do responsável).
     * Ex.: GET /Responsavel/3/Motoristas/Buscar?nome=jo&idDependente=7&somenteProximos=true
     */
    @GetMapping("/{idResponsavel}/Motoristas/Buscar")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavel(authentication, #p0)")
    public ResponseEntity<List<MotoristaDisponivelDTO>> buscarMotoristas(
            @PathVariable Long idResponsavel,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Long idDependente,
            @RequestParam(defaultValue = "false") boolean somenteProximos) {
        return ResponseEntity.ok(buscaMotoristaService.buscar(idResponsavel, nome, idDependente, somenteProximos));
    }

    /** O responsável pede vaga para o dependente em uma viagem; o motorista responde. */
    @PostMapping("/SolicitarTransporte/{idViagem}/{idDependente}")
    @PreAuthorize("#p1 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p1)")
    public ResponseEntity<SolicitacaoDTO> solicitarTransporte(
            @PathVariable Long idViagem,
            @PathVariable Long idDependente,
            @Valid @RequestBody TioDaPerua.BackEndJava.DTOs.SolicitarTransporteDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        Solicitacao solicitacao = solicitacaoService.solicitarPeloResponsavel(
                idViagem, idDependente, Long.valueOf(jwt.getSubject()), dto.compartilhamentoAutorizado());
        return ResponseEntity.status(HttpStatus.CREATED).body(SolicitacaoDTO.toDTO(solicitacao));
    }

    /** Avaliação do motorista pelo responsável; exige acordo de transporte ativo entre eles. */
    @PostMapping("/AvaliarMotorista/{idMotorista}")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ResponseEntity<AvaliacaoDTO> avaliarMotorista(
            @PathVariable Long idMotorista,
            @Valid @RequestBody AvaliarMotoristaDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(avaliacaoService.avaliar(
                Long.valueOf(jwt.getSubject()), idMotorista, dto.nota(), dto.comentario()));
    }

    @GetMapping("/Motorista/{idMotorista}/Avaliacoes")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ResponseEntity<AvaliacaoDTO.Resumo> avaliacoesDoMotorista(@PathVariable Long idMotorista) {
        return ResponseEntity.ok(avaliacaoService.resumo(idMotorista));
    }

    @GetMapping("/Perfil")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ResponseEntity<ResponsavelDTO> consultarPerfil(@AuthenticationPrincipal Jwt jwt) {
        Usuario usuario = usuarioService.findByID(Long.valueOf(jwt.getSubject()));
        return ResponseEntity.ok(ResponsavelDTO.toDTO(usuarioService.logarResponsavel(usuario)));
    }

    @PutMapping("/AlterarInfos")
    @PreAuthorize("#p0 != null && #p0.usuario() != null && @resourceAuthorization.isResponsavel(authentication, #p0.id()) && @resourceAuthorization.isUsuarioAutenticado(authentication, #p0.usuario().id())")
    public ResponseEntity<ResponsavelDTO> AlterarInfos(@Valid @RequestBody ResponsavelDTO dto) {
        if (dto == null || dto.usuario() == null || dto.usuario().senha() == null) {
            return ResponseEntity.badRequest().build();
        }

        usuarioService.validarSenha(dto.usuario().id(), dto.usuario().senha());

        Responsavel atualizado = responsavelService.alterarInfos(ResponsavelDTO.toEntity(dto));
        return ResponseEntity.ok(ResponsavelDTO.toDTO(atualizado));
    }

    @PutMapping("/Dependente/AlterarInfos")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0.id())")
    public ResponseEntity<DependenteDTO> AlterarDependenteInfos(
            @Valid @RequestBody DependenteDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        if (dto == null || dto.senha() == null) {
            return ResponseEntity.badRequest().build();
        }

        Dependente atualizado = dependenteService.alterarInfos(
                DependenteDTO.toEntity(dto), dto.senha(), Long.valueOf(jwt.getSubject()));
        return ResponseEntity.ok(DependenteDTO.toDTO(atualizado));
    }

    @GetMapping("/Dependente/{idDependente}/Enderecos")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0)")
    public ResponseEntity<List<EnderecoDependenteDTO>> listarEnderecos(
            @PathVariable Long idDependente,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(enderecoDependenteService.listar(
                idDependente, Long.valueOf(jwt.getSubject())));
    }

    @PostMapping("/Dependente/{idDependente}/Enderecos")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0)")
    public ResponseEntity<EnderecoDependenteDTO> adicionarEndereco(
            @PathVariable Long idDependente,
            @Valid @RequestBody EnderecoDependenteRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        if (dto == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(enderecoDependenteService.adicionar(
                idDependente, Long.valueOf(jwt.getSubject()), dto));
    }

    @PutMapping("/Dependente/{idDependente}/Enderecos/{idEndereco}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0)")
    public ResponseEntity<EnderecoDependenteDTO> alterarEndereco(
            @PathVariable Long idDependente,
            @PathVariable Long idEndereco,
            @Valid @RequestBody EnderecoDependenteRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        if (dto == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(enderecoDependenteService.alterar(
                idDependente, idEndereco, Long.valueOf(jwt.getSubject()), dto));
    }

    @DeleteMapping("/Dependente/{idDependente}/Enderecos/{idEndereco}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0)")
    public ResponseEntity<Void> desativarEndereco(
            @PathVariable Long idDependente,
            @PathVariable Long idEndereco,
            @AuthenticationPrincipal Jwt jwt) {
        enderecoDependenteService.desativar(
                idDependente, idEndereco, Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/Dependente/{idDependente}/Convites")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0)")
    public ResponseEntity<ConviteResponsavelDTO> convidarResponsavel(
            @PathVariable Long idDependente,
            @Valid @RequestBody CriarConviteResponsavelDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        if (dto == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(redeResponsaveisService.convidar(
                idDependente, Long.valueOf(jwt.getSubject()), dto));
    }

    @GetMapping("/Convites/Pendentes")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ResponseEntity<List<ConviteResponsavelDTO>> listarConvitesPendentes(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(redeResponsaveisService.listarPendentes(
                Long.valueOf(jwt.getSubject())));
    }

    @GetMapping("/Dependente/{idDependente}/Convites")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0)")
    public ResponseEntity<List<ConviteResponsavelDTO>> listarHistoricoConvites(
            @PathVariable Long idDependente,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(redeResponsaveisService.listarHistorico(
                idDependente, Long.valueOf(jwt.getSubject())));
    }

    @GetMapping("/Dependente/{idDependente}/Responsaveis")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0)")
    public ResponseEntity<List<VinculoResponsavelDTO>> listarResponsaveisAtivos(
            @PathVariable Long idDependente,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(redeResponsaveisService.listarResponsaveisAtivos(
                idDependente, Long.valueOf(jwt.getSubject())));
    }

    @PutMapping("/Convites/{idConvite}/Aceitar")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ResponseEntity<ConviteResponsavelDTO> aceitarConvite(
            @PathVariable Long idConvite,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(redeResponsaveisService.responder(
                idConvite, Long.valueOf(jwt.getSubject()), true));
    }

    @PutMapping("/Convites/{idConvite}/Recusar")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ResponseEntity<ConviteResponsavelDTO> recusarConvite(
            @PathVariable Long idConvite,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(redeResponsaveisService.responder(
                idConvite, Long.valueOf(jwt.getSubject()), false));
    }

    @DeleteMapping("/Dependente/{idDependente}/Responsaveis/{idResponsavel}")
    @PreAuthorize("hasAnyRole('RESPONSAVEL', 'ADMIN')")
    public ResponseEntity<Void> revogarVinculoResponsavel(
            @PathVariable Long idDependente,
            @PathVariable Long idResponsavel,
            @AuthenticationPrincipal Jwt jwt,
            org.springframework.security.core.Authentication authentication) {
        boolean administrador = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        redeResponsaveisService.revogar(
                idDependente, idResponsavel, Long.valueOf(jwt.getSubject()), administrador);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/Cadastrar")
    public ResponseEntity<ResponsavelDTO> cadastrar(@Valid @RequestBody ResponsavelDTO dto) {
        if (dto == null || dto.id() != null || dto.usuario() == null
                || dto.usuario().id() != null || !"RESPONSAVEL".equals(dto.usuario().tipoPerfil())
                || (dto.dependentes() != null && !dto.dependentes().isEmpty())) {
            return ResponseEntity.badRequest().build();
        }
        Responsavel entity = ResponsavelDTO.toEntity(dto);
        Responsavel salvo = responsavelService.cadastrarResponsavel(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponsavelDTO.toDTO(salvo));
    }

    @PostMapping("/Autenticar")
    public ResponseEntity<ResponsavelDTO> autenticar(@Valid @RequestBody UsuarioDTO dto) {
        if (dto == null || dto.email() == null || dto.senha() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (dto.email().isEmpty() || dto.senha().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Usuario usuarioLogado = usuarioService.autenticar(dto.email(), dto.senha());
        Responsavel responsavel = usuarioService.logarResponsavel(usuarioLogado);
        return ResponseEntity.ok(ResponsavelDTO.toDTO(responsavel));
    }

    @PostMapping("/CadastrarDependente/{id_Responsavel}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavel(authentication, #p0)")
    public ResponseEntity<ResponsavelDTO> cadastrarDependente(@PathVariable Long id_Responsavel, @Valid @RequestBody DependenteDTO dto) {
        if (dto == null || dto.id() != null || id_Responsavel == null) {
            return ResponseEntity.badRequest().build();
        }
        Dependente entity = DependenteDTO.toEntity(dto);
        Responsavel responsavel = responsavelService.cadastrarDependente(id_Responsavel, entity,
                dto.escola() != null ? dto.escola().id() : null);
        return ResponseEntity.ok(ResponsavelDTO.toDTO(responsavel));
    }

    @GetMapping("/Monitorar/{id_dependente}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDoDependente(authentication, #p0)")
    public ResponseEntity<MonitoramentoDependenteDTO> Monitorar(@PathVariable Long id_dependente) {
        if (id_dependente == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Dependente dependente = responsavelService.buscarDependente(id_dependente);

        List<ViagemPresenca> historico = responsavelService.buscarHistoricoDependente(id_dependente);
        ViagemPresenca statusAtualEntity = responsavelService.buscarStatusAtualDependente(id_dependente);
        
        ViagemPresencaDTO statusAtual = null;
        if (statusAtualEntity != null) {
            statusAtual = ViagemPresencaDTO.toDTO(statusAtualEntity);
        }

        List<ViagemPresencaDTO> historicoDTO = historico.stream()
                .map(ViagemPresencaDTO::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new MonitoramentoDependenteDTO(
                dependente.getId(),
                dependente.getNome(),
                statusAtual,
                historicoDTO
        ));
    }

    @PostMapping("/ResponderSolicitacao/{id_solicitacao}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDaSolicitacao(authentication, #p0)")
    public ResponseEntity<SolicitacaoDTO> ResponderSolicitacao(
            @PathVariable Long id_solicitacao,
            @Valid @RequestBody TioDaPerua.BackEndJava.DTOs.ResponderSolicitacaoDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        if (id_solicitacao == null || dto == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(SolicitacaoDTO.toDTO(solicitacaoService.responderSolicitacao(
                id_solicitacao, dto.aceito(), dto.compartilhamentoAutorizado(), Long.valueOf(jwt.getSubject()))));
    }

    @GetMapping("/ListarEscolas")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ResponseEntity<List<EscolaDTO>> listarEscolas() {
        List<EscolaDTO> escolas = escolaService.listarEscolas().stream()
                .map(EscolaDTO::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(escolas);
    }

    @PostMapping("/VincularEscola/{idResponsavel}/{idEscola}/{idDependente}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavel(authentication, #p0)")
    public ResponseEntity<ResponsavelDTO> vincularEscola(
            @PathVariable Long idResponsavel,
            @PathVariable Long idEscola,
            @PathVariable Long idDependente) {
        if (idResponsavel == null || idEscola == null || idDependente == null) {
            return ResponseEntity.badRequest().build();
        }
        Responsavel responsavel = responsavelService.vincularEscola(idResponsavel, idEscola, idDependente);
        return ResponseEntity.ok(ResponsavelDTO.toDTO(responsavel));
    }

    @GetMapping("/ListarSolicitacoes/{idResponsavel}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavel(authentication, #p0)")
    public ResponseEntity<List<SolicitacaoDTO>> listarSolicitacoes(@PathVariable Long idResponsavel) {
        if (idResponsavel == null) {
            return ResponseEntity.badRequest().build();
        }
        List<SolicitacaoDTO> solicitacoes = responsavelService.listarSolicitacoes(idResponsavel).stream()
                .map(SolicitacaoDTO::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(solicitacoes);
    }

    @PutMapping("/EncerrarVinculoViagem/{idSolicitacao}")
    @PreAuthorize("#p0 != null && @resourceAuthorization.isResponsavelDaSolicitacao(authentication, #p0)")
    public ResponseEntity<SolicitacaoDTO> encerrarVinculoViagem(@PathVariable Long idSolicitacao) {
        if (idSolicitacao == null) {
            return ResponseEntity.badRequest().build();
        }
        Solicitacao solicitacao = responsavelService.encerrarVinculoViagem(idSolicitacao);
        return ResponseEntity.ok(SolicitacaoDTO.toDTO(solicitacao));
    }

}
