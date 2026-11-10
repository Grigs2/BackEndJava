package TioDaPerua.BackEndJava.DTOs;

import TioDaPerua.BackEndJava.entities.Viagem;
import TioDaPerua.BackEndJava.enums.Periodo;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Viagem do motorista. Na criação, informe periodo e escolas (ids); nome é opcional e, se vazio,
 * o servidor usa "Viagem para escola(s): ..." (ida) ou "Viagem de volta das escola(s): ..." (volta).
 */
public record ViagemDTO(Long id,
                       Long motoristaId,
                       @NotBlank String periodo,
                       Boolean ativo,
                       List<EscolaResumoDTO> escolas,
                       String nome) {

    public static ViagemDTO toDTO(Viagem viagem) {
        if (viagem == null) return null;
        return new ViagemDTO(
                viagem.getId(),
                viagem.getMotorista() != null ? viagem.getMotorista().getId() : null,
                viagem.getPeriodo() != null ? viagem.getPeriodo().name() : null,
                viagem.getAtivo(),
                viagem.getEscolas().stream().map(e -> new EscolaResumoDTO(e.getId(), e.getNome())).toList(),
                viagem.getNome()
        );
    }

    public static Viagem toEntity(ViagemDTO dto) {
        if (dto == null) return null;
        Viagem viagem = new Viagem();
        viagem.setId(dto.id());
        if (dto.periodo() != null) {
            viagem.setPeriodo(Periodo.valueOf(dto.periodo()));
        }
        if (dto.ativo() != null) {
            viagem.setAtivo(dto.ativo());
        }
        viagem.setNome(dto.nome());
        return viagem;
    }
}
