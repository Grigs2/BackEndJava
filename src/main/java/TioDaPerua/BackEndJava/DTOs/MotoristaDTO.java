package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Motorista;

import java.time.LocalDate;

public record MotoristaDTO(Long id,
                           String nome,
                           LocalDate dataNascimento,
                           String cpf,
                           String cnh,
                           UsuarioDTO usuarioDTO,
                           VeiculoDTO veiculoDTO) {


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
