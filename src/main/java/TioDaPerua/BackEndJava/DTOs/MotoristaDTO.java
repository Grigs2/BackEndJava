package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Motorista;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record MotoristaDTO(Long id,
                           @NotBlank String nome,
                           LocalDate dataNascimento,
                           String cpf,
                           String cnh,
                           @Valid UsuarioDTO usuarioDTO,
                           @Valid VeiculoDTO veiculoDTO,
                           @Valid EnderecoOperacionalDTO enderecoOperacionalDTO) {

    public MotoristaDTO(Long id, String nome, LocalDate dataNascimento, String cpf, String cnh,
                        UsuarioDTO usuarioDTO, VeiculoDTO veiculoDTO) {
        this(id, nome, dataNascimento, cpf, cnh, usuarioDTO, veiculoDTO, null);
    }

    public static MotoristaDTO toDTO(Motorista motorista) {
        if (motorista == null) { return null; }
        return new MotoristaDTO(motorista.getId(), motorista.getNome(), motorista.getDataNascimento(), motorista.getCpf(),
                motorista.getCnh(), UsuarioDTO.toDTO(motorista.getUsuario()), VeiculoDTO.toDTO(motorista.getVeiculo()),
                motorista.getUsuario() == null ? null
                        : EnderecoOperacionalDTO.toDTO(motorista.getUsuario().getEnderecoOperacional()));
    }

    public static Motorista toEntity(MotoristaDTO dto) {
        if (dto == null) { return null; }
        Motorista motorista = new Motorista(dto.id(), dto.nome, dto.dataNascimento, dto.cpf, dto.cnh,
                VeiculoDTO.toEntity(dto.veiculoDTO()), UsuarioDTO.toEntity(dto.usuarioDTO));
        if (motorista.getUsuario() != null && dto.enderecoOperacionalDTO() != null) {
            motorista.getUsuario().setEnderecoOperacional(
                    EnderecoOperacionalDTO.toEntity(dto.enderecoOperacionalDTO()));
        }
        return motorista;
    }





}
