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
    public ResponseEntity<ResponsavelDTO> AlterarInfos(@RequestBody ResponsavelDTO dto) {
        if (dto == null || dto.usuario() == null || dto.usuario().senha() == null) {
            return ResponseEntity.badRequest().build();
        }

        usuarioService.validarSenha(dto.usuario().id(), dto.usuario().senha());

        Responsavel atualizado = responsavelService.alterarInfos(ResponsavelDTO.toEntity(dto));
        return ResponseEntity.ok(ResponsavelDTO.toDTO(atualizado));
    }

    @PutMapping("/Dependente/AlterarInfos")
    public ResponseEntity<DependenteDTO> AlterarDependenteInfos(@RequestBody DependenteDTO dto) {
        if (dto == null || dto.senha() == null) {
            return ResponseEntity.badRequest().build();
        }

        Dependente atualizado = dependenteService.alterarInfos(DependenteDTO.toEntity(dto), dto.senha());
        return ResponseEntity.ok(DependenteDTO.toDTO(atualizado));
    }

    @PostMapping("/Cadastrar")
    public ResponseEntity<ResponsavelDTO> cadastrar(@RequestBody ResponsavelDTO dto) {
        if (dto == null) {
            return ResponseEntity.badRequest().build();
        }
        Responsavel entity = ResponsavelDTO.toEntity(dto);
        Responsavel salvo = responsavelService.cadastrarResponsavel(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponsavelDTO.toDTO(salvo));
    }

    @PostMapping("/Autenticar")
    public ResponseEntity<ResponsavelDTO> autenticar(@RequestBody UsuarioDTO dto) {
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
    public ResponseEntity<ResponsavelDTO> cadastrarDependente(@PathVariable Long id_Responsavel, @RequestBody DependenteDTO dto) {
        if (dto == null || id_Responsavel == null) {
            return ResponseEntity.badRequest().build();
        }
        Dependente entity = DependenteDTO.toEntity(dto);
        Responsavel responsavel = responsavelService.cadastrarDependente(id_Responsavel, entity);
        return ResponseEntity.ok(ResponsavelDTO.toDTO(responsavel));
    }

    @GetMapping("/Monitorar/{id_dependente}")
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
    public ResponseEntity<SolicitacaoDTO> ResponderSolicitacao(@PathVariable Long id_solicitacao, @RequestBody SolicitacaoDTO dto) {
        if (id_solicitacao == null || dto == null) {
            return ResponseEntity.badRequest().build();
        }
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
    public ResponseEntity<SolicitacaoDTO> encerrarVinculoViagem(@PathVariable Long idSolicitacao) {
        if (idSolicitacao == null) {
            return ResponseEntity.badRequest().build();
        }
        Solicitacao solicitacao = responsavelService.encerrarVinculoViagem(idSolicitacao);
        return ResponseEntity.ok(SolicitacaoDTO.toDTO(solicitacao));
    }

}
