package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.*;
import TioDaPerua.BackEndJava.entities.Dependente;
import TioDaPerua.BackEndJava.entities.Responsavel;
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

        if (!usuarioService.verificarSenha(dto.usuario().id(), dto.usuario().senha())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Responsavel atualizado = responsavelService.alterarInfos(ResponsavelDTO.toEntity(dto));
        if (atualizado == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(ResponsavelDTO.toDTO(atualizado));
    }

    @PutMapping("/Dependente/AlterarInfos")
    public ResponseEntity<DependenteDTO> AlterarDependenteInfos(@RequestBody DependenteDTO dto) {
        if (dto == null || dto.senha() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Dependente atualizado = dependenteService.alterarInfos(DependenteDTO.toEntity(dto), dto.senha());
            if (atualizado == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(DependenteDTO.toDTO(atualizado));
        } catch (RuntimeException e) {
            if ("SENHA_INVALIDA".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/Cadastrar")
    public ResponsavelDTO cadastrar(@RequestBody ResponsavelDTO dto) {
        if(dto ==null)return null;
        Responsavel entity = ResponsavelDTO.toEntity(dto);
        return ResponsavelDTO.toDTO(responsavelService.cadastrarResponsavel(entity));
    }

    @PostMapping("/Autenticar")
    public ResponsavelDTO autenticar(@RequestBody UsuarioDTO dto){
        if (dto == null && dto.email() == null && dto.senha() == null) return null;
        if (dto.email().isEmpty() && dto.senha().isEmpty()) return null;


        Usuario usuarioLogado = usuarioService.autenticar(dto.email(), dto.senha());
        if (usuarioLogado == null) return null;
        if("RESPONSAVEL".equals(usuarioLogado.getTipoPerfil())){
            return ResponsavelDTO.toDTO(usuarioService.logarResponsavel(usuarioLogado));
        }
        //Perfil nao é motorista
        return null;
    }

    @PostMapping("/CadastrarDependente/{id_Responsavel}")
    public ResponsavelDTO cadastrarDependente(@PathVariable Long id_Responsavel, @RequestBody DependenteDTO dto) {
        if (dto == null || id_Responsavel == null) return null;
        Dependente entity = DependenteDTO.toEntity(dto);
        return ResponsavelDTO.toDTO(responsavelService.cadastrarDependente(id_Responsavel, entity));
    }

    @GetMapping("/Monitorar/{id_dependente}")
    public MonitoramentoDependenteDTO Monitorar(@PathVariable Long id_dependente) {
        if (id_dependente == null) return null;
        
        Dependente dependente = responsavelService.buscarDependente(id_dependente);
        if (dependente == null) return null;

        List<ViagemPresenca> historico = responsavelService.buscarHistoricoDependente(id_dependente);
        ViagemPresenca statusAtualEntity = responsavelService.buscarStatusAtualDependente(id_dependente);
        
        ViagemPresencaDTO statusAtual = null;
        if (statusAtualEntity != null) {
            statusAtual = ViagemPresencaDTO.toDTO(statusAtualEntity);
        }

        List<ViagemPresencaDTO> historicoDTO = historico.stream()
                .map(ViagemPresencaDTO::toDTO)
                .collect(Collectors.toList());

        return new MonitoramentoDependenteDTO(
                dependente.getId(),
                dependente.getNome(),
                statusAtual,
                historicoDTO
        );
    }

    @PostMapping("/ResponderSolicitacao/{id_solicitacao}")
    public SolicitacaoDTO ResponderSolicitacao(@PathVariable Long id_solicitacao, @RequestBody SolicitacaoDTO dto) {
        if (id_solicitacao == null || dto == null) return null;
        return SolicitacaoDTO.toDTO(solicitacaoService.responderSolicitacao(id_solicitacao, dto.aceito()));
    }

    @GetMapping("/ListarEscolas")
    public List<EscolaDTO> listarEscolas() {
        return escolaService.listarEscolas().stream()
                .map(EscolaDTO::toDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/VincularEscola/{idResponsavel}/{idEscola}/{idDependente}")
    public ResponsavelDTO vincularEscola(
            @PathVariable Long idResponsavel,
            @PathVariable Long idEscola,
            @PathVariable Long idDependente) {
        if (idResponsavel == null || idEscola == null || idDependente == null) return null;
        return ResponsavelDTO.toDTO(responsavelService.vincularEscola(idResponsavel, idEscola, idDependente));
    }

    @GetMapping("/ListarSolicitacoes/{idResponsavel}")
    public List<SolicitacaoDTO> listarSolicitacoes(@PathVariable Long idResponsavel) {
        if (idResponsavel == null) return null;
        return responsavelService.listarSolicitacoes(idResponsavel).stream()
                .map(SolicitacaoDTO::toDTO)
                .collect(Collectors.toList());
    }

    @PutMapping("/EncerrarVinculoViagem/{idSolicitacao}")
    public SolicitacaoDTO encerrarVinculoViagem(@PathVariable Long idSolicitacao) {
        if (idSolicitacao == null) return null;
        return SolicitacaoDTO.toDTO(responsavelService.encerrarVinculoViagem(idSolicitacao));
    }

}
