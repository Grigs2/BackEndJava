package TioDaPerua.BackEndJava.controllers;

import TioDaPerua.BackEndJava.DTOs.EscolaDTO;
import TioDaPerua.BackEndJava.DTOs.UsuarioDTO;
import TioDaPerua.BackEndJava.entities.Escola;
import TioDaPerua.BackEndJava.entities.Usuario;
import TioDaPerua.BackEndJava.services.EscolaService;
import TioDaPerua.BackEndJava.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Escola")
public class EscolaController {

    @Autowired
    private EscolaService escolaService;

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/Cadastrar")
    public EscolaDTO cadastrar(@RequestBody EscolaDTO dto) {
        if (dto == null || dto.usuarioDTO() == null) return null;
        if (!"ESCOLA".equals(dto.usuarioDTO().tipoPerfil())) return null;

        Escola escola = EscolaDTO.toEntity(dto);
        Escola escolaSalva = escolaService.cadastrarEscola(escola);
        return EscolaDTO.toDTO(escolaSalva);
    }

    @PostMapping("/Autenticar")
    public EscolaDTO autenticar(@RequestBody UsuarioDTO dto) {
        if (dto == null || dto.email() == null || dto.senha() == null) return null;

        Usuario usuario = usuarioService.autenticar(dto.email(), dto.senha());
        if (usuario != null) {
            Escola escola = usuarioService.logarEscola(usuario);
            return EscolaDTO.toDTO(escola);
        }
        return null;
    }
}
