package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.UsuarioDTO;
import TioDaPerua.BackEndJava.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Usuario")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/Cadastrar")
    public UsuarioDTO cadastrar(@RequestBody UsuarioDTO dto) {
        if (dto == null && dto.email() == null && dto.senha() == null) return null;
        if (dto.email().isEmpty() && dto.senha().isEmpty()) return null;

        return UsuarioDTO.toDTO(
                usuarioService.cadastrarUsuario(
                        UsuarioDTO.toEntity(dto)
                )
        );
    }

    @GetMapping("/Autenticar")
    public Boolean autenticar(@RequestBody UsuarioDTO dto){
        if (dto == null && dto.email() == null && dto.senha() == null) return null;
        if (dto.email().isEmpty() && dto.senha().isEmpty()) return null;

        if (usuarioService.autenticar(dto.email(), dto.senha())) return true;
        else return false;
    }




}
