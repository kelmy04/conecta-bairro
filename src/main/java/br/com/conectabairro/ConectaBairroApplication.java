package br.com.conectabairro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

/** Ponto de entrada da aplicação Spring Boot. */
@SpringBootApplication
public class ConectaBairroApplication {
    public static void main(String[] args) {
        // Servidores na nuvem costumam usar UTC. Sem este ajuste, "agora" ficaria 4 horas adiantado e a
        // agenda trataria horários futuros como passados (e vice-versa). Pode ser mudado com APP_TIMEZONE.
        TimeZone.setDefault(TimeZone.getTimeZone(System.getenv().getOrDefault("APP_TIMEZONE", "America/Cuiaba")));
        SpringApplication.run(ConectaBairroApplication.class, args);
    }
}
