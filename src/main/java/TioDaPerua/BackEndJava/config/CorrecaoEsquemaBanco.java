package TioDaPerua.BackEndJava.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ajustes de esquema que o {@code ddl-auto=update} do Hibernate não faz sozinho.
 *
 * Quando o Hibernate cria uma tabela com coluna de enum, ele adiciona uma restrição CHECK com os valores da época.
 * Ao incluir um valor novo no enum (ex.: {@code Periodo.MANHA_VOLTA}), bancos já existentes passam a recusar o valor,
 * porque o {@code update} não recria a restrição. Aqui a restrição antiga é removida na inicialização; a validação
 * dos valores continua sendo feita pelo próprio enum no código.
 *
 * É idempotente: se a restrição não existir (banco novo ou já corrigido), nada acontece.
 */
@Component
public class CorrecaoEsquemaBanco implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CorrecaoEsquemaBanco.class);

    /** Restrições CHECK de colunas de enum que precisam aceitar valores novos. */
    static final List<String> COMANDOS = List.of(
            "ALTER TABLE IF EXISTS public.viagem DROP CONSTRAINT IF EXISTS viagem_periodo_check"
    );

    private final JdbcTemplate jdbcTemplate;

    public CorrecaoEsquemaBanco(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String comando : COMANDOS) {
            try {
                jdbcTemplate.execute(comando);
            } catch (RuntimeException e) {
                // Não impede a aplicação de subir (ex.: banco que não é PostgreSQL em testes).
                log.warn("Não foi possível aplicar o ajuste de esquema '{}': {}", comando, e.getMessage());
            }
        }
    }
}
