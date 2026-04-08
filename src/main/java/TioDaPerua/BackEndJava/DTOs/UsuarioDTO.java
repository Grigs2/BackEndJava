package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Usuario;

import java.io.Serial;
import java.io.Serializable;

public record UsuarioDTO(
        Long id,
        String email,
        String senha,
        String endereco,
        String telefone,
        String tipoPerfil
) implements Serializable {

    @Serial
    private static final long serialVersionUID = -59861L;


    public static UsuarioDTO toDTO(Usuario usuario) {
        if (usuario == null) return null;
        return new UsuarioDTO(usuario.getId(), usuario.getEmail(),null, usuario.getEndereco(), usuario.getTelefone(), usuario.getTipoPerfil());
    }

    public static Usuario toEntity(UsuarioDTO dto) {
        if (dto == null) return null;
        return new Usuario(dto.email, dto.senha, dto.endereco, dto.telefone, dto.tipoPerfil);
    }

}
