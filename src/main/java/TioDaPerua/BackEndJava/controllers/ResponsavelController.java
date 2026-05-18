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


    @PostMapping("/Cadastrar")
    public ResponsavelDTO cadastrar(@RequestBody ResponsavelDTO dto) {
        if(dto ==null)return null;
        Responsavel entity = ResponsavelDTO.toEntity(dto);
        return ResponsavelDTO.toDTO(responsavelService.cadastrarResponsavel(entity));
    }

    @GetMapping("/Autenticar")
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
        
        ViagemPresencaDTO statusAtual = null;
        if (!historico.isEmpty()) {
            statusAtual = ViagemPresencaDTO.toDTO(historico.get(0));
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

    @PutMapping("/EncerrarVinculoViagem")
    public SolicitacaoDTO encerrarVinculoViagem(@RequestBody EncerrarVinculoViagemDTO dto) {
        if (dto == null) return null;
        return SolicitacaoDTO.toDTO(responsavelService.encerrarVinculoViagem(dto.idSolicitacao()));
    }

}
