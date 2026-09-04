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

import TioDaPerua.BackEndJava.DTOs.DependenteDTO;
import TioDaPerua.BackEndJava.DTOs.EscolaDTO;
import TioDaPerua.BackEndJava.DTOs.MonitoramentoDependenteDTO;
import TioDaPerua.BackEndJava.DTOs.ResponsavelDTO;
import TioDaPerua.BackEndJava.DTOs.SolicitacaoDTO;
import TioDaPerua.BackEndJava.DTOs.UsuarioDTO;
import TioDaPerua.BackEndJava.DTOs.ViagemPresencaDTO;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Responsavel;
import TioDaPerua.BackEndJava.entities.Solicitacao;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.entities.ViagemPresenca;
import TioDaPerua.BackEndJava.services.DependenteService;
import TioDaPerua.BackEndJava.services.EscolaService;
import TioDaPerua.BackEndJava.services.ResponsavelService;
import TioDaPerua.BackEndJava.services.SolicitacaoService;
import TioDaPerua.BackEndJava.services.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/Responsavel")
public class ResponsavelController {

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

    @PutMapping("/AlterarInfos")
    public ResponseEntity<ResponsavelDTO> AlterarInfos(@Valid @RequestBody ResponsavelDTO dto) {
        usuarioService.validarSenha(dto.usuario().id(), dto.usuario().senha());

        Responsavel atualizado = responsavelService.alterarInfos(ResponsavelDTO.toEntity(dto));
        return ResponseEntity.ok(ResponsavelDTO.toDTO(atualizado));
    }

    @PutMapping("/Dependente/AlterarInfos")
    public ResponseEntity<DependenteDTO> AlterarDependenteInfos(@Valid @RequestBody DependenteDTO dto) {
        Dependente atualizado = dependenteService.alterarInfos(DependenteDTO.toEntity(dto), dto.senha());
        return ResponseEntity.ok(DependenteDTO.toDTO(atualizado));
    }

    @PostMapping("/Cadastrar")
    public ResponseEntity<ResponsavelDTO> cadastrar(@Valid @RequestBody ResponsavelDTO dto) {
        Responsavel entity = ResponsavelDTO.toEntity(dto);
        Responsavel salvo = responsavelService.cadastrarResponsavel(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponsavelDTO.toDTO(salvo));
    }

    @PostMapping("/Autenticar")
    public ResponseEntity<ResponsavelDTO> autenticar(@Valid @RequestBody UsuarioDTO dto) {
        Usuario usuarioLogado = usuarioService.autenticar(dto.email(), dto.senha());
        Responsavel responsavel = usuarioService.logarResponsavel(usuarioLogado);
        return ResponseEntity.ok(ResponsavelDTO.toDTO(responsavel));
    }

    @PostMapping("/CadastrarDependente/{id_Responsavel}")
    public ResponseEntity<ResponsavelDTO> cadastrarDependente(@NotBlank(message = "ID do responsável está nullo") @PathVariable Long id_Responsavel,
                                                                                                                  @Valid @RequestBody DependenteDTO dto) {
        Dependente entity = DependenteDTO.toEntity(dto);
        Responsavel responsavel = responsavelService.cadastrarDependente(id_Responsavel, entity);
        return ResponseEntity.ok(ResponsavelDTO.toDTO(responsavel));
    }

    @GetMapping("/Monitorar/{id_dependente}")
    public ResponseEntity<MonitoramentoDependenteDTO> Monitorar(@NotBlank(message = "ID do dependente está nullo") @PathVariable Long id_dependente) {
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
    public ResponseEntity<SolicitacaoDTO> ResponderSolicitacao(@NotBlank(message = "ID da solicitacao está nullo") @PathVariable Long id_solicitacao,
                                                               @Valid @RequestBody SolicitacaoDTO dto) {
        return ResponseEntity.ok(SolicitacaoDTO.toDTO(solicitacaoService.responderSolicitacao(id_solicitacao, dto.aceito())));
    }

    @GetMapping("/ListarEscolas")
    public ResponseEntity<List<EscolaDTO>> listarEscolas() {
        List<EscolaDTO> escolas = escolaService.listarEscolas().stream()
                .map(EscolaDTO::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(escolas);
    }

    @PostMapping("/VincularEscola/{idResponsavel}/{idEscola}/{idDependente}")
    public ResponseEntity<ResponsavelDTO> vincularEscola(
                                                        @NotBlank(message = "ID do responsável está nullo") @PathVariable Long idResponsavel,
                                                        @NotBlank(message = "ID da escola está nullo") @PathVariable Long idEscola,
                                                        @NotBlank(message = "ID do dependente está nullo") @PathVariable Long idDependente) {
        Responsavel responsavel = responsavelService.vincularEscola(idResponsavel, idEscola, idDependente);
        return ResponseEntity.ok(ResponsavelDTO.toDTO(responsavel));
    }

    @GetMapping("/ListarSolicitacoes/{idResponsavel}")
    public ResponseEntity<List<SolicitacaoDTO>> listarSolicitacoes(@NotBlank(message = "ID do responsável está nullo") @PathVariable Long idResponsavel) {
        List<SolicitacaoDTO> solicitacoes = responsavelService.listarSolicitacoes(idResponsavel).stream()
                .map(SolicitacaoDTO::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(solicitacoes);
    }

    @PutMapping("/EncerrarVinculoViagem/{idSolicitacao}")
    public ResponseEntity<SolicitacaoDTO> encerrarVinculoViagem(@NotBlank(message = "ID da solicitacao está nullo") @PathVariable Long idSolicitacao) {
        if (idSolicitacao == null) {
            return ResponseEntity.badRequest().build();
        }
        Solicitacao solicitacao = responsavelService.encerrarVinculoViagem(idSolicitacao);
        return ResponseEntity.ok(SolicitacaoDTO.toDTO(solicitacao));
    }

}
