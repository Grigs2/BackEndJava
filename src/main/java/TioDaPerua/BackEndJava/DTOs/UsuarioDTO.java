package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;

public record UsuarioDTO(
        Long id,
        @NotBlank @Email String email,
        @NotBlank String senha,
        String endereco,
        String telefone,
        String tipoPerfil,
        Boolean aceitouTermos
) implements Serializable {

    @Serial
    private static final long serialVersionUID = -59861L;


    public static UsuarioDTO toDTO(Usuario usuario) {
        if (usuario == null) return null;
        return new UsuarioDTO(usuario.getId(), usuario.getEmail(), null, usuario.getEndereco(),
                usuario.getTelefone(), usuario.getTipoPerfil(),
                usuario.getTermosAceitosEm() == null ? null : true);
    }

    public static Usuario toEntity(UsuarioDTO dto) {
        if (dto == null) return null;
        Usuario usuario = new Usuario(dto.email, dto.senha, dto.endereco, dto.telefone, dto.tipoPerfil);
        usuario.setAceitouTermos(dto.aceitouTermos());
        return usuario;
    }

}
