package TioDaPerua.BackEndJava.DTOs;

import java.time.LocalDate;

import org.hibernate.validator.constraints.br.CPF;

import com.fasterxml.jackson.annotation.JsonFormat;

import TioDaPerua.BackEndJava.entities.Motorista;
import TioDaPerua.BackEndJava.validations.CNH;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

public record MotoristaDTO(Long id,
                            @NotBlank(message = "Nome está nullo")String nome,
                            @NotNull(message = "dataNascimento está nullo")
                                @Past(message = "A data deve ser no passado.") 
                                @JsonFormat(pattern = "yyyy-MM-dd")LocalDate dataNascimento,
                            @NotBlank(message = "CPF está nullo")
                                @Pattern(regexp = "^\\d{11}$", message = "O CPF deve conter exatamente 11 dígitos numéricos.")
                                @CPF(message = "CPF inválido.")String cpf,
                            @NotBlank(message = "CNH está nullo") 
                                @Pattern(regexp = "^\\d{11}$", message = "A CNH deve conter exatamente 11 dígitos numéricos.")
                                @CNH(message = "CNH inválida.") String cnh,
                            @NotNull(message = "Usuário está nullo")@Valid UsuarioDTO usuarioDTO,
                            @NotNull(message = "Veículo está nullo")@Valid VeiculoDTO veiculoDTO) {


    public static MotoristaDTO toDTO(Motorista motorista) {
        if (motorista == null) { return null; }
        return new MotoristaDTO(motorista.getId(), motorista.getNome(), motorista.getDataNascimento(), motorista.getCpf(),
                motorista.getCnh(), UsuarioDTO.toDTO(motorista.getUsuario()), VeiculoDTO.toDTO(motorista.getVeiculo()));
    }

    public static Motorista toEntity(MotoristaDTO dto) {
        if (dto == null) { return null; }
        return new Motorista(dto.id(), dto.nome, dto.dataNascimento, dto.cpf, dto.cnh, VeiculoDTO.toEntity(dto.veiculoDTO()),
                UsuarioDTO.toEntity(dto.usuarioDTO));
    }





}
