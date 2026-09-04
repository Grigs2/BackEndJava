package TioDaPerua.BackEndJava.DTOs;

import java.io.Serial;
import java.io.Serializable;

import TioDaPerua.BackEndJava.entities.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UsuarioDTO(
        Long id,
        @NotBlank(message = "Email está nullo") 
            @Email(message = "E-mail inválido.") String email,
        @NotBlank(message = "Senha está nullo") String senha,
        @NotBlank(message = "Endereço está nullo") String endereco,
        @NotBlank(message = "Telefone está nullo") 
            @Pattern(regexp = "^\\+\\d{2}\\d{2}\\d{8,9}$", message = "O telefone deve estar no padrão E.164 (+5511999999999).")String telefone,
        @NotBlank(message = "Tipo de perfil está nullo") String tipoPerfil
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
