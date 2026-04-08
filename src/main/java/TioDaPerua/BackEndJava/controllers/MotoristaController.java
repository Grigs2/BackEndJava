package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.MotoristaDTO;
import TioDaPerua.BackEndJava.DTOs.UsuarioDTO;
import TioDaPerua.BackEndJava.DTOs.VeiculoDTO;
import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.services.MotoristaService;
import TioDaPerua.BackEndJava.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Motorista")
public class MotoristaController {

    @Autowired
    private MotoristaService motoristaService;

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/Cadastrar")
    public MotoristaDTO Cadastrar(@RequestBody MotoristaDTO dto) {
        if(dto ==null || dto.usuarioDTO()==null) return null;
        if(!"MOTORISTA".equals(dto.usuarioDTO().tipoPerfil())) return null;

        return MotoristaDTO.toDTO(motoristaService.cadastraMotorista(MotoristaDTO.toEntity(dto)));

    }

    @PostMapping("/CadastrarVeiculo/{id_motorista}")
    public MotoristaDTO CadastrarVeiculo(@PathVariable Long id_motorista, @RequestBody VeiculoDTO dto) {
        if (dto == null || id_motorista == null) return null;
        return MotoristaDTO.toDTO(motoristaService.cadastrarVeiculo(id_motorista, VeiculoDTO.toEntity(dto)));
    }

    @GetMapping("/Autenticar")
    public MotoristaDTO autenticar(@RequestBody UsuarioDTO dto){
        if (dto == null && dto.email() == null && dto.senha() == null) return null;
        if (dto.email().isEmpty() && dto.senha().isEmpty()) return null;


        Usuario usuarioLogado = usuarioService.autenticar(dto.email(), dto.senha());
        if (usuarioLogado == null) return null;
        if("MOTORISTA".equals(usuarioLogado.getTipoPerfil())){
            return MotoristaDTO.toDTO(usuarioService.logarMotorista(usuarioLogado));
        }
        //Perfil nao é motorista
        return null;
    }
}
