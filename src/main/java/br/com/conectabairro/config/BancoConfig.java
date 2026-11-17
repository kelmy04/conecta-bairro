package br.com.conectabairro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Garante, a cada inicialização, o índice único que impede dois agendamentos CONFIRMADOS no mesmo
 * horário do mesmo comércio (última barreira contra reservas simultâneas). O Hibernate não cria
 * índices parciais sozinho; com isto, um banco novo na hospedagem não precisa rodar o schema.sql.
 * É idempotente: se o índice já existe, nada acontece.
 */
@Configuration
public class BancoConfig {
    private static final Logger log = LoggerFactory.getLogger(BancoConfig.class);

    @Bean
    CommandLineRunner garantirIndiceDeAgendamento(JdbcTemplate jdbc) {
        return args -> {
            try {
                jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_agendamento_horario "
                        + "ON agendamento (comercio_id, data_hora) WHERE status = 'CONFIRMADO'");
            } catch (Exception e) {
                // Ex.: já existem reservas duplicadas antigas. O sistema continua, mas avise o responsável.
                log.warn("Não foi possível criar o índice uq_agendamento_horario: {}", e.getMessage());
            }
        };
    }

    /**
     * O preço deixou de ser obrigatório (item "a combinar"). Em um banco já criado, a coluna produto.preco
     * continua NOT NULL, e o Hibernate (ddl-auto=update) não remove essa restrição sozinho. Este comando
     * a remove. É idempotente: se a coluna já aceita nulo, nada acontece.
     */
    @Bean
    CommandLineRunner liberarPrecoOpcional(JdbcTemplate jdbc) {
        return args -> {
            try {
                jdbc.execute("ALTER TABLE produto ALTER COLUMN preco DROP NOT NULL");
            } catch (Exception e) {
                log.warn("Não foi possível tornar produto.preco opcional: {}", e.getMessage());
            }
        };
    }
}
