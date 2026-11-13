package TioDaPerua.BackEndJava.DTOs;

import java.util.List;

/**
 * Prontidão da viagem para iniciar. {@code bloqueios} impedem o início; {@code avisos} são pendências que não temos
 * como verificar no sistema (documentos do motorista e vínculo com a escola) e apenas informam o motorista.
 */
public record ProntidaoViagemDTO(
        Long idViagem,
        boolean pronta,
        List<String> bloqueios,
        List<String> avisos) {

    public ProntidaoViagemDTO(Long idViagem, boolean pronta, List<String> bloqueios) {
        this(idViagem, pronta, bloqueios, List.of());
    }
}
