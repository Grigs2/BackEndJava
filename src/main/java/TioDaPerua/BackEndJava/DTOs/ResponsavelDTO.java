package TioDaPerua.BackEndJava.DTOs;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.validator.constraints.br.CPF;

import com.fasterxml.jackson.annotation.JsonFormat;

import TioDaPerua.BackEndJava.entities.Responsavel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

public record ResponsavelDTO(Long id, 
                            @NotBlank(message = "Nome do responsável está nullo") String nome, 
                            @NotBlank(message = "CPF do responsável está nullo") 
                                @Pattern(regexp = "^\\d{11}$", message = "O CPF deve conter exatamente 11 dígitos numéricos.") 
                                @CPF(message = "CPF inválido.")String cpf, 
                            @NotBlank(message = "Data de nascimento do responsável está nullo")
                                @Past(message = "A data deve ser no passado.")
                                @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dataNascimento,
                            @NotBlank(message = "Usuário do responsável está nullo") @Valid UsuarioDTO usuario, 
                            @NotBlank(message = "Dependentes do responsável está nullo") @Valid List<DependenteDTO> dependentes) {

    public static ResponsavelDTO toDTO(Responsavel responsavel){
        if(responsavel == null){return null;}
        return new ResponsavelDTO(responsavel.getId(), responsavel.getNome(), responsavel.getCpf(), responsavel.getDataNascimento(),
                UsuarioDTO.toDTO(responsavel.getUsuario()), DependenteDTO.toListDTO(responsavel.getDependentes()) );
    }

    public static Responsavel toEntity(ResponsavelDTO dto){
        return new Responsavel(dto.id(), dto.nome(), dto.cpf(), dto.dataNascimento(), DependenteDTO.toListEntity(dto.dependentes()),
                UsuarioDTO.toEntity(dto.usuario()));
    }

}
