package TioDaPerua.BackEndJava.repositories;

import TioDaPerua.BackEndJava.enums.CategoriaVinculoResponsavel;

import java.time.Instant;

public interface AdminVinculoResponsavelProjection {
    Long getDependenteId();
    Long getResponsavelId();
    CategoriaVinculoResponsavel getCategoria();
    Boolean getResponsavelCriador();
    Instant getVinculadoEm();
}
