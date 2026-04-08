package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.DependenteDTO;
import TioDaPerua.BackEndJava.DTOs.MotoristaDTO;
import TioDaPerua.BackEndJava.DTOs.ResponsavelDTO;
import TioDaPerua.BackEndJava.DTOs.UsuarioDTO;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.services.ResponsavelService;
import TioDaPerua.BackEndJava.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Responsavel")
public class ResponsavelController {

    @Autowired
    private ResponsavelService responsavelService;

    @Autowired
    private UsuarioService usuarioService;


    @PostMapping("/Cadastrar")
    public ResponsavelDTO cadastrar(@RequestBody ResponsavelDTO dto) {
        if(dto ==null)return null;
        return ResponsavelDTO.toDTO(responsavelService.cadastrarResponsavel(ResponsavelDTO.toEntity(dto)));
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
        return ResponsavelDTO.toDTO(responsavelService.cadastrarDependente(id_Responsavel, DependenteDTO.toEntity(dto)));
    }

}
